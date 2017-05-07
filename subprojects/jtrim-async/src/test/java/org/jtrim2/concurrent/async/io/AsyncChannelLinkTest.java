package org.jtrim2.concurrent.async.io;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.InterruptibleChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.concurrent.async.AsyncDataController;
import org.jtrim2.concurrent.async.AsyncDataListener;
import org.jtrim2.concurrent.async.AsyncDataState;
import org.jtrim2.concurrent.async.AsyncReport;
import org.jtrim2.concurrent.async.SimpleDataState;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.ThreadPoolTaskExecutor;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.jtrim2.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class AsyncChannelLinkTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @SuppressWarnings("unchecked")
    private static <DataType, ChannelType extends Channel>
            ChannelProcessor<DataType, ChannelType> mockChannelProcessor() {
        return mock(ChannelProcessor.class);
    }

    @SuppressWarnings("unchecked")
    private static <ChannelType extends Channel> ChannelOpener<ChannelType> mockChannelOpener() {
        return mock(ChannelOpener.class);
    }

    private static <DataType, ChannelType extends Channel> AsyncChannelLink<DataType> create(
            TaskExecutor processorExecutor,
            TaskExecutor cancelExecutor,
            ChannelOpener<? extends ChannelType> channelOpener,
            ChannelProcessor<? extends DataType, ChannelType> channelProcessor) {
        return new AsyncChannelLink<>(processorExecutor, cancelExecutor, channelOpener, channelProcessor);
    }

    private static <T> AsyncChannelLink<T> createChannelLink(
            boolean closeThrowsException,
            int millisPerInput,
            TaskExecutorService processorExecutor,
            T[] inputs) {
        return create(
                processorExecutor,
                SyncTaskExecutor.getSimpleExecutor(),
                new ObjectChannelOpener<>(millisPerInput, inputs, closeThrowsException),
                new ObjectChannelProcessor<T>());
    }

    private static <T> AsyncChannelLink<T> createFailChannelLink(
            TaskExecutorService processorExecutor) {
        return new AsyncChannelLink<>(
                processorExecutor,
                SyncTaskExecutor.getSimpleExecutor(),
                new FailedChannelOpener<T>(),
                new ObjectChannelProcessor<T>());
    }

    private static TaskExecutorService createAsyncExecutor(int threadCount) {
        return new ThreadPoolTaskExecutor("AsyncChannelLinkTest Executor", threadCount);
    }

    private static <T> void testChannelLink(
            boolean closeThrowsException,
            int threadCount,
            int millisPerInput,
            T[] inputs,
            TestTask<T> testTask) {
        TaskExecutorService processorExecutor = createAsyncExecutor(threadCount);
        try {
            AsyncChannelLink<T> dataLink = createChannelLink(
                    closeThrowsException, millisPerInput, processorExecutor, inputs);
            testTask.doTest(dataLink);
        } finally {
            processorExecutor.shutdown();
            processorExecutor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    private static <T> void testFailChannelLink(
            int threadCount,
            TestTask<T> testTask) {
        TaskExecutorService processorExecutor = createAsyncExecutor(threadCount);
        try {
            AsyncChannelLink<T> dataLink = createFailChannelLink(processorExecutor);
            testTask.doTest(dataLink);
        } finally {
            processorExecutor.shutdown();
            processorExecutor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    private static Integer[] getIntegerArray(int size) {
        Integer[] result = new Integer[size];
        for (int i = 0; i < size; i++) {
            result[i] = i;
        }
        return result;
    }

    private static void runSimpleTest(int threadCount, int dataCount) {
        Integer[] inputs = getIntegerArray(dataCount);
        final List<Integer> received = new LinkedList<>();
        final List<AsyncReport> receivedReports = new LinkedList<>();

        final AtomicReference<AsyncDataController> controllerRef = new AtomicReference<>(null);
        testChannelLink(false, threadCount, 0, inputs, (AsyncChannelLink<Integer> linkToTest) -> {
            AsyncDataController controller = linkToTest.getData(Cancellation.UNCANCELABLE_TOKEN,
                    new AsyncDataListener<Integer>() {
                @Override
                public void onDataArrive(Integer data) {
                    received.add(data);
                }

                @Override
                public void onDoneReceive(AsyncReport report) {
                    receivedReports.add(report);
                }
            });
            controller.controlData(new Object());
            controllerRef.set(controller);
        });

        AsyncDataState state = controllerRef.get().getDataState();
        assertEquals(1.0, state.getProgress(), 0.00001);

        assertEquals("Invalid received datas.", Arrays.asList(inputs), received);
        assertEquals("Multiple recevied reports", 1, receivedReports.size());

        AsyncReport receivedReport = receivedReports.get(0);
        assertTrue("Invalid data report",
                receivedReport.isSuccess());
    }

    private static void runCancelTest(
            int threadCount,
            int dataCount,
            boolean closeThrowsException,
            final boolean cancelDuringFirstData) {
        Integer[] inputs = getIntegerArray(dataCount);
        final List<Integer> received = new LinkedList<>();
        final List<AsyncReport> receivedReports = new LinkedList<>();

        int readMillis = cancelDuringFirstData ? 0 : 1000;
        testChannelLink(closeThrowsException, threadCount, readMillis, inputs, (linkToTest) -> {
            final CancellationSource cancelSource = Cancellation.createCancellationSource();

            if (cancelDuringFirstData) {
                final WaitableSignal receivedDataSignal = new WaitableSignal();
                final WaitableSignal canceledSignal = new WaitableSignal();

                linkToTest.getData(cancelSource.getToken(),
                        new AsyncDataListener<Integer>() {
                            @Override
                            public void onDataArrive(Integer data) {
                                receivedDataSignal.signal();
                                received.add(data);
                                canceledSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
                            }

                            @Override
                            public void onDoneReceive(AsyncReport report) {
                                receivedReports.add(report);
                            }
                        });
                new Thread(() -> {
                    receivedDataSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
                    cancelSource.getController().cancel();
                    canceledSignal.signal();
                }).start();
            }
            else {
                linkToTest.getData(cancelSource.getToken(),
                        new AsyncDataListener<Integer>() {
                            @Override
                            public void onDataArrive(Integer data) {
                                received.add(data);
                            }

                            @Override
                            public void onDoneReceive(AsyncReport report) {
                                receivedReports.add(report);
                            }
                        });
                cancelSource.getController().cancel();
            }
        });

        assertTrue("Too many inputs.", received.size() <= inputs.length);
        assertEquals("Invalid received datas.",
                Arrays.asList(inputs).subList(0, received.size()),
                received);
        assertTrue("Multiple recevied reports", receivedReports.size() <= 1);

        AsyncReport receivedReport = receivedReports.get(0);
        assertTrue("Invalid data report",
                receivedReport.isCanceled() && receivedReport.getException() == null);
    }

    private static void runFailTest(
            int threadCount) {
        final List<Integer> received = new LinkedList<>();
        final List<AsyncReport> receivedReports = new LinkedList<>();

        testFailChannelLink(threadCount, (AsyncChannelLink<Integer> linkToTest) -> {
            linkToTest.getData(Cancellation.UNCANCELABLE_TOKEN,
                    new AsyncDataListener<Integer>() {
                @Override
                public void onDataArrive(Integer data) {
                    received.add(data);
                }

                @Override
                public void onDoneReceive(AsyncReport report) {
                    receivedReports.add(report);
                }
            });
        });

        assertEquals("Too many inputs.", 0, received.size());
        assertEquals("Multiple recevied reports", 1, receivedReports.size());

        AsyncReport receivedReport = receivedReports.get(0);
        assertTrue("Invalid data report",
                !receivedReport.isCanceled()
                && receivedReport.getException() instanceof FailChannelException);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor1() {
        create(null,
                mock(TaskExecutorService.class),
                mockChannelOpener(),
                mockChannelProcessor());
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(mock(TaskExecutorService.class),
                null,
                mockChannelOpener(),
                mockChannelProcessor());
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor3() {
        create(mock(TaskExecutorService.class),
                mock(TaskExecutorService.class),
                null,
                mockChannelProcessor());
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor4() {
        create(mock(TaskExecutorService.class),
                mock(TaskExecutorService.class),
                mockChannelOpener(),
                null);
    }

    @Test
    public void testPreCanceled() {
        SyncTaskExecutor executor = new SyncTaskExecutor();

        AsyncChannelLink<Object> link = createChannelLink(false, 0, executor, new Object[]{new Object()});

        AsyncDataListener<Object> listener = mockListener();
        AsyncDataController controller = link.getData(Cancellation.CANCELED_TOKEN, listener);
        controller.controlData(new Object());

        ArgumentCaptor<AsyncReport> receivedReport = ArgumentCaptor.forClass(AsyncReport.class);
        verify(listener).onDoneReceive(receivedReport.capture());
        verifyNoMoreInteractions(listener);

        assertTrue(receivedReport.getValue().isCanceled());
        assertNull(receivedReport.getValue().getException());
    }

    @Test
    public void testSimple() {
        for (int i = 0; i < 100; i++) {
            runSimpleTest(1, 1000);
        }
    }

    @Test(timeout = 20000)
    public void testCancelCloseThrowsException() {
        try (LogCollector logs = LogTests.startCollecting()) {
            runCancelTest(1, 10, true, true);
            assertEquals(1, logs.getNumberOfLogs(Level.WARNING));
        }
    }

    @Test(timeout = 20000)
    public void testCancel() {
        for (int i = 0; i < 1000; i++) {
            runCancelTest(1, 10, false, false);
        }

        for (int i = 0; i < 10; i++) {
            runCancelTest(1, 10, false, true);
        }
    }

    @Test
    public void testFailure() {
        for (int i = 0; i < 100; i++) {
            runFailTest(1);
        }
    }

    private interface TestTask<T> {
        public void doTest(AsyncChannelLink<T> linkToTest);
    }

    private static final class ObjectChannelOpener<T>
    implements
            ChannelOpener<ObjectReadChannel<T>> {

        private final int millisPerInput;
        private final List<T> inputs;
        private final boolean closeThrowsException;

        public ObjectChannelOpener(int millisPerInput, T[] inputs, boolean closeThrowsException) {
            this.millisPerInput = millisPerInput;
            this.inputs = new ArrayList<>(Arrays.asList(inputs));
            this.closeThrowsException = closeThrowsException;
        }

        @Override
        public ObjectReadChannel<T> openChanel() {
            return new StaticObjectReadChannel<>(millisPerInput, inputs, closeThrowsException);
        }
    }

    private static final class ObjectChannelProcessor<T>
    implements
            ChannelProcessor<T, ObjectReadChannel<T>> {

        @Override
        public void processChannel(
                ObjectReadChannel<T> channel,
                AsyncDataListener<T> listener,
                StateListener stateListener) throws IOException {

            int objectCount = channel.getRemainingCount();
            int processedCount = 0;

            for (T current = channel.readNextObject();
                    current != null;
                    current = channel.readNextObject()) {
                double progressValue = (double)processedCount / (double)objectCount;
                stateListener.setState(new SimpleDataState("Retrieving data.", progressValue));
                listener.onDataArrive(current);
            }
            stateListener.setState(new SimpleDataState("Data retrieval has completed.", 1.0));
        }
    }

    private interface ObjectReadChannel<T> extends Channel {
        public int getRemainingCount();
        public T readNextObject() throws IOException;
    }

    private static final class FailedChannelOpener<T>
    implements
            ChannelOpener<ObjectReadChannel<T>> {

        @Override
        public ObjectReadChannel<T> openChanel() throws IOException {
            throw new FailChannelException();
        }
    }

    private static final class StaticObjectReadChannel<T>
    implements
            ObjectReadChannel<T>, InterruptibleChannel {
        private final List<T> inputs;
        private final AtomicInteger currentInput;
        private final Lock closeLock;
        private final Condition closeSignal;
        private volatile boolean closed;

        private final long readTimeNanos;
        private final boolean closeThrowsException;

        public StaticObjectReadChannel(
                int readTimeMS,
                List<? extends T> inputs,
                boolean closeThrowsException) {

            this.closeLock = new ReentrantLock();
            this.closeSignal = closeLock.newCondition();
            this.readTimeNanos = TimeUnit.NANOSECONDS.convert(readTimeMS, TimeUnit.MILLISECONDS);
            this.inputs = new ArrayList<>(inputs);
            this.currentInput = new AtomicInteger(0);
            this.closed = false;
            this.closeThrowsException = closeThrowsException;
        }

        private static int getAndIncWithLimit(AtomicInteger value, int limit) {
            int currentValue;
            int nextValue;
            do {
                currentValue = value.get();
                nextValue = currentValue + 1;
                if (nextValue > limit) {
                    return limit;
                }
            } while (!value.compareAndSet(currentValue, nextValue));
            return currentValue;
        }

        @Override
        public int getRemainingCount() {
            return inputs.size() - currentInput.get();
        }

        private void trySleep() {
            long toWaitNanos = readTimeNanos;
            if (toWaitNanos <= 0) {
                return;
            }

            closeLock.lock();
            try {
                while (!closed && toWaitNanos > 0) {
                    toWaitNanos = closeSignal.awaitNanos(toWaitNanos);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                closeLock.unlock();
            }
        }

        @Override
        public T readNextObject() throws IOException {
            trySleep();

            if (closed) {
                throw new ClosedChannelException();
            }
            int index = getAndIncWithLimit(currentInput, inputs.size());
            return index < inputs.size() ? inputs.get(index) : null;
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public void close() {
            if (!closed) {
                boolean wasClosed;
                closeLock.lock();
                try {
                    wasClosed = closed;
                    closed = true;
                    closeSignal.signalAll();
                } finally {
                    closeLock.unlock();
                }

                if (!wasClosed && closeThrowsException) {
                    throw new RuntimeException("AsyncChannelLinkTest - close exception");
                }
            }
        }
    }

    private static class FailChannelException extends IOException {
        private static final long serialVersionUID = -7770961451322743809L;
    }
}
