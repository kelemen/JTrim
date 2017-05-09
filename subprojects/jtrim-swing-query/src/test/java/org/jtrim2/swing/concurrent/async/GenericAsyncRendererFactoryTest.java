package org.jtrim2.swing.concurrent.async;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.query.AsyncDataController;
import org.jtrim2.concurrent.query.AsyncDataConverter;
import org.jtrim2.concurrent.query.AsyncDataLink;
import org.jtrim2.concurrent.query.AsyncDataListener;
import org.jtrim2.concurrent.query.AsyncDataState;
import org.jtrim2.concurrent.query.AsyncLinks;
import org.jtrim2.concurrent.query.AsyncReport;
import org.jtrim2.concurrent.query.SimpleDataController;
import org.jtrim2.concurrent.query.SimpleDataState;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.UnregisteredListenerRef;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.ThreadPoolTaskExecutor;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class GenericAsyncRendererFactoryTest {
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
        AsyncRenderer asyncRenderer = new GenericAsyncRendererFactory(executor).createRenderer();

        TestRenderer<Object> renderer = createRenderer();
        renderer.allowAll();

        RenderingState state = asyncRenderer.render(
                Cancellation.UNCANCELABLE_TOKEN,
                null,
                renderer);
        AsyncReport report = renderer.awaitDone();

        waitForFinished(state);

        assertTrue("Rendering time is required to be non-negative",
                state.getRenderingTime(TimeUnit.NANOSECONDS) >= 0);
        assertNotNull(report);
        assertEquals(true, report.isSuccess());
        assertEquals("Unexpected data", 0, renderer.getReceivedDatas().size());
        checkCommonErrors(renderer);
    }

    private void doTestWithData(
            TaskExecutor executor,
            TaskExecutor dataExecutor,
            int dataCount) throws InterruptedException {

        AsyncRenderer asyncRenderer = new GenericAsyncRendererFactory(executor).createRenderer();
        List<Integer> datas = createTestDatas(dataCount);
        AsyncDataLink<Integer> dataLink = new TestDataLink<>(dataExecutor, datas);

        TestRenderer<Integer> renderer = createRenderer();
        renderer.allowAll();

        RenderingState state = asyncRenderer.render(
                Cancellation.UNCANCELABLE_TOKEN,
                dataLink,
                renderer);
        AsyncReport report = renderer.awaitDone();

        waitForFinished(state);

        assertTrue("Rendering time is required to be non-negative",
                state.getRenderingTime(TimeUnit.NANOSECONDS) >= 0);
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
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
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
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    @Test(timeout = 10000)
    public void testOverwriteRender() throws InterruptedException {
        TaskExecutorService executor = createParallelExecutor(2);
        try {
            AsyncRenderer asyncRenderer = new GenericAsyncRendererFactory(executor).createRenderer();

            AtomicInteger currentIndex = new AtomicInteger(0);
            Queue<String> orderErrors = new ConcurrentLinkedQueue<>();

            TestRenderer<Integer> renderer1 = createRenderer(
                    new OrderListenerRenderer<Integer>(1, currentIndex, orderErrors));
            TestRenderer<Integer> renderer2 = createRenderer(
                    new OrderListenerRenderer<Integer>(2, currentIndex, orderErrors));
            TestRenderer<Integer> renderer3 = createRenderer(
                    new OrderListenerRenderer<Integer>(3, currentIndex, orderErrors));

            RenderingState state1 = asyncRenderer.render(
                Cancellation.UNCANCELABLE_TOKEN,
                null,
                renderer1);
            RenderingState state2 = asyncRenderer.render(
                Cancellation.UNCANCELABLE_TOKEN,
                null,
                renderer2);
            RenderingState state3 = asyncRenderer.render(
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
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> DataRenderer<T> mockRenderer() {
        return mock(DataRenderer.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> DataRenderer<T> mockDummyRenderer(boolean significant, final CountDownLatch finishLatch) {
        DataRenderer<T> renderer = mockRenderer();
        stub(renderer.willDoSignificantRender((T)any())).toReturn(significant);
        stub(renderer.startRendering(any(CancellationToken.class))).toReturn(significant);
        stub(renderer.render(any(CancellationToken.class), (T)any())).toReturn(significant);
        if (finishLatch != null) {
            final Runnable finishTask = Tasks.runOnceTask(finishLatch::countDown, false);
            doAnswer((InvocationOnMock invocation) -> {
                finishTask.run();
                return null;
            }).when(renderer).finishRendering(any(CancellationToken.class), any(AsyncReport.class));
        }
        return renderer;
    }

    private static AsyncDataLink<Object> asyncPreparedLink(TaskExecutorService executor, Object[] datas) {
        List<AsyncDataConverter<Void, Object>> converters = new ArrayList<>(datas.length);
        for (final Object currentData: datas) {
            converters.add(new AsyncDataConverter<>((Void data) -> currentData, executor));
        }
        return AsyncLinks.convertGradually(null, converters);
    }

    private void testConcurrent(
            int numberOfThreads,
            int numberOfDatas,
            final boolean significant,
            DataLinkFactory linkFactory) throws InterruptedException {
        Object[] datas = new Object[numberOfDatas];
        for (int i = 0; i < datas.length; i++) {
            datas[i] = new Object();
        }

        List<DataRenderer<Object>> renderers = new ArrayList<>(numberOfThreads);

        TaskExecutorService executor = createParallelExecutor(numberOfThreads);
        try {
            final AsyncDataLink<Object> dataLink = linkFactory.createLink(executor, datas);
            GenericAsyncRendererFactory rendererFactory = new GenericAsyncRendererFactory(executor);

            final CountDownLatch taskLatch = new CountDownLatch(numberOfThreads);
            Runnable[] tasks = new Runnable[numberOfThreads];
            for (int i = 0; i < numberOfThreads; i++) {
                final AsyncRenderer asyncRenderer = rendererFactory.createRenderer();
                final DataRenderer<Object> renderer = mockDummyRenderer(true, taskLatch);

                renderers.add(renderer);
                tasks[i] = () -> {
                    asyncRenderer.render(
                            Cancellation.UNCANCELABLE_TOKEN,
                            dataLink,
                            mockDummyRenderer(significant, null));
                    asyncRenderer.render(
                            Cancellation.UNCANCELABLE_TOKEN,
                            dataLink,
                            renderer);
                };
            }

            Tasks.runConcurrently(tasks);
            taskLatch.await();
        } finally {
            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }

        for (DataRenderer<Object> renderer: renderers) {
            ArgumentCaptor<Object> renderArgsCaptor = ArgumentCaptor.forClass(Object.class);

            InOrder inOrder = inOrder(renderer);
            inOrder.verify(renderer)
                    .startRendering(any(CancellationToken.class));
            inOrder.verify(renderer, atLeastOnce())
                    .render(any(CancellationToken.class), renderArgsCaptor.capture());
            inOrder.verify(renderer)
                    .finishRendering(any(CancellationToken.class), any(AsyncReport.class));

            assertSame(datas[datas.length - 1], renderArgsCaptor.getValue());
        }
    }

    private void testConcurrent1(
            int numberOfThreads,
            int numberOfDatas,
            boolean significant) throws InterruptedException {

        testConcurrent(numberOfThreads, numberOfDatas, significant, (executor, datas) -> {
            if (datas.length == 1) {
                return AsyncLinks.createPreparedLink(datas[0], null);
            }

            final Object[] preparedData = datas.clone();

            return (CancellationToken cancelToken, final AsyncDataListener<? super Object> dataListener) -> {
                Throwable receiveError = null;
                try {
                    for (Object data: preparedData) {
                        dataListener.onDataArrive(data);
                    }
                } catch (Throwable ex) {
                    receiveError = ex;
                } finally {
                    dataListener.onDoneReceive(AsyncReport.getReport(
                            receiveError, cancelToken.isCanceled()));
                }
                return new SimpleDataController();
            };
        });
    }

    private void testConcurrent2(
            int numberOfThreads,
            int numberOfDatas,
            boolean significant) throws InterruptedException {

        testConcurrent(numberOfThreads, numberOfDatas, significant, (executor, datas) -> {
            if (datas.length == 1) {
                final Object data = datas[0];
                return (CancellationToken cancelToken, final AsyncDataListener<? super Object> dataListener) -> {
                    executor.execute(cancelToken, (CancellationToken subTaskCancelToken) -> {
                        dataListener.onDataArrive(data);
                    }, (boolean canceled, Throwable error) -> {
                        dataListener.onDoneReceive(AsyncReport.getReport(error, canceled));
                    });
                    return new SimpleDataController();
                };
            }
            return asyncPreparedLink(executor, datas);
        });
    }

    @Test(timeout = 30000)
    public void testConcurrent1Significant() throws InterruptedException {
        int numberOfThreads = 2 * Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 100; i++) {
            testConcurrent1(numberOfThreads, 1, true);
            testConcurrent1(numberOfThreads, 5, true);
        }
    }

    @Test(timeout = 30000)
    public void testConcurrent2Significant() throws InterruptedException {
        int numberOfThreads = 2 * Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 100; i++) {
            testConcurrent2(numberOfThreads, 1, true);
            testConcurrent2(numberOfThreads, 5, true);
        }
    }

    @Test(timeout = 30000)
    public void testConcurrent1Insignificant() throws InterruptedException {
        int numberOfThreads = 2 * Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 100; i++) {
            testConcurrent1(numberOfThreads, 1, false);
            testConcurrent1(numberOfThreads, 5, false);
        }
    }

    @Test(timeout = 30000)
    public void testConcurrent2Insignificant() throws InterruptedException {
        int numberOfThreads = 2 * Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < 100; i++) {
            testConcurrent2(numberOfThreads, 1, false);
            testConcurrent2(numberOfThreads, 5, false);
        }
    }

    @Test
    public void testCanceledBeforeSubmit() {
        GenericAsyncRendererFactory rendererFactory
                = new GenericAsyncRendererFactory(SyncTaskExecutor.getSimpleExecutor());

        Object data = new Object();
        AsyncDataLink<Object> dataLink = AsyncLinks.createPreparedLink(data, null);
        DataRenderer<Object> renderer1 = mockRenderer();
        DataRenderer<Object> renderer2 = mockDummyRenderer(true, null);

        rendererFactory.createRenderer()
                .render(Cancellation.CANCELED_TOKEN, dataLink, renderer1);
        rendererFactory.createRenderer()
                .render(Cancellation.UNCANCELABLE_TOKEN, dataLink, renderer2);

        verifyZeroInteractions(renderer1);

        InOrder inOrder = inOrder(renderer2);
        inOrder.verify(renderer2).startRendering(any(CancellationToken.class));
        inOrder.verify(renderer2).render(any(CancellationToken.class), same(data));
        inOrder.verify(renderer2).finishRendering(any(CancellationToken.class), any(AsyncReport.class));
    }

    @Test
    public void testAsyncState() {
        ManualTaskExecutor executor = new ManualTaskExecutor(false);
        GenericAsyncRendererFactory rendererFactory = new GenericAsyncRendererFactory(executor);

        Object data = new Object();
        AsyncDataState state = new SimpleDataState("", 1.0);
        AsyncDataLink<Object> dataLink = AsyncLinks.createPreparedLink(data, state);
        DataRenderer<Object> renderer1 = mockDummyRenderer(true, null);
        DataRenderer<Object> renderer2 = mockDummyRenderer(true, null);

        AsyncRenderer asyncRenderer = rendererFactory.createRenderer();
        RenderingState renderingState1 = asyncRenderer
                .render(Cancellation.UNCANCELABLE_TOKEN, dataLink, renderer1);
        RenderingState renderingState2 = asyncRenderer
                .render(Cancellation.UNCANCELABLE_TOKEN, dataLink, renderer2);
        // The initial state should not be null.
        assertNotNull(renderingState1.getAsyncDataState());
        assertNotNull(renderingState2.getAsyncDataState());

        while (executor.executeCurrentlySubmitted() > 0) {
            // Do nothing but execute tasks.
        }

        assertSame(state, renderingState1.getAsyncDataState());
        assertSame(state, renderingState2.getAsyncDataState());
    }

    @Test
    public void testVeryFastOverwrite() {
        TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
        GenericAsyncRendererFactory rendererFactory = new GenericAsyncRendererFactory(executor);
        final AsyncRenderer asyncRenderer = rendererFactory.createRenderer();

        DataRenderer<Object> renderer1 = mockDummyRenderer(true, new CountDownLatch(1));
        final DataRenderer<Object> renderer2 = mockDummyRenderer(true, new CountDownLatch(1));

        final Runnable runRenderer2 = Tasks.runOnceTask(() -> {
            asyncRenderer.render(Cancellation.UNCANCELABLE_TOKEN, null, renderer2);
        }, false);

        CancellationToken cancelToken = new CancellationToken() {
            @Override
            public ListenerRef addCancellationListener(Runnable listener) {
                runRenderer2.run();
                listener.run();
                return UnregisteredListenerRef.INSTANCE;
            }

            @Override
            public boolean isCanceled() {
                runRenderer2.run();
                return true;
            }

            @Override
            public void checkCanceled() {
                runRenderer2.run();
                throw new OperationCanceledException();
            }
        };

        asyncRenderer.render(cancelToken, null, renderer1);
        runRenderer2.run();

        InOrder inOrder = inOrder(renderer2);
        inOrder.verify(renderer2).startRendering(any(CancellationToken.class));
        inOrder.verify(renderer2).finishRendering(any(CancellationToken.class), any(AsyncReport.class));
    }

    private static final class OrderListenerRenderer<DataType>
    implements
            DataRenderer<DataType> {
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
        public boolean startRendering(CancellationToken cancelToken) {
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
        public boolean render(CancellationToken cancelToken, DataType data) {
            return true;
        }

        @Override
        public void finishRendering(CancellationToken cancelToken, AsyncReport report) {
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
            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(dataListener, "dataListener");

            final AtomicInteger step = new AtomicInteger(0);

            executor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
                for (DataType data: datas) {
                    taskCancelToken.checkCanceled();
                    dataListener.onDataArrive(data);
                    step.incrementAndGet();
                }
            }, (boolean canceled, Throwable error) -> {
                dataListener.onDoneReceive(AsyncReport.getReport(error, canceled));
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

    private static interface DataLinkFactory {
        public AsyncDataLink<Object> createLink(TaskExecutorService executor, Object... datas);
    }
}
