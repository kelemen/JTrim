package org.jtrim2.executor;

import java.util.concurrent.Executor;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.testutils.executor.MockCleanup;
import org.jtrim2.testutils.executor.MockFunction;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class ExecutorAsTaskExecutorTest {
    private static final int DEFAULT_TEST_COUNT = 5;

    private static Executor syncExecutor() {
        return Runnable::run;
    }

    @Test
    public void testRunnableExecute() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(syncExecutor(), false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            Runnable task = mock(Runnable.class);
            executor.execute(task);
            verify(task).run();
        }
    }

    @Test
    public void testSimpleExecute() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(syncExecutor(), false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            CancelableTask task = mock(CancelableTask.class);
            MockCleanup cleanup = mock(MockCleanup.class);

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));

            InOrder inOrder = inOrder(task, cleanup);
            inOrder.verify(task).execute(any(CancellationToken.class));
            inOrder.verify(cleanup).cleanup(null, null);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testExecuteFunction() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(syncExecutor(), false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            Object result = "Test-result-3543543-" + i;
            MockFunction<Object> function = MockFunction.mock(result);
            MockCleanup cleanup = mock(MockCleanup.class);

            executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, MockFunction.toFunction(function))
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));

            InOrder inOrder = inOrder(function, cleanup);
            inOrder.verify(function).execute(false);
            inOrder.verify(cleanup).cleanup(same(result), isNull(Throwable.class));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testExecutePreCanceled() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(syncExecutor(), false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            Object result = "Test-result-6465475-" + i;
            MockFunction<Object> function = MockFunction.mock(result);
            MockCleanup cleanup = mock(MockCleanup.class);

            executor.executeFunction(Cancellation.CANCELED_TOKEN, MockFunction.toFunction(function))
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));

            verifyZeroInteractions(function);
            verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
            verifyNoMoreInteractions(cleanup);
        }
    }

    @Test
    public void testExecuteTaskCancels() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(syncExecutor(), false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            CancelableTask task = mock(CancelableTask.class);
            doThrow(OperationCanceledException.class)
                    .when(task)
                    .execute(any(CancellationToken.class));

            MockCleanup cleanup = mock(MockCleanup.class);

            executor.execute(Cancellation.CANCELED_TOKEN, task)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));

            verifyZeroInteractions(task);
            verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
        }
    }

    @Test
    public void testExecuteTaskThrowsException() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(syncExecutor(), false);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            CancelableTask task = mock(CancelableTask.class);
            doThrow(OperationCanceledException.class)
                    .when(task)
                    .execute(any(CancellationToken.class));

            MockCleanup cleanup = mock(MockCleanup.class);

            Exception taskException = new Exception();
            doThrow(taskException)
                    .when(task)
                    .execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));

            InOrder inOrder = inOrder(task, cleanup);
            inOrder.verify(task).execute(any(CancellationToken.class));
            inOrder.verify(cleanup).cleanup(isNull(), same(taskException));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testThreadInterrupts() throws Exception {
        ExecutorAsTaskExecutor executor = new ExecutorAsTaskExecutor(syncExecutor(), true);

        for (int i = 0; i < DEFAULT_TEST_COUNT; i++) {
            Thread.interrupted(); // clear the interrupted status

            final CancellationSource cancelSource = Cancellation.createCancellationSource();

            CancelableTask task = mock(CancelableTask.class);
            MockCleanup cleanup = mock(MockCleanup.class);

            doAnswer((InvocationOnMock invocation) -> {
                cancelSource.getController().cancel();
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                return null;
            }).when(task).execute(any(CancellationToken.class));

            executor.execute(cancelSource.getToken(), task)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));

            InOrder inOrder = inOrder(task, cleanup);
            inOrder.verify(task).execute(any(CancellationToken.class));
            inOrder.verify(cleanup).cleanup(isNull(), isA(InterruptedException.class));
            inOrder.verifyNoMoreInteractions();
        }
    }
}
