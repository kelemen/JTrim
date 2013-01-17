package org.jtrim.swing.concurrent.async;

import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.jtrim.access.AccessManager;
import org.jtrim.access.AccessRequest;
import org.jtrim.access.AccessResult;
import org.jtrim.access.AccessToken;
import org.jtrim.access.HierarchicalAccessManager;
import org.jtrim.access.HierarchicalRight;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.Tasks;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.concurrent.async.AsyncDataController;
import org.jtrim.concurrent.async.AsyncDataLink;
import org.jtrim.concurrent.async.AsyncDataListener;
import org.jtrim.concurrent.async.AsyncDataQuery;
import org.jtrim.concurrent.async.AsyncReport;
import org.jtrim.concurrent.async.SimpleDataController;
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
public class BackgroundDataProviderTest {
    public BackgroundDataProviderTest() {
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

    private <ResultType> void checkValidResults(ResultType[] expectedResults, ResultType[] actualResults) {
        int expectedIndex = 0;
        for (ResultType actual: actualResults) {
            int foundIndex = findInArray(actual, actualResults, expectedIndex);
            if (foundIndex < 0) {
                fail("Unexpected results: " + Arrays.toString(actualResults)
                        + " (expected = " + Arrays.toString(expectedResults) + ")");
            }
        }
    }

    private <ResultType> void checkValidCompleteResults(ResultType[] expectedResults, ResultType[] actualResults) {
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
    @Test
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
    @Test
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

    private CollectListener startAndWaitQuery(
            CancellationToken cancelToken,
            String[] datas) {
        final AccessRequest<String, HierarchicalRight> request = createWriteRequest("RIGHT");

        final AccessManager<String, HierarchicalRight> manager
                = new HierarchicalAccessManager<>(SyncTaskExecutor.getSimpleExecutor());
        BackgroundDataProvider<String, HierarchicalRight> dataProvider
                = new BackgroundDataProvider<>(manager);

        TestQuery wrappedQuery = new TestQuery(datas);
        AsyncDataQuery<Void, String> query = dataProvider.createQuery(request, wrappedQuery);
        assertNotNull(query);

        CollectListener resultCollector = new CollectListener(Tasks.noOpTask());
        query.createDataLink(null).getData(cancelToken, resultCollector);
        resultCollector.waitCompletion(5, TimeUnit.SECONDS);

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
        private final Runnable onDoneCheck;

        private final Queue<String> results;
        private final AtomicReference<AsyncReport> reportRef;
        private final WaitableSignal doneSignal;
        private final AtomicReference<String> miscErrorRef;

        public CollectListener(Runnable onDoneCheck) {
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

        public void waitCompletion(long timeout, TimeUnit unit) {
            if (!doneSignal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, timeout, unit)) {
                throw new OperationCanceledException("timeout");
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
            if (!SwingUtilities.isEventDispatchThread()) {
                setMiscError("onDataArrive was not called from the EDT.");
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
                if (!SwingUtilities.isEventDispatchThread()) {
                    setMiscError("onDoneReceive was not called from the EDT.");
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

            executor.execute(cancelToken, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    for (String data: datas) {
                        cancelToken.checkCanceled();
                        dataListener.onDataArrive(data);
                    }
                }
            }, new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) {
                    dataListener.onDoneReceive(AsyncReport.getReport(error, canceled));
                }
            });

            return new SimpleDataController();
        }
    }
}
