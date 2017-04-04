package org.jtrim.concurrent;

import java.util.logging.Level;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.utils.LogCollector;
import org.jtrim.utils.LogCollectorTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class DebugTaskExecutorServiceTest {
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

    private static LogCollector startCollecting() {
        return LogCollector.startCollecting(DebugTaskExecutorService.class.getName());
    }

    private static void testExpectLog(TestMethod task) throws Exception {
        try (LogCollector logs = startCollecting()) {
            task.run();
            LogCollectorTest.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }
    }

    private static void testNotExpectLog(TestMethod task) throws Exception {
        try (LogCollector logs = startCollecting()) {
            task.run();
            assertEquals(0, logs.getNumberOfLogs());
        }
    }

    private static CancelableTask mockTask(Throwable exception) throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        doThrow(exception).when(task).execute(any(CancellationToken.class));
        return task;
    }

    private static CancelableFunction<?> mockFunction(Throwable exception) throws Exception {
        CancelableFunction<?> task = mock(CancelableFunction.class);
        stub(task.execute(any(CancellationToken.class))).toThrow(exception);
        return task;
    }

    @Test
    public void testExecuteTaskWithException() throws Exception {
        final TaskExecutorService executor
                = new DebugTaskExecutorService(SyncTaskExecutor.getDefaultInstance());

        testExpectLog(() -> {
            CancelableTask task = mockTask(new TestException());
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
            verify(task).execute(any(CancellationToken.class));
        });

        testExpectLog(() -> {
            CancelableTask task = mockTask(new TestException());
            CleanupTask cleanup = mock(CleanupTask.class);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testExecuteTaskWithCanceledException() throws Exception {
        final TaskExecutorService executor
                = new DebugTaskExecutorService(SyncTaskExecutor.getDefaultInstance());

        testNotExpectLog(() -> {
            CancelableTask task = mockTask(new OperationCanceledException());
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
            verify(task).execute(any(CancellationToken.class));
        });

        testNotExpectLog(() -> {
            CancelableTask task = mockTask(new OperationCanceledException());
            CleanupTask cleanup = mock(CleanupTask.class);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testExecuteTaskWithoutException() throws Exception {
        final TaskExecutorService executor
                = new DebugTaskExecutorService(SyncTaskExecutor.getDefaultInstance());

        testNotExpectLog(() -> {
            CancelableTask task = mock(CancelableTask.class);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
            verify(task).execute(any(CancellationToken.class));
        });

        testNotExpectLog(() -> {
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testSubmitTaskWithException() throws Exception {
        final TaskExecutorService executor
                = new DebugTaskExecutorService(SyncTaskExecutor.getDefaultInstance());

        testExpectLog(() -> {
            CancelableTask task = mockTask(new TestException());
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, null);
            verify(task).execute(any(CancellationToken.class));
        });

        testExpectLog(() -> {
            CancelableTask task = mockTask(new TestException());
            CleanupTask cleanup = mock(CleanupTask.class);
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testSubmitTaskWithCanceledException() throws Exception {
        final TaskExecutorService executor
                = new DebugTaskExecutorService(SyncTaskExecutor.getDefaultInstance());

        testNotExpectLog(() -> {
            CancelableTask task = mockTask(new OperationCanceledException());
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, null);
            verify(task).execute(any(CancellationToken.class));
        });

        testNotExpectLog(() -> {
            CancelableTask task = mockTask(new OperationCanceledException());
            CleanupTask cleanup = mock(CleanupTask.class);
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testSubmitTaskWithoutException() throws Exception {
        final TaskExecutorService executor
                = new DebugTaskExecutorService(SyncTaskExecutor.getDefaultInstance());

        testNotExpectLog(() -> {
            CancelableTask task = mock(CancelableTask.class);
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, null);
            verify(task).execute(any(CancellationToken.class));
        });

        testNotExpectLog(() -> {
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testSubmitFunctionWithException() throws Exception {
        final TaskExecutorService executor
                = new DebugTaskExecutorService(SyncTaskExecutor.getDefaultInstance());

        testExpectLog(() -> {
            CancelableFunction<?> task = mockFunction(new TestException());
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, null);
            verify(task).execute(any(CancellationToken.class));
        });

        testExpectLog(() -> {
            CancelableFunction<?> task = mockFunction(new TestException());
            CleanupTask cleanup = mock(CleanupTask.class);
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testSubmitFunctionWithCanceledException() throws Exception {
        final TaskExecutorService executor
                = new DebugTaskExecutorService(SyncTaskExecutor.getDefaultInstance());

        testNotExpectLog(() -> {
            CancelableFunction<?> task = mockFunction(new OperationCanceledException());
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, null);
            verify(task).execute(any(CancellationToken.class));
        });

        testNotExpectLog(() -> {
            CancelableFunction<?> task = mockFunction(new OperationCanceledException());
            CleanupTask cleanup = mock(CleanupTask.class);
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testSubmitFunctionWithoutException() throws Exception {
        final TaskExecutorService executor
                = new DebugTaskExecutorService(SyncTaskExecutor.getDefaultInstance());

        testNotExpectLog(() -> {
            CancelableFunction<?> task = mock(CancelableFunction.class);
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, null);
            verify(task).execute(any(CancellationToken.class));
        });

        testNotExpectLog(() -> {
            CancelableFunction<?> task = mock(CancelableFunction.class);
            CleanupTask cleanup = mock(CleanupTask.class);
            executor.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    private static interface TestMethod {
        public void run() throws Exception;
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
