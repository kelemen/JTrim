package org.jtrim2.executor;

import java.util.concurrent.Executor;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.utils.LogCollector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class ExecutorAsTaskExecutorTest {
    private static final int DEFAULT_TEST_COUNT = 5;

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

    @Test
    public void testSimpleExecute() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(SyncExecutor.INSTANCE, false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);

            InOrder inOrder = inOrder(task, cleanup);
            inOrder.verify(task).execute(any(CancellationToken.class));
            inOrder.verify(cleanup).cleanup(false, null);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testExecuteWithoutCleanup() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(SyncExecutor.INSTANCE, false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            CancelableTask task = mock(CancelableTask.class);

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);

            verify(task).execute(any(CancellationToken.class));
        }
    }

    @Test
    public void testExecutePreCanceled() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(SyncExecutor.INSTANCE, false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);

            executor.execute(Cancellation.CANCELED_TOKEN, task, cleanup);

            verifyZeroInteractions(task);
            verify(cleanup).cleanup(true, null);
            verifyNoMoreInteractions(cleanup);
        }
    }

    @Test
    public void testExecuteTaskCancels() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(SyncExecutor.INSTANCE, false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);

            doThrow(OperationCanceledException.class)
                    .when(task)
                    .execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);

            InOrder inOrder = inOrder(task, cleanup);
            inOrder.verify(task).execute(any(CancellationToken.class));
            inOrder.verify(cleanup).cleanup(eq(true), isA(OperationCanceledException.class));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testExecuteTaskThrowsException() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(SyncExecutor.INSTANCE, false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);

            Exception taskException = new Exception();
            doThrow(taskException)
                    .when(task)
                    .execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);

            InOrder inOrder = inOrder(task, cleanup);
            inOrder.verify(task).execute(any(CancellationToken.class));
            inOrder.verify(cleanup).cleanup(eq(false), same(taskException));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testExecuteCleanupThrowsException() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(SyncExecutor.INSTANCE, false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);

            doThrow(new TestException())
                    .when(cleanup)
                    .cleanup(anyBoolean(), any(Throwable.class));

            try (LogCollector logs = LogTests.startCollecting()) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
                LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
            }

            InOrder inOrder = inOrder(task, cleanup);
            inOrder.verify(task).execute(any(CancellationToken.class));
            inOrder.verify(cleanup).cleanup(false, null);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testThreadInterrupts() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(SyncExecutor.INSTANCE, true);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            Thread.interrupted(); // clear the interrupted status

            final CancellationSource cancelSource = Cancellation.createCancellationSource();

            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);

            doAnswer((InvocationOnMock invocation) -> {
                cancelSource.getController().cancel();
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                return null;
            }).when(task).execute(any(CancellationToken.class));

            executor.execute(cancelSource.getToken(), task, cleanup);

            InOrder inOrder = inOrder(task, cleanup);
            inOrder.verify(task).execute(any(CancellationToken.class));
            inOrder.verify(cleanup).cleanup(eq(false), isA(InterruptedException.class));
            inOrder.verifyNoMoreInteractions();
        }
    }

    private enum SyncExecutor implements Executor {
        INSTANCE;

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
