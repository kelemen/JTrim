package org.jtrim.concurrent;

import java.util.concurrent.Executor;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class ExecutorAsTaskExecutorTest {
    private static final int DEFAULT_TEST_COUNT = 5;

    public ExecutorAsTaskExecutorTest() {
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

            Exception cleanupExecption = new Exception();
            doThrow(cleanupExecption)
                    .when(cleanup)
                    .cleanup(anyBoolean(), any(Throwable.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);

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

            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws InterruptedException {
                    cancelSource.getController().cancel();
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    return null;
                }
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
}
