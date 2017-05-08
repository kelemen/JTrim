package org.jtrim2.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationController;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.TaskState;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MultiPhaseTaskTest {
    private static TaskExecutor delegate(final TaskExecutor executor) {
        return new DelegatedTaskExecutor(executor);
    }

    private static TaskExecutorService delegate(final TaskExecutorService executor) {
        return new DelegatedTaskExecutorService(executor);
    }

    private static UpdateTaskExecutor delegate(final UpdateTaskExecutor executor) {
        return new DelegatedUpdateTaskExecutor(executor);
    }

    @Test(timeout = 5000)
    public void testFinishWithNullNotification() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        Object result = new Object();
        assertTrue(task.finishTask(result, null, false));
        assertTrue(task.isDone());
        assertSame(result, task.getFuture().get());
    }

    /**
     * Test of executeSubTask method, of class MultiPhaseTask.
     */
    @Test
    public void testExecuteSubTask_UpdateTaskExecutor_Runnable() {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        UpdateTaskExecutor executor = spy(delegate(new GenericUpdateTaskExecutor(
                SyncTaskExecutor.getSimpleExecutor())));
        Runnable subTask = mock(Runnable.class);

        task.executeSubTask(executor, subTask);

        verify(executor).execute(any(Runnable.class));
        verify(subTask).run();
        verifyNoMoreInteractions(executor, subTask);
    }

    /**
     * Test of executeSubTask method, of class MultiPhaseTask.
     */
    @Test
    public void testExecuteSubTask_UpdateTaskExecutor_RunnableAfterFinish() {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        UpdateTaskExecutor executor = spy(delegate(new GenericUpdateTaskExecutor(
                SyncTaskExecutor.getSimpleExecutor())));
        Runnable subTask = mock(Runnable.class);

        task.finishTask(null, null, false);
        task.executeSubTask(executor, subTask);

        verifyZeroInteractions(executor, subTask);
    }

    private static Answer<Void> cancelIsEffective(
            final CancellationController controller,
            final Runnable cancelTestFailed) {
        assert controller != null;
        assert cancelTestFailed != null;

        return (InvocationOnMock invocation) -> {
            CancellationToken token = (CancellationToken)invocation.getArguments()[0];

            if (token.isCanceled()) cancelTestFailed.run();
            controller.cancel();
            if (!token.isCanceled()) cancelTestFailed.run();
            return null;
        };
    }

    @Test
    public void testExecuteSubTask_4args() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        TaskExecutor executor = spy(delegate(SyncTaskExecutor.getSimpleExecutor()));
        CancelableTask subTask = mock(CancelableTask.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        Runnable cancelTestFailed = mock(Runnable.class);

        doAnswer(cancelIsEffective(cancelSource.getController(), cancelTestFailed))
                .when(subTask)
                .execute(any(CancellationToken.class));

        task.executeSubTask(executor, cancelSource.getToken(), subTask, cleanupTask);

        verifyZeroInteractions(cancelTestFailed);

        verify(executor).execute(
                any(CancellationToken.class),
                any(CancelableTask.class),
                any(CleanupTask.class));
        verify(subTask).execute(any(CancellationToken.class));
        verify(cleanupTask).cleanup(eq(false), isNull(Throwable.class));
        verifyNoMoreInteractions(executor, subTask, cleanupTask);
    }

    @Test
    public void testExecuteSubTask_4argsWithoutCleanup() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        TaskExecutor executor = spy(delegate(SyncTaskExecutor.getSimpleExecutor()));
        CancelableTask subTask = mock(CancelableTask.class);

        Runnable cancelTestFailed = mock(Runnable.class);

        doAnswer(cancelIsEffective(cancelSource.getController(), cancelTestFailed))
                .when(subTask)
                .execute(any(CancellationToken.class));

        task.executeSubTask(executor, cancelSource.getToken(), subTask, null);

        verifyZeroInteractions(cancelTestFailed);

        verify(executor).execute(
                any(CancellationToken.class),
                any(CancelableTask.class),
                any(CleanupTask.class));
        verify(subTask).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(executor, subTask);
    }

    @Test
    public void testExecuteSubTask_4argsAfterFinish() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        TaskExecutor executor = spy(delegate(SyncTaskExecutor.getSimpleExecutor()));
        CancelableTask subTask = mock(CancelableTask.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        task.finishTask(null, null, false);
        task.executeSubTask(executor, Cancellation.UNCANCELABLE_TOKEN, subTask, cleanupTask);

        verify(executor).execute(
                any(CancellationToken.class),
                any(CancelableTask.class),
                any(CleanupTask.class));
        verify(cleanupTask).cleanup(true, null);
        verifyZeroInteractions(subTask);
    }

    @Test
    public void testSubmitSubTask_4args_1() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        TaskExecutorService executor = spy(delegate(new SyncTaskExecutor()));
        CancelableTask subTask = mock(CancelableTask.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);
        final Runnable cancelTestFailed = mock(Runnable.class);

        doAnswer(cancelIsEffective(cancelSource.getController(), cancelTestFailed))
                .when(subTask)
                .execute(any(CancellationToken.class));

        TaskFuture<?> future = task.submitSubTask(executor, cancelSource.getToken(), subTask, cleanupTask);
        assertEquals(TaskState.DONE_COMPLETED, future.getTaskState());
        assertNull(future.tryGetResult());

        verifyZeroInteractions(cancelTestFailed);

        verify(executor).submit(any(CancellationToken.class), any(CancelableTask.class), any(CleanupTask.class));
        verify(subTask).execute(any(CancellationToken.class));
        verify(cleanupTask).cleanup(eq(false), isNull(Throwable.class));
        verifyNoMoreInteractions(executor, subTask, cleanupTask);
    }

    @Test
    public void testSubmitSubTask_4args_1WithoutCleanup() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        TaskExecutorService executor = spy(delegate(new SyncTaskExecutor()));
        CancelableTask subTask = mock(CancelableTask.class);
        final Runnable cancelTestFailed = mock(Runnable.class);

        doAnswer(cancelIsEffective(cancelSource.getController(), cancelTestFailed))
                .when(subTask)
                .execute(any(CancellationToken.class));

        TaskFuture<?> future = task.submitSubTask(executor, cancelSource.getToken(), subTask, null);
        assertEquals(TaskState.DONE_COMPLETED, future.getTaskState());
        assertNull(future.tryGetResult());

        verifyZeroInteractions(cancelTestFailed);

        verify(executor).submit(any(CancellationToken.class), any(CancelableTask.class), any(CleanupTask.class));
        verify(subTask).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(executor, subTask);
    }

    @Test
    public void testSubmitSubTask_4args_1AfterFinish() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        TaskExecutorService executor = new SyncTaskExecutor();
        CancelableTask subTask = mock(CancelableTask.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        task.finishTask(null, null, false);
        TaskFuture<?> future = task.submitSubTask(executor, Cancellation.UNCANCELABLE_TOKEN, subTask, cleanupTask);
        assertEquals(TaskState.DONE_CANCELED, future.getTaskState());

        verify(cleanupTask).cleanup(true, null);
        verifyZeroInteractions(subTask);
    }

    @Test
    public void testSubmitSubTask_4args_2() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        TaskExecutorService executor = spy(delegate(new SyncTaskExecutor()));
        CancelableFunction<?> subTask = mock(CancelableFunction.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);
        final Runnable cancelTestFailed = mock(Runnable.class);

        final Object result = new Object();
        stub(subTask.execute(any(CancellationToken.class))).toAnswer((InvocationOnMock invocation) -> {
            cancelIsEffective(cancelSource.getController(), cancelTestFailed).answer(invocation);
            return result;
        });

        TaskFuture<?> future = task.submitSubTask(executor, cancelSource.getToken(), subTask, cleanupTask);
        assertEquals(TaskState.DONE_COMPLETED, future.getTaskState());
        assertSame(result, future.tryGetResult());

        verifyZeroInteractions(cancelTestFailed);

        verify(executor).submit(
                any(CancellationToken.class),
                (CancelableFunction<?>)any(CancelableFunction.class),
                any(CleanupTask.class));
        verify(subTask).execute(any(CancellationToken.class));
        verify(cleanupTask).cleanup(eq(false), isNull(Throwable.class));
        verifyNoMoreInteractions(executor, subTask, cleanupTask);
    }

    @Test
    public void testSubmitSubTask_4args_2WithoutCleanup() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        TaskExecutorService executor = spy(delegate(new SyncTaskExecutor()));
        CancelableFunction<?> subTask = mock(CancelableFunction.class);
        final Runnable cancelTestFailed = mock(Runnable.class);

        final Object result = new Object();
        stub(subTask.execute(any(CancellationToken.class))).toAnswer((InvocationOnMock invocation) -> {
            cancelIsEffective(cancelSource.getController(), cancelTestFailed).answer(invocation);
            return result;
        });

        TaskFuture<?> future = task.submitSubTask(executor, cancelSource.getToken(), subTask, null);
        assertEquals(TaskState.DONE_COMPLETED, future.getTaskState());
        assertSame(result, future.tryGetResult());

        verifyZeroInteractions(cancelTestFailed);

        verify(executor).submit(
                any(CancellationToken.class),
                (CancelableFunction<?>)any(CancelableFunction.class),
                any(CleanupTask.class));
        verify(subTask).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(executor, subTask);
    }

    @Test
    public void testSubmitSubTask_4args_2AfterFinish() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        TaskExecutorService executor = new SyncTaskExecutor();
        CancelableFunction<?> subTask = mock(CancelableFunction.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        task.finishTask(null, null, false);
        TaskFuture<?> future = task.submitSubTask(executor,
                Cancellation.UNCANCELABLE_TOKEN, subTask, cleanupTask);
        assertEquals(TaskState.DONE_CANCELED, future.getTaskState());

        verify(cleanupTask).cleanup(true, null);
        verifyZeroInteractions(subTask);
    }

    /**
     * Test of executeSubTask method, of class MultiPhaseTask.
     */
    @Test
    public void testExecuteSubTask_Runnable() {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        Runnable subTask = mock(Runnable.class);

        task.executeSubTask(subTask);

        verify(subTask).run();
        verifyNoMoreInteractions(subTask);
    }

    @Test(timeout = 5000)
    public void testExecuteSubTask_RunnableWithException() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        Runnable subTask = mock(Runnable.class);

        RuntimeException expected = new RuntimeException();
        doThrow(expected).when(subTask).run();

        task.executeSubTask(subTask);

        assertTrue(task.getFuture().isDone());
        try {
            task.getFuture().get();
            fail("Task should have thrown an exception.");
        } catch (ExecutionException ex) {
            assertSame(expected, ex.getCause());
        }

        verify(subTask).run();
        verifyNoMoreInteractions(subTask);
    }

    @Test
    public void testExecuteSubTask_RunnableAfterFinish() {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        Runnable subTask = mock(Runnable.class);

        task.finishTask(null, null, false);
        task.executeSubTask(subTask);

        verifyZeroInteractions(subTask);
    }

    @Test
    public void testExecuteSubTask_Callable() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        Object result = new Object();
        Callable<?> subTask = mock(Callable.class);
        stub(subTask.call()).toReturn(result);

        assertSame(result, task.executeSubTask(subTask));

        verify(subTask).call();
        verifyNoMoreInteractions(subTask);
    }

    @Test(timeout = 5000)
    public void testExecuteSubTask_CallableWithException() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        Callable<?> subTask = mock(Callable.class);

        Exception expected = new Exception();
        stub(subTask.call()).toThrow(expected);

        assertNull(task.executeSubTask(subTask));
        assertTrue(task.getFuture().isDone());
        try {
            task.getFuture().get();
            fail("Task should have thrown an exception.");
        } catch (ExecutionException ex) {
            assertSame(expected, ex.getCause());
        }

        verify(subTask).call();
        verifyNoMoreInteractions(subTask);
    }

    @Test
    public void testExecuteSubTask_CallableAfterFinish() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        Callable<?> subTask = mock(Callable.class);

        task.finishTask(null, null, false);
        task.executeSubTask(subTask);

        verifyZeroInteractions(subTask);
    }

    @Test
    public void testExecuteSubTask_3args_1() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        CancellationSource cancelSource = Cancellation.createCancellationSource();
        CancelableTask subTask = mock(CancelableTask.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        Runnable cancelTestFailed = mock(Runnable.class);

        doAnswer(cancelIsEffective(cancelSource.getController(), cancelTestFailed))
                .when(subTask)
                .execute(any(CancellationToken.class));

        task.executeSubTask(cancelSource.getToken(), subTask, cleanupTask);

        verifyZeroInteractions(cancelTestFailed);

        verify(subTask).execute(any(CancellationToken.class));
        verify(cleanupTask).cleanup(eq(false), isNull(Throwable.class));
        verifyNoMoreInteractions(subTask, cleanupTask);
    }

    @Test
    public void testExecuteSubTask_3args_1AfterFinish() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        CancelableTask subTask = mock(CancelableTask.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        task.finishTask(null, null, false);
        task.executeSubTask(Cancellation.UNCANCELABLE_TOKEN, subTask, cleanupTask);

        verify(cleanupTask).cleanup(true, null);
        verifyZeroInteractions(subTask);
    }

    @Test
    public void testExecuteSubTask_3args_2() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        CancelableFunction<?> subTask = mock(CancelableFunction.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);
        final Runnable cancelTestFailed = mock(Runnable.class);

        final Object result = new Object();
        stub(subTask.execute(any(CancellationToken.class))).toAnswer((InvocationOnMock invocation) -> {
            cancelIsEffective(cancelSource.getController(), cancelTestFailed).answer(invocation);
            return result;
        });

        assertSame(result, task.executeSubTask(cancelSource.getToken(), subTask, cleanupTask));

        verifyZeroInteractions(cancelTestFailed);

        verify(subTask).execute(any(CancellationToken.class));
        verify(cleanupTask).cleanup(eq(false), isNull(Throwable.class));
        verifyNoMoreInteractions(subTask, cleanupTask);
    }

    @Test
    public void testExecuteSubTask_3args_2AfterFinish() throws Exception {
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(null);

        CancelableFunction<?> subTask = mock(CancelableFunction.class);
        CleanupTask cleanupTask = mock(CleanupTask.class);

        task.finishTask(null, null, false);
        assertNull(task.executeSubTask(Cancellation.UNCANCELABLE_TOKEN, subTask, cleanupTask));

        verify(cleanupTask).cleanup(true, null);
        verifyZeroInteractions(subTask);
    }

    @SuppressWarnings("unchecked")
    private static <T> MultiPhaseTask.TerminateListener<T> mockListener() {
        return mock(MultiPhaseTask.TerminateListener.class);
    }

    private void checkCompletedTask(MultiPhaseTask<?> task, Object expected) throws Exception {
        assertTrue(task.isDone());

        Future<?> future = task.getFuture();
        assertSame(expected, future.get());
        assertSame(expected, future.get(0, TimeUnit.NANOSECONDS));
        assertSame(expected, future.get(Long.MAX_VALUE, TimeUnit.DAYS));
        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
    }

    private void checkFailedTask(MultiPhaseTask<?> task, Throwable expected) throws Exception {
        assertTrue(task.isDone());

        Future<?> future = task.getFuture();

        try {
            future.get();
            fail("Exception expected.");
        } catch (ExecutionException ex) {
            assertSame(expected, ex.getCause());
        }
        try {
            future.get(0, TimeUnit.NANOSECONDS);
            fail("Exception expected.");
        } catch (ExecutionException ex) {
            assertSame(expected, ex.getCause());
        }
        try {
            future.get(Long.MAX_VALUE, TimeUnit.DAYS);
            fail("Exception expected.");
        } catch (ExecutionException ex) {
            assertSame(expected, ex.getCause());
        }
        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
    }

    private void checkCanceledTask(MultiPhaseTask<?> task) throws Exception {
        assertTrue(task.isDone());

        Future<?> future = task.getFuture();

        try {
            future.get();
            fail("Cancellation expected.");
        } catch (CancellationException ex) {
        }
        try {
            future.get(0, TimeUnit.NANOSECONDS);
            fail("Cancellation expected.");
        } catch (CancellationException ex) {
        }
        try {
            future.get(Long.MAX_VALUE, TimeUnit.DAYS);
            fail("Cancellation expected.");
        } catch (CancellationException ex) {
        }
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());
    }

    @Test(timeout = 5000)
    public void testFinishTwice() throws Exception {
        MultiPhaseTask.TerminateListener<Object> listener = mockListener();
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(listener);
        verifyZeroInteractions(listener);

        Object result = new Object();
        assertTrue(task.finishTask(result, null, false));
        verify(listener).onTerminate(same(result), isNull(Throwable.class), eq(false));

        assertTrue(task.finishTask(null, null, false));
        verifyNoMoreInteractions(listener);

        checkCompletedTask(task, result);
    }

    @Test(timeout = 5000)
    public void testFinishNormally() throws Exception {
        MultiPhaseTask.TerminateListener<Object> listener = mockListener();
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(listener);

        verifyZeroInteractions(listener);
        assertFalse(task.isDone());
        assertFalse(task.getFuture().isCancelled());
        assertFalse(task.getFuture().isDone());

        Object result = new Object();
        assertTrue(task.finishTask(result, null, false));
        verify(listener).onTerminate(same(result), isNull(Throwable.class), eq(false));
        verifyNoMoreInteractions(listener);

        checkCompletedTask(task, result);
    }

    @Test(timeout = 5000)
    public void testFinishWithFailure() throws Exception {
        MultiPhaseTask.TerminateListener<Object> listener = mockListener();
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(listener);

        Exception failure = new Exception();
        assertTrue(task.finishTask(null, failure, false));
        verify(listener).onTerminate(isNull(Object.class), same(failure), eq(false));
        verifyNoMoreInteractions(listener);

        checkFailedTask(task, failure);
    }

    @Test(timeout = 5000)
    public void testFinishWithCancellation() throws Exception {
        MultiPhaseTask.TerminateListener<Object> listener = mockListener();
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(listener);

        assertTrue(task.finishTask(null, null, true));
        verify(listener).onTerminate(null, null, true);
        verifyNoMoreInteractions(listener);

        checkCanceledTask(task);
    }

    @Test(timeout = 5000, expected = TimeoutException.class)
    public void testGetTimeout() throws Exception {
        MultiPhaseTask.TerminateListener<Object> listener = mockListener();
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(listener);

        task.getFuture().get(1, TimeUnit.NANOSECONDS);
    }

    @Test(timeout = 5000)
    public void testCancel() throws Exception {
        MultiPhaseTask.TerminateListener<Object> listener = mockListener();
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(listener);

        task.cancel();
        verify(listener).onTerminate(null, null, true);
        verifyNoMoreInteractions(listener);

        checkCanceledTask(task);
    }

    @Test(timeout = 5000)
    public void testCancelTwice() throws Exception {
        MultiPhaseTask.TerminateListener<Object> listener = mockListener();
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(listener);

        assertTrue(task.getFuture().cancel(true));
        verify(listener).onTerminate(null, null, true);
        assertTrue(task.getFuture().cancel(true));
        verifyNoMoreInteractions(listener);

        checkCanceledTask(task);
    }

    @Test(timeout = 5000)
    public void testCancelAfterFinish() throws Exception {
        MultiPhaseTask.TerminateListener<Object> listener = mockListener();
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(listener);

        Object result = new Object();
        assertTrue(task.finishTask(result, null, false));
        verify(listener).onTerminate(same(result), isNull(Throwable.class), eq(false));
        assertFalse(task.getFuture().cancel(true));
        verifyNoMoreInteractions(listener);

        checkCompletedTask(task, result);
    }

    @Test(timeout = 5000)
    public void testCancelThroughTask() throws Exception {
        MultiPhaseTask.TerminateListener<Object> listener = mockListener();
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(listener);

        task.executeSubTask(() -> {
            throw new OperationCanceledException();
        });
        verify(listener).onTerminate(isNull(Object.class), isA(OperationCanceledException.class), eq(true));
        verifyNoMoreInteractions(listener);

        assertTrue(task.isDone());
        assertTrue(task.getFuture().isCancelled());
        assertTrue(task.getFuture().isDone());
    }

    @Test(timeout = 5000)
    public void testQueuedTasksWhenFinish() throws Exception {
        MultiPhaseTask.TerminateListener<Object> listener = mockListener();
        MultiPhaseTask<Object> task = new MultiPhaseTask<>(listener);

        CancelableTask subTask1 = mock(CancelableTask.class);
        CancelableTask subTask2 = mock(CancelableTask.class);

        ManualTaskExecutor executor = new ManualTaskExecutor(false);

        task.executeSubTask(executor, Cancellation.UNCANCELABLE_TOKEN, subTask1, null);
        task.executeSubTask(executor, Cancellation.UNCANCELABLE_TOKEN, subTask2, null);

        executor.tryExecuteOne();

        Object result = new Object();
        assertTrue(task.finishTask(result, null, false));
        verify(listener).onTerminate(same(result), isNull(Throwable.class), eq(false));

        executor.tryExecuteOne();

        verify(subTask1).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(subTask1, listener);
        verifyZeroInteractions(subTask2);

        checkCompletedTask(task, result);
    }

    static class DelegatedTaskExecutor implements TaskExecutor {
        private final TaskExecutor executor;

        public DelegatedTaskExecutor(TaskExecutor executor) {
            this.executor = executor;
        }

        @Override
        public void execute(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
            executor.execute(cancelToken, task, cleanupTask);
        }
    }

    static class DelegatedUpdateTaskExecutor implements UpdateTaskExecutor {
        private final UpdateTaskExecutor executor;

        public DelegatedUpdateTaskExecutor(UpdateTaskExecutor executor) {
            this.executor = executor;
        }

        @Override
        public void execute(Runnable task) {
            executor.execute(task);
        }

        @Override
        public void shutdown() {
            executor.shutdown();
        }
    }
}
