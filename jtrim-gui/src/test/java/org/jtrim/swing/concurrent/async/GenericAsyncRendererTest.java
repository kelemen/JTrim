package org.jtrim.swing.concurrent.async;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.TaskExecutorService;
import org.jtrim.concurrent.ThreadPoolTaskExecutor;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncDataState;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.concurrent.async.SimpleDataState;
import org.jtrim.utils.ExceptionHelper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class GenericAsyncRendererTest {

    public GenericAsyncRendererTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static <DataType> TestRenderer<DataType> createRenderer() {
        return createRenderer(null);
    }

    private static <DataType> TestRenderer<DataType> createRenderer(DataRenderer<DataType> renderer) {
        return new TestRenderer<>(renderer, 5000, TimeUnit.MILLISECONDS);
    }

    private static TaskExecutorService createParallelExecutor(int maxThreadCount) {
        return new ThreadPoolTaskExecutor("Test Executor", maxThreadCount);
    }

    private static List<Integer> createTestDatas(int dataCount) {
        List<Integer> datas = new ArrayList<>(dataCount);
        for (int i = 0; i < dataCount; i++) {
            datas.add(i);
        }
        return datas;
    }

    private static boolean findElement(Iterator<?> itr, Object element) {
        while (itr.hasNext()) {
            Object current = itr.next();
            if (element == current) {
                return true;
            }
        }
        return false;
    }

    private static void checkReceivedDatas(
            List<?> expected,
            List<?> received) {

        Iterator<?> expectedItr = expected.iterator();
        for (Object current: received) {
            if (!findElement(expectedItr, current)) {
                fail("Invalid received datas. "
                        + "Expected = " + expected
                        + ", Received = " + received);
            }
        }
    }

    private static void checkNotCanceledReceivedDatas(
            List<?> expected,
            List<?> received) {
        checkReceivedDatas(expected, received);
        if (expected.isEmpty() && received.isEmpty()) {
            return;
        }

        Object lastExpected = expected.get(expected.size() - 1);
        Object lastReceived = received.get(received.size() - 1);
        assertSame("The final received data must be the final expected data.", lastExpected, lastReceived);
    }

    private void checkCommonErrors(TestRenderer<?> renderer) {
        List<String> miscErrors = renderer.getMiscErrors();
        if (!miscErrors.isEmpty()) {
            fail(miscErrors.toString());
        }

        if (renderer.isStartRenderingCalled()) {
            assertTrue("Missing finishRendering call.", renderer.isFinishRenderingCalled());
        }
    }

    private static boolean isFinished(RenderingState... states) {
        for (RenderingState state: states) {
            if (state.isRenderingFinished()) {
                return true;
            }
        }
        return false;
    }

    private static void waitForFinished(RenderingState... states) throws InterruptedException {
        while (!isFinished(states)) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    private void doTestWithoutData(TaskExecutor executor) throws InterruptedException {
        AsyncRenderer asyncRenderer = new GenericAsyncRenderer(executor);

        TestRenderer<Object> renderer = createRenderer();
        renderer.allowAll();

        RenderingState state = asyncRenderer.render(
                Boolean.TRUE,
                Cancellation.UNCANCELABLE_TOKEN,
                null,
                renderer);
        AsyncReport report = renderer.awaitDone();

        waitForFinished(state);

        assertTrue("Rendering time is required to be non-negative", state.getRenderingTime(TimeUnit.NANOSECONDS) >= 0);
        assertNotNull(report);
        assertEquals(true, report.isSuccess());
        assertEquals("Unexpected data", 0, renderer.getReceivedDatas().size());
        checkCommonErrors(renderer);
    }

    private void doTestWithData(TaskExecutor executor, TaskExecutor dataExecutor, int dataCount) throws InterruptedException {
        AsyncRenderer asyncRenderer = new GenericAsyncRenderer(executor);
        List<Integer> datas = createTestDatas(dataCount);
        AsyncDataLink<Integer> dataLink = new TestDataLink<>(dataExecutor, datas);

        TestRenderer<Integer> renderer = createRenderer();
        renderer.allowAll();

        RenderingState state = asyncRenderer.render(
                Boolean.TRUE,
                Cancellation.UNCANCELABLE_TOKEN,
                dataLink,
                renderer);
        AsyncReport report = renderer.awaitDone();

        waitForFinished(state);

        assertTrue("Rendering time is required to be non-negative", state.getRenderingTime(TimeUnit.NANOSECONDS) >= 0);
        assertNotNull(report);
        assertEquals(true, report.isSuccess());
        checkNotCanceledReceivedDatas(datas, renderer.getReceivedDatas());
        checkCommonErrors(renderer);
    }

    @Test(timeout = 10000)
    public void testSyncWithoutData() throws InterruptedException {
        doTestWithoutData(SyncTaskExecutor.getSimpleExecutor());
    }

    @Test(timeout = 10000)
    public void testWithoutData() throws InterruptedException {
        TaskExecutorService executor = createParallelExecutor(1);
        try {
            doTestWithoutData(executor);
        } finally {
            executor.shutdown();
        }
    }

    @Test(timeout = 10000)
    public void testSyncWithData() throws InterruptedException {
        doTestWithData(SyncTaskExecutor.getSimpleExecutor(), SyncTaskExecutor.getSimpleExecutor(), 5);
    }

    @Test(timeout = 10000)
    public void testWithData() throws InterruptedException {
        TaskExecutorService executor = createParallelExecutor(2);
        try {
            doTestWithData(executor, executor, 10);
        } finally {
            executor.shutdown();
        }
    }

    @Test(timeout = 10000)
    public void testOverwriteRender() throws InterruptedException {
        TaskExecutorService executor = createParallelExecutor(2);
        try {
            Object renderingKey = Boolean.TRUE;

            AsyncRenderer asyncRenderer = new GenericAsyncRenderer(executor);

            AtomicInteger currentIndex = new AtomicInteger(0);
            Queue<String> orderErrors = new ConcurrentLinkedQueue<>();

            TestRenderer<Integer> renderer1 = createRenderer(
                    new OrderListenerRenderer<Integer>(1, currentIndex, orderErrors));
            TestRenderer<Integer> renderer2 = createRenderer(
                    new OrderListenerRenderer<Integer>(2, currentIndex, orderErrors));
            TestRenderer<Integer> renderer3 = createRenderer(
                    new OrderListenerRenderer<Integer>(3, currentIndex, orderErrors));

            RenderingState state1 = asyncRenderer.render(
                renderingKey,
                Cancellation.UNCANCELABLE_TOKEN,
                null,
                renderer1);
            RenderingState state2 = asyncRenderer.render(
                renderingKey,
                Cancellation.UNCANCELABLE_TOKEN,
                null,
                renderer2);
            RenderingState state3 = asyncRenderer.render(
                renderingKey,
                Cancellation.UNCANCELABLE_TOKEN,
                null,
                renderer3);

            renderer1.allowAll();
            renderer2.allowAll();
            renderer3.allowAll();

            AsyncReport report = renderer3.awaitDone();

            waitForFinished(state1, state2, state3);

            assertNotNull(report);
            assertEquals(true, report.isSuccess());

            assertTrue("Second rendering request must be skipped.", !renderer2.isStartRenderingCalled());

            checkCommonErrors(renderer1);
            checkCommonErrors(renderer2);
            checkCommonErrors(renderer3);

            if (!orderErrors.isEmpty()) {
                fail(orderErrors.toString());
            }
        } finally {
            executor.shutdown();
        }
    }

    private static final class OrderListenerRenderer<DataType> implements DataRenderer<DataType> {
        private final int ourIndex;
        private final AtomicInteger currentIndex;
        private final Queue<String> errorList;

        public OrderListenerRenderer(
                int ourIndex,
                AtomicInteger currentIndex,
                Queue<String> errorList) {

            this.ourIndex = ourIndex;
            this.currentIndex = currentIndex;
            this.errorList = errorList;
        }

        @Override
        public boolean startRendering() {
            int prevIndex;
            do {
                prevIndex = currentIndex.get();
                if (prevIndex >= ourIndex) {
                    errorList.add("The renderer was called at the wrong time."
                            + "Current index = " + prevIndex
                            + ", Index = " + ourIndex);
                }
            } while (!currentIndex.compareAndSet(prevIndex, ourIndex));
            return true;
        }

        @Override
        public boolean willDoSignificantRender(DataType data) {
            return true;
        }

        @Override
        public boolean render(DataType data) {
            return true;
        }

        @Override
        public void finishRendering(AsyncReport report) {
        }
    }

    private static class TestDataLink<DataType> implements AsyncDataLink<DataType> {
        private final TaskExecutor executor;
        private final List<DataType> datas;

        public TestDataLink(TaskExecutor executor, List<DataType> datas) {
            this.executor = executor;
            this.datas = new ArrayList<>(datas);
        }

        @Override
        public AsyncDataController getData(
                CancellationToken cancelToken,
                final AsyncDataListener<? super DataType> dataListener) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(dataListener, "dataListener");

            final AtomicInteger step = new AtomicInteger(0);

            executor.execute(cancelToken, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    for (DataType data: datas) {
                        cancelToken.checkCanceled();

                        dataListener.onDataArrive(data);
                        step.incrementAndGet();
                    }
                }
            }, new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) {
                    dataListener.onDoneReceive(AsyncReport.getReport(error, canceled));
                }
            });

            return new AsyncDataController() {
                @Override
                public void controlData(Object controlArg) {
                }

                @Override
                public AsyncDataState getDataState() {
                    double progress = (double)step.get() / (double)datas.size();
                    return new SimpleDataState("", progress);
                }
            };
        }
    }
}
