package org.jtrim.concurrent.async.io;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.concurrent.ExecutorsEx;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.concurrent.async.SimpleDataState;
import org.jtrim.utils.ShutdownHelper;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class AsyncChannelLinkTest {

    public AsyncChannelLinkTest() {
    }

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

    @SafeVarargs
    private static <T> AsyncChannelLink<T> createChannelLink(
            int millisPerInput,
            ExecutorService processorExecutor,
            T... inputs) {
        return new AsyncChannelLink<>(
                processorExecutor,
                SyncTaskExecutor.getSimpleExecutor(),
                new ObjectChannelOpener<>(millisPerInput, inputs),
                new ObjectChannelProcessor<T>());
    }

    private static <T> AsyncChannelLink<T> createFailChannelLink(
            ExecutorService processorExecutor) {
        return new AsyncChannelLink<>(
                processorExecutor,
                SyncTaskExecutor.getSimpleExecutor(),
                new FailedChannelOpener<T>(),
                new ObjectChannelProcessor<T>());
    }

    private static ExecutorService createAsyncExecutor(int threadCount) {
        return ExecutorsEx.newMultiThreadedExecutor(1, false, "AsyncChannelLinkTest Executor");
    }

    private static <T> void testChannelLink(
            int threadCount,
            int millisPerInput,
            T[] inputs,
            TestTask<T> testTask) {
        ExecutorService processorExecutor = createAsyncExecutor(threadCount);
        try {
            AsyncChannelLink<T> dataLink = createChannelLink(millisPerInput, processorExecutor, inputs);
            testTask.doTest(dataLink);
        } finally {
            processorExecutor.shutdown();
            ShutdownHelper.awaitTerminateExecutorsSilently(processorExecutor);
        }
    }

    private static <T> void testFailChannelLink(
            int threadCount,
            TestTask<T> testTask) {
        ExecutorService processorExecutor = createAsyncExecutor(threadCount);
        try {
            AsyncChannelLink<T> dataLink = createFailChannelLink(processorExecutor);
            testTask.doTest(dataLink);
        } finally {
            processorExecutor.shutdown();
            ShutdownHelper.awaitTerminateExecutorsSilently(processorExecutor);
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

        testChannelLink(threadCount, 0, inputs,
                new TestTask<Integer>() {
            @Override
            public void doTest(AsyncChannelLink<Integer> linkToTest) {
                linkToTest.getData(new AsyncDataListener<Integer>() {
                    @Override
                    public boolean requireData() {
                        return true;
                    }

                    @Override
                    public void onDataArrive(Integer data) {
                        received.add(data);
                    }

                    @Override
                    public void onDoneReceive(AsyncReport report) {
                        receivedReports.add(report);
                    }
                });
            }
        });

        assertEquals("Invalid received datas.", Arrays.asList(inputs), received);
        assertEquals("Multiple recevied reports", 1, receivedReports.size());

        AsyncReport receivedReport = receivedReports.get(0);
        assertTrue("Invalid data report",
                receivedReport.isSuccess());
    }

    private static void runCancelTest(
            int threadCount,
            int dataCount) {
        Integer[] inputs = getIntegerArray(dataCount);
        final List<Integer> received = new LinkedList<>();
        final List<AsyncReport> receivedReports = new LinkedList<>();

        testChannelLink(threadCount, 1000, inputs,
                new TestTask<Integer>() {
            @Override
            public void doTest(AsyncChannelLink<Integer> linkToTest) {
                AsyncDataController controller = linkToTest.getData(new AsyncDataListener<Integer>() {
                    @Override
                    public boolean requireData() {
                        return true;
                    }

                    @Override
                    public void onDataArrive(Integer data) {
                        received.add(data);
                    }

                    @Override
                    public void onDoneReceive(AsyncReport report) {
                        receivedReports.add(report);
                    }
                });
                controller.cancel();
            }
        });

        assertTrue("Too many inputs.", received.size() <= inputs.length);
        assertEquals("Invalid received datas.",
                Arrays.asList(inputs).subList(0, received.size()),
                received);
        assertEquals("Multiple recevied reports", 1, receivedReports.size());

        AsyncReport receivedReport = receivedReports.get(0);
        assertTrue("Invalid data report",
                receivedReport.isCanceled() && receivedReport.getException() == null);
    }

    private static void runFailTest(
            int threadCount) {
        final List<Integer> received = new LinkedList<>();
        final List<AsyncReport> receivedReports = new LinkedList<>();

        testFailChannelLink(threadCount,
                new TestTask<Integer>() {
            @Override
            public void doTest(AsyncChannelLink<Integer> linkToTest) {
                linkToTest.getData(new AsyncDataListener<Integer>() {
                    @Override
                    public boolean requireData() {
                        return true;
                    }

                    @Override
                    public void onDataArrive(Integer data) {
                        received.add(data);
                    }

                    @Override
                    public void onDoneReceive(AsyncReport report) {
                        receivedReports.add(report);
                    }
                });
            }
        });

        assertEquals("Too many inputs.", 0, received.size());
        assertEquals("Multiple recevied reports", 1, receivedReports.size());

        AsyncReport receivedReport = receivedReports.get(0);
        assertTrue("Invalid data report",
                !receivedReport.isCanceled()
                && receivedReport.getException() instanceof FailChannelException);
    }

    @Test
    public void testSimple() {
        for (int i = 0; i < 100; i++) {
            runSimpleTest(1, 1000);
        }
    }

    @Test
    public void testCancel() {
        for (int i = 0; i < 100; i++) {
            runCancelTest(1, 10);
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
        private final T[] inputs;

        @SafeVarargs
        public ObjectChannelOpener(int millisPerInput, T... inputs) {
            this.millisPerInput = millisPerInput;
            this.inputs = inputs.clone();
        }

        @Override
        public ObjectReadChannel<T> openChanel() {
            return new StaticObjectReadChannel<>(millisPerInput, inputs);
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

    private static final class StaticObjectReadChannel<T> implements ObjectReadChannel<T> {
        private final T[] inputs;
        private AtomicInteger currentInput;
        private final Lock closeLock;
        private final Condition closeSignal;
        private volatile boolean closed;

        private final long readTimeNanos;

        @SafeVarargs
        public StaticObjectReadChannel(int readTimeMS, T... inputs) {
            this.closeLock = new ReentrantLock();
            this.closeSignal = closeLock.newCondition();
            this.readTimeNanos = TimeUnit.NANOSECONDS.convert(readTimeMS, TimeUnit.MILLISECONDS);
            this.inputs = inputs.clone();
            this.currentInput = new AtomicInteger(0);
            this.closed = false;
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
            return inputs.length - currentInput.get();
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
            int index = getAndIncWithLimit(currentInput, inputs.length);
            return index < inputs.length ? inputs[index] : null;
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public void close() {
            if (!closed) {
                closeLock.lock();
                try {
                    closed = true;
                    closeSignal.signalAll();
                } finally {
                    closeLock.unlock();
                }
            }
        }
    }

    private static class FailChannelException extends IOException {
        private static final long serialVersionUID = -7770961451322743809L;
    }
}
