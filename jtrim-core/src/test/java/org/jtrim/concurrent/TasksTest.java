package org.jtrim.concurrent;

import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class TasksTest {

    public TasksTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        // clear interrupted status
        Thread.interrupted();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of noOpTask method, of class Tasks.
     */
    @Test
    public void testNoOpTask() {
        Runnable task = Tasks.noOpTask();
        assertNotNull(task.toString());
        task.run();
    }

    @Test
    public void testNoOpCancelableTask() throws Exception {
        CancelableTask task = Tasks.noOpCancelableTask();
        assertNotNull(task.toString());
        task.execute(Cancellation.CANCELED_TOKEN);
        task.execute(Cancellation.UNCANCELABLE_TOKEN);
    }

    /**
     * Test of runOnceTask method, of class Tasks.
     */
    @Test
    public void testRunOnceTask() {
        Runnable subTask = mock(Runnable.class);
        stub(subTask.toString()).toReturn("TEST");

        Runnable task = Tasks.runOnceTask(subTask, false);
        assertNotNull(task.toString());

        task.run();
        verify(subTask).run();
        assertNotNull(task.toString());

        task.run();
        verify(subTask).run();
    }

    @Test(expected = IllegalStateException.class)
    public void testRunOnceTaskFailOnReRun() {
        Runnable subTask = mock(Runnable.class);
        Runnable task = Tasks.runOnceTask(subTask, true);

        try {
            try {
                task.run();
            } catch (IllegalStateException ex) {
                throw new RuntimeException(ex);
            }
            task.run();
        } finally {
            verify(subTask).run();
        }
    }

    @Test
    public void testRunOnceCancelableTask() throws Exception {
        CancelableTask subTask = mock(CancelableTask.class);
        stub(subTask.toString()).toReturn("TEST");

        CancelableTask task = Tasks.runOnceCancelableTask(subTask, false);
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
        CancelableTask task = Tasks.runOnceCancelableTask(subTask, true);

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
        TaskFuture<?> future = Tasks.canceledTaskFuture();
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

        Tasks.executeTaskWithCleanup(cancelToken, task, cleanup);

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

        Tasks.executeTaskWithCleanup(cancelToken, task, cleanup);

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

        Tasks.executeTaskWithCleanup(cancelToken, task, cleanup);

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

        Tasks.executeTaskWithCleanup(cancelToken, task, cleanup);

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

        doThrow(Exception.class)
                .when(cleanup)
                .cleanup(anyBoolean(), any(Throwable.class));

        Tasks.executeTaskWithCleanup(cancelToken, task, cleanup);

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(cancelToken);
        inOrder.verify(cleanup).cleanup(false, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testExecuteTaskWithCleanupExceptionInTaskNoCleanup() throws Exception {
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        CancelableTask task = mock(CancelableTask.class);

        Throwable exception = new Exception();
        doThrow(exception)
                .when(task)
                .execute(any(CancellationToken.class));

        Tasks.executeTaskWithCleanup(cancelToken, task, null);

        verify(task).execute(cancelToken);
    }

    @Test
    public void testExecuteTaskWithCleanupNullCleanup() throws Exception {
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        CancelableTask task = mock(CancelableTask.class);

        Tasks.executeTaskWithCleanup(cancelToken, task, null);

        verify(task).execute(cancelToken);
        verifyNoMoreInteractions(task);
    }

    @Test
    public void testExecuteTaskWithCleanupNullTask() throws Exception {
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        CleanupTask cleanup = mock(CleanupTask.class);

        Tasks.executeTaskWithCleanup(cancelToken, null, cleanup);

        verify(cleanup).cleanup(true, null);
        verifyNoMoreInteractions(cleanup);
    }

    @Test(timeout = 30000)
    public void testRunConcurrently() {
        for (int testIndex = 0; testIndex < 100; testIndex++) {
            for (int taskCount = 0; taskCount < 5; taskCount++) {
                Runnable[] tasks = new Runnable[taskCount];
                for (int i = 0; i < tasks.length; i++) {
                    tasks[i] = mock(Runnable.class);
                }

                Tasks.runConcurrently(tasks);

                for (int i = 0; i < tasks.length; i++) {
                    verify(tasks[i]).run();
                }
            }
        }
    }

    @Test(timeout = 30000)
    public void testRunConcurrentlyWithInterrupt() {
        Runnable task1 = mock(Runnable.class);
        Runnable task2 = mock(Runnable.class);

        Thread.currentThread().interrupt();
        Tasks.runConcurrently(task1, task2);
        assertTrue(Thread.currentThread().isInterrupted());

        verify(task1).run();
        verify(task2).run();
    }

    @Test(timeout = 30000)
    public void testRunConcurrentlyWithException() {
        Runnable task1 = mock(Runnable.class);
        Runnable task2 = mock(Runnable.class);
        Runnable task3 = mock(Runnable.class);
        Runnable task4 = mock(Runnable.class);

        RuntimeException ex2 = new RuntimeException();
        RuntimeException ex3 = new RuntimeException();

        doThrow(ex2).when(task2).run();
        doThrow(ex3).when(task3).run();

        try {
            Tasks.runConcurrently(task1, task2, task3, task4);
            fail("Expected TaskExecutionException.");
        } catch (TaskExecutionException ex) {
            assertSame(ex2, ex.getCause());

            Throwable[] suppressed = ex.getSuppressed();
            assertEquals(1, suppressed.length);
            assertSame(ex3, suppressed[0]);
        }

        verify(task1).run();
        verify(task2).run();
        verify(task3).run();
        verify(task4).run();
    }
}
