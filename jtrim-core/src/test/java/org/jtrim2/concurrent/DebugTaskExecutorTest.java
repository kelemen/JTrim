package org.jtrim2.concurrent;

import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.utils.LogCollector;
import org.jtrim2.utils.LogCollectorTest;
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
public class DebugTaskExecutorTest {
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
        return LogCollector.startCollecting(DebugTaskExecutor.class.getName());
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
