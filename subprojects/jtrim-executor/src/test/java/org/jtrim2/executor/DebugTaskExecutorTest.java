package org.jtrim2.executor;

import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DebugTaskExecutorTest {
    private static LogCollector startCollecting() {
        return LogCollector.startCollecting(DebugTaskExecutor.class.getName());
    }

    private static void testExpectLog(TestMethod task) throws Exception {
        try (LogCollector logs = startCollecting()) {
            task.run();
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
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

    @Test
    public void testExecuteTaskWithException() throws Exception {
        final TaskExecutor executor
                = new DebugTaskExecutor(SyncTaskExecutor.getSimpleExecutor());

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
        final TaskExecutor executor
                = new DebugTaskExecutor(SyncTaskExecutor.getSimpleExecutor());

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
        final TaskExecutor executor
                = new DebugTaskExecutor(SyncTaskExecutor.getSimpleExecutor());

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

    private static interface TestMethod {
        public void run() throws Exception;
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
