package org.jtrim2.executor;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.TaskState;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class CancelableTasksTest {
    @Before
    public void setUp() {
        // clear interrupted status
        Thread.interrupted();
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(CancelableTasks.class);
    }

    @Test
    public void testRunOnceCancelableTask() throws Exception {
        CancelableTask subTask = mock(CancelableTask.class);
        stub(subTask.toString()).toReturn("TEST");

        CancelableTask task = CancelableTasks.runOnceCancelableTask(subTask, false);
        assertNotNull(task.toString());

        task.execute(Cancellation.CANCELED_TOKEN);
        verify(subTask).execute(any(CancellationToken.class));
        assertNotNull(task.toString());

        task.execute(Cancellation.UNCANCELABLE_TOKEN);
        verify(subTask).execute(any(CancellationToken.class));
        assertNotNull(task.toString());

        task.execute(Cancellation.UNCANCELABLE_TOKEN);
        verify(subTask).execute(any(CancellationToken.class));
        assertNotNull(task.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testRunOnceCancelableTaskFailOnReRun() throws Exception {
        CancelableTask subTask = mock(CancelableTask.class);
        CancelableTask task = CancelableTasks.runOnceCancelableTask(subTask, true);

        try {
            try {
                task.execute(Cancellation.UNCANCELABLE_TOKEN);
            } catch (IllegalStateException ex) {
                throw new RuntimeException(ex);
            }
            task.execute(Cancellation.UNCANCELABLE_TOKEN);
        } finally {
            verify(subTask).execute(any(CancellationToken.class));
        }
    }

    /**
     * Test of canceledTaskFuture method, of class Tasks.
     */
    @Test
    public void testCanceledTaskFuture() {
        TaskFuture<?> future = CancelableTasks.canceledTaskFuture();
        assertNotNull(future.toString());
        assertEquals(TaskState.DONE_CANCELED, future.getTaskState());

        try {
            future.tryGetResult();
            fail("Expected cancellation.");
        } catch (OperationCanceledException ex) {
        }
        try {
            future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
            fail("Expected cancellation.");
        } catch (OperationCanceledException ex) {
        }
        try {
            future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
            fail("Expected cancellation.");
        } catch (OperationCanceledException ex) {
        }
    }

    @Test
    public void testExecuteTaskWithCleanup() throws Exception {
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        CancelableTasks.executeTaskWithCleanup(cancelToken, task, cleanup);

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(cancelToken);
        inOrder.verify(cleanup).cleanup(false, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testExecuteTaskWithCleanupPreCanceled() throws Exception {
        CancellationToken cancelToken = Cancellation.CANCELED_TOKEN;
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        CancelableTasks.executeTaskWithCleanup(cancelToken, task, cleanup);

        verifyZeroInteractions(task);
        verify(cleanup).cleanup(true, null);
    }

    @Test
    public void testExecuteTaskWithCleanupTaskIsCanceled() throws Exception {
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        Throwable exception = new OperationCanceledException();
        doThrow(exception)
                .when(task)
                .execute(any(CancellationToken.class));

        CancelableTasks.executeTaskWithCleanup(cancelToken, task, cleanup);

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(cancelToken);
        inOrder.verify(cleanup).cleanup(true, exception);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testExecuteTaskWithCleanupExceptionInTask() throws Exception {
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        Throwable exception = new Exception();
        doThrow(exception)
                .when(task)
                .execute(any(CancellationToken.class));

        CancelableTasks.executeTaskWithCleanup(cancelToken, task, cleanup);

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(cancelToken);
        inOrder.verify(cleanup).cleanup(false, exception);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testExecuteTaskWithCleanupExceptionInCleanup() throws Exception {
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        doThrow(new TestException())
                .when(cleanup)
                .cleanup(anyBoolean(), any(Throwable.class));

        try (LogCollector logs = LogTests.startCollecting()) {
            CancelableTasks.executeTaskWithCleanup(cancelToken, task, cleanup);
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(cancelToken);
        inOrder.verify(cleanup).cleanup(false, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testExecuteTaskWithCleanupExceptionInTaskNoCleanup() throws Exception {
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        CancelableTask task = mock(CancelableTask.class);

        doThrow(new TestException())
                .when(task)
                .execute(any(CancellationToken.class));

        try (LogCollector logs = LogTests.startCollecting()) {
            CancelableTasks.executeTaskWithCleanup(cancelToken, task, null);
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }

        verify(task).execute(cancelToken);
    }

    @Test
    public void testExecuteTaskWithCleanupNullCleanup() throws Exception {
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        CancelableTask task = mock(CancelableTask.class);

        CancelableTasks.executeTaskWithCleanup(cancelToken, task, null);

        verify(task).execute(cancelToken);
        verifyNoMoreInteractions(task);
    }

    @Test
    public void testExecuteTaskWithCleanupNullTask() throws Exception {
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        CleanupTask cleanup = mock(CleanupTask.class);

        CancelableTasks.executeTaskWithCleanup(cancelToken, null, cleanup);

        verify(cleanup).cleanup(true, null);
        verifyNoMoreInteractions(cleanup);
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
