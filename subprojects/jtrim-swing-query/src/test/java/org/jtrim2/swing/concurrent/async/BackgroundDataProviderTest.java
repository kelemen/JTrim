package org.jtrim2.swing.concurrent.async;

import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.access.AccessManager;
import org.jtrim2.access.AccessRequest;
import org.jtrim2.access.AccessResult;
import org.jtrim2.access.AccessToken;
import org.jtrim2.access.HierarchicalAccessManager;
import org.jtrim2.access.HierarchicalRight;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.concurrent.query.AsyncDataController;
import org.jtrim2.concurrent.query.AsyncDataLink;
import org.jtrim2.concurrent.query.AsyncDataListener;
import org.jtrim2.concurrent.query.AsyncDataQuery;
import org.jtrim2.concurrent.query.AsyncDataState;
import org.jtrim2.concurrent.query.AsyncReport;
import org.jtrim2.concurrent.query.SimpleDataController;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.testutils.concurrent.AbstractUiExecutorProvider;
import org.jtrim2.testutils.concurrent.ManualUiExecutorProvider;
import org.jtrim2.testutils.concurrent.SyncUiExecutorProvider;
import org.jtrim2.ui.concurrent.UiExecutorProvider;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class BackgroundDataProviderTest {
    private static AccessRequest<String, HierarchicalRight> createWriteRequest(
            Object... rights) {
        return AccessRequest.getWriteRequest("", HierarchicalRight.create(rights));
    }

    private <T> int findInArray(T toFind, T[] array, int startOffset) {
        for (int i = startOffset; i < array.length; i++) {
            if (array[i] == toFind) {
                return i;
            }
        }
        return -1;
    }

    private <ResultType> void checkValidResults(
            ResultType[] expectedResults,
            ResultType[] actualResults) {
        int expectedIndex = 0;
        for (ResultType actual: actualResults) {
            int foundIndex = findInArray(actual, actualResults, expectedIndex);
            if (foundIndex < 0) {
                fail("Unexpected results: " + Arrays.toString(actualResults)
                        + " (expected = " + Arrays.toString(expectedResults) + ")");
            }
        }
    }

    private <ResultType> void checkValidCompleteResults(
            ResultType[] expectedResults,
            ResultType[] actualResults) {
        if (expectedResults.length == 0) {
            assertEquals("Expected no results.", 0, actualResults.length);
            return;
        }
        if (actualResults.length == 0) {
            fail("Need at least one result.");
        }

        assertEquals(
                "The final result must match the final expected result.",
                expectedResults[expectedResults.length - 1],
                actualResults[actualResults.length - 1]);

        checkValidResults(expectedResults, actualResults);
    }

    /**
     * Test for the simplest case when all thing goes well and every data gets
     * transfered without error or cancellation.
     */
    @Test(timeout = 20000)
    public void testNormalCompletion() {
        String[] datas = {"DATA1", "DATA2", "DATA3"};
        CollectListener resultCollector = startAndWaitQuery(
                Cancellation.UNCANCELABLE_TOKEN, datas);

        AsyncReport report = resultCollector.getReport();
        assertNotNull(report);
        assertTrue(report.isSuccess());

        String[] results = resultCollector.getResults();
        assertTrue("Missing results.", results.length > 0);
        checkValidCompleteResults(datas, results);
    }

    /**
     * Test when requesting the data has been canceled prior transferring the
     * data.
     */
    @Test(timeout = 20000)
    public void testCanceledCompletion() {
        String[] datas = {"DATA1", "DATA2", "DATA3"};
        CollectListener resultCollector = startAndWaitQuery(
                Cancellation.CANCELED_TOKEN, datas);

        AsyncReport report = resultCollector.getReport();
        assertNotNull("Missing data transfer report.", report);
        assertNull("Unexpected exception.", report.getException());
        assertTrue("Data transfer must be canceled.", report.isCanceled());

        String[] results = resultCollector.getResults();
        assertEquals("Expecting no results.", 0, results.length);
    }

    private static BackgroundDataProvider<String, HierarchicalRight> createSync(
            AccessManager<String, HierarchicalRight> accessManager) {
        return create(accessManager, new SyncUiExecutorProvider());
    }

    private static BackgroundDataProvider<String, HierarchicalRight> create(
            AccessManager<String, HierarchicalRight> accessManager,
            UiExecutorProvider uiExecutorProvider) {
        return new BackgroundDataProvider<>(accessManager, uiExecutorProvider);
    }

    @Test(timeout = 20000)
    public void testAccessDenied() {
        String[] datas = {"DATA1", "DATA2"};
        final AccessRequest<String, HierarchicalRight> request = createWriteRequest("RIGHT");

        final AccessManager<String, HierarchicalRight> manager
                = new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());

        ManualUiExecutorProvider ui = new ManualUiExecutorProvider(true);
        BackgroundDataProvider<String, HierarchicalRight> dataProvider = create(manager, ui);

        AccessResult<String> access = manager.tryGetAccess(request);
        assertTrue(access.isAvailable());

        TestQuery wrappedQuery = new TestQuery(datas);
        AsyncDataQuery<Void, String> query = dataProvider.createQuery(request, wrappedQuery);

        CollectListener resultCollector = new CollectListener(ui, Tasks.noOpTask());
        AsyncDataController controller = query.createDataLink(null).getData(
                Cancellation.UNCANCELABLE_TOKEN, resultCollector);
        controller.controlData(new Object());
        AsyncDataState dataState = controller.getDataState();
        assertEquals(dataState.getProgress(), dataState.getProgress(), 0.00000001);

        ui.executeAll();
        resultCollector.assertCompleted();

        assertNull(resultCollector.getMiscError());

        AsyncReport report = resultCollector.getReport();
        assertTrue(report.isCanceled());
        assertNull(report.getException());

        assertEquals(0, resultCollector.getResults().length);
    }

    @Test(timeout = 20000)
    public void testBuggyWrappedQuery() {
        final AccessManager<String, HierarchicalRight> manager
                = new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
        BackgroundDataProvider<String, HierarchicalRight> dataProvider = createSync(manager);

        @SuppressWarnings("unchecked")
        AsyncDataQuery<Void, String> query = mock(AsyncDataQuery.class);
        stub(query.createDataLink(any(Void.class))).toThrow(new TestException());

        final AccessRequest<String, HierarchicalRight> request = createWriteRequest("RIGHT");
        try {
            dataProvider.createQuery(request, query).createDataLink(null);
            fail("Expected exception");
        } catch (TestException ex) {
        }

        assertTrue(manager.isAvailable(request.getReadRights(), request.getWriteRights()));
    }

    @Test(timeout = 20000)
    @SuppressWarnings("unchecked")
    public void testBuggyWrappedLink() {
        final AccessManager<String, HierarchicalRight> manager
                = new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
        BackgroundDataProvider<String, HierarchicalRight> dataProvider = createSync(manager);

        AsyncDataLink<String> link = mock(AsyncDataLink.class);
        stub(link.getData(
                any(CancellationToken.class),
                any(AsyncDataListener.class))).toThrow(new TestException());

        AsyncDataQuery<Void, String> query = mock(AsyncDataQuery.class);
        stub(query.createDataLink(any(Void.class))).toReturn(link);

        final AccessRequest<String, HierarchicalRight> request = createWriteRequest("RIGHT");
        try {
            AsyncDataListener<String> listener = mock(AsyncDataListener.class);
            dataProvider.createQuery(request, query).createDataLink(null).getData(
                    Cancellation.UNCANCELABLE_TOKEN, listener);
            fail("Expected exception");
        } catch (TestException ex) {
        }

        assertTrue(manager.isAvailable(request.getReadRights(), request.getWriteRights()));
    }

    private CollectListener startAndWaitQuery(
            CancellationToken cancelToken,
            String[] datas) {
        final AccessRequest<String, HierarchicalRight> request = createWriteRequest("RIGHT");

        ManualUiExecutorProvider ui = new ManualUiExecutorProvider(true);
        final AccessManager<String, HierarchicalRight> manager
                = new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
        BackgroundDataProvider<String, HierarchicalRight> dataProvider = create(manager, ui);

        TestQuery wrappedQuery = new TestQuery(datas);
        AsyncDataQuery<Void, String> query = dataProvider.createQuery(request, wrappedQuery);
        assertNotNull(query);

        CollectListener resultCollector = new CollectListener(ui, Tasks.noOpTask());
        AsyncDataController controller = query.createDataLink(null).getData(
                cancelToken, resultCollector);
        controller.controlData(new Object());
        AsyncDataState dataState = controller.getDataState();
        if (dataState != null) {
            double progress = dataState.getProgress();
            assertTrue(progress >= 0.0 && progress <= 1.0);
        }

        ui.executeAll();
        resultCollector.assertCompleted();

        assertNull(resultCollector.getMiscError());

        AccessResult<String> waitToken = manager.getScheduledAccess(request);
        Collection<AccessToken<String>> blockingTokens = waitToken.getBlockingTokens();
        for (AccessToken<?> token: blockingTokens) {
            token.tryAwaitRelease(cancelToken, 5, TimeUnit.SECONDS);
        }
        waitToken.release();

        assertTrue("Request must be available after completion.",
                manager.isAvailable(request.getReadRights(), request.getWriteRights()));

        return resultCollector;
    }

    private static class CollectListener implements AsyncDataListener<String> {
        private final AbstractUiExecutorProvider ui;
        private final Runnable onDoneCheck;

        private final Queue<String> results;
        private final AtomicReference<AsyncReport> reportRef;
        private final WaitableSignal doneSignal;
        private final AtomicReference<String> miscErrorRef;

        public CollectListener(AbstractUiExecutorProvider ui, Runnable onDoneCheck) {
            this.ui = ui;
            this.onDoneCheck = onDoneCheck;
            this.results = new ConcurrentLinkedQueue<>();
            this.reportRef = new AtomicReference<>(null);
            this.doneSignal = new WaitableSignal();
            this.miscErrorRef = new AtomicReference<>(null);
        }

        private void setMiscError(String error) {
            miscErrorRef.compareAndSet(null, error);
        }

        public String getMiscError() {
            return miscErrorRef.get();
        }

        public void assertCompleted() {
            if (!doneSignal.isSignaled()) {
                throw new AssertionError("Expected completion");
            }
        }

        public void waitCompletion(long timeout, TimeUnit unit) {
            if (!doneSignal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, timeout, unit)) {
                throw new AssertionError("timeout");
            }
        }

        public String[] getResults() {
            return results.toArray(new String[0]);
        }

        public AsyncReport getReport() {
            return reportRef.get();
        }

        @Override
        public void onDataArrive(String data) {
            if (!ui.isInContext()) {
                setMiscError("onDataArrive was not called from the UI thread.");
            }

            results.add(data);
        }

        @Override
        public void onDoneReceive(AsyncReport report) {
            try {
                try {
                    onDoneCheck.run();
                } catch (Throwable checkError) {
                    setMiscError(checkError.getMessage());
                }
                if (!ui.isInContext()) {
                    setMiscError("onDoneReceive was not called from the UI thread.");
                }
                if (!reportRef.compareAndSet(null, report)) {
                    setMiscError("Report has been sent multiple times.");
                }
            } finally {
                doneSignal.signal();
            }
        }
    }

    private static class TestQuery implements AsyncDataQuery<Void, String> {
        private final TaskExecutor executor;
        private final String[] datas;

        public TestQuery(String... datas) {
            this(SyncTaskExecutor.getSimpleExecutor(), datas);
        }

        public TestQuery(TaskExecutor executor, String... datas) {
            this.executor = executor;
            this.datas = datas.clone();
        }

        @Override
        public AsyncDataLink<String> createDataLink(Void arg) {
            return new TestLink(executor, datas);
        }
    }

    private static class TestLink implements AsyncDataLink<String> {
        private final TaskExecutor executor;
        private final String[] datas;

        public TestLink(String... datas) {
            this(SyncTaskExecutor.getSimpleExecutor(), datas);
        }

        public TestLink(TaskExecutor executor, String... datas) {
            this.executor = executor;
            this.datas = datas.clone();
        }

        @Override
        public AsyncDataController getData(
                CancellationToken cancelToken,
                final AsyncDataListener<? super String> dataListener) {

            executor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
                for (String data: datas) {
                    taskCancelToken.checkCanceled();
                    dataListener.onDataArrive(data);
                }
            }, (boolean canceled, Throwable error) -> {
                dataListener.onDoneReceive(AsyncReport.getReport(error, canceled));
            });

            return new SimpleDataController();
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -6949622194960124944L;
    }
}
