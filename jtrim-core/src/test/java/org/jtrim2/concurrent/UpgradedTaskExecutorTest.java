package org.jtrim2.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class UpgradedTaskExecutorTest {
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

    @Test(timeout = 5000)
    public void testSubmitTask() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        TaskFuture<?> future = upgraded.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(task).execute(any(CancellationToken.class));
        verify(cleanup).cleanup(anyBoolean(), any(Throwable.class));
        verifyNoMoreInteractions(task, cleanup);

        assertEquals(TaskState.DONE_COMPLETED, future.getTaskState());
        assertNull(future.tryGetResult());
        assertNull(future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN));
        assertNull(future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));
    }

    @Test(timeout = 5000)
    public void testSubmitTaskWithoutCleanup() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        CancelableTask task = mock(CancelableTask.class);

        TaskFuture<?> future = upgraded.submit(Cancellation.UNCANCELABLE_TOKEN, task, null);
        verify(task).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(task);

        assertEquals(TaskState.DONE_COMPLETED, future.getTaskState());
        assertNull(future.tryGetResult());
        assertNull(future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN));
        assertNull(future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));
    }

    @Test(timeout = 5000)
    public void testExecuteExceptionTaskWithoutCleanup() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        CancelableTask task = mock(CancelableTask.class);

        doThrow(new TestException())
                .when(task)
                .execute(any(CancellationToken.class));

        try (LogCollector logs = LogCollectorTest.startCollecting()) {
            upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
            LogCollectorTest.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }

        verify(task).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(task);
    }

    @Test(timeout = 5000)
    public void testSubmitTaskAfterShutdown() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        upgraded.shutdown();
        TaskFuture<?> future = upgraded.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verifyZeroInteractions(task);
        verify(cleanup).cleanup(anyBoolean(), any(Throwable.class));
        verifyNoMoreInteractions(cleanup);

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

    @Test(timeout = 5000)
    public void testCancelByShutdown() throws Exception {
        final UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        CleanupTask cleanup = mock(CleanupTask.class);

        final AtomicBoolean canceled = new AtomicBoolean(false);
        TaskFuture<?> future = upgraded.submit(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            upgraded.shutdownAndCancel();
            canceled.set(cancelToken.isCanceled());
        }, cleanup);
        verify(cleanup).cleanup(anyBoolean(), any(Throwable.class));
        verifyNoMoreInteractions(cleanup);

        assertEquals(TaskState.DONE_COMPLETED, future.getTaskState());
        assertNull(future.tryGetResult());
        assertNull(future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN));
        assertNull(future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));
        assertTrue(canceled.get());
    }

    @Test(timeout = 5000)
    public void testExceptionInTask() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        Exception thrownException = new Exception();
        doThrow(thrownException).when(task).execute(any(CancellationToken.class));

        TaskFuture<?> future = upgraded.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(task).execute(any(CancellationToken.class));
        verify(cleanup).cleanup(anyBoolean(), any(Throwable.class));
        verifyNoMoreInteractions(task, cleanup);

        assertEquals(TaskState.DONE_ERROR, future.getTaskState());

        try {
            future.tryGetResult();
        } catch (TaskExecutionException ex) {
            assertSame(thrownException, ex.getCause());
        }

        try {
            future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
        } catch (TaskExecutionException ex) {
            assertSame(thrownException, ex.getCause());
        }

        try {
            future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (TaskExecutionException ex) {
            assertSame(thrownException, ex.getCause());
        }
    }

    @Test(timeout = 5000)
    public void testSubmitTaskExceptionInCleanup() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        doThrow(new TestException()).when(cleanup).cleanup(anyBoolean(), any(Throwable.class));

        TaskFuture<?> future;
        try (LogCollector logs = LogCollectorTest.startCollecting()) {
            future = upgraded.submit(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            LogCollectorTest.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }
        TaskFuture<?> future2 = upgraded.submit(Cancellation.UNCANCELABLE_TOKEN, task2, null);

        verify(task).execute(any(CancellationToken.class));
        verify(cleanup).cleanup(anyBoolean(), any(Throwable.class));
        verifyNoMoreInteractions(task, cleanup);

        assertEquals(TaskState.DONE_COMPLETED, future.getTaskState());
        assertNull(future.tryGetResult());
        assertNull(future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN));
        assertNull(future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));

        assertEquals(TaskState.DONE_COMPLETED, future2.getTaskState());
        assertNull(future2.tryGetResult());
        assertNull(future2.waitAndGet(Cancellation.UNCANCELABLE_TOKEN));
        assertNull(future2.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));
    }

    @Test(timeout = 5000)
    public void testShutdownExecutesQueue() throws Exception {
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(false);
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(manualExecutor);
        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        TaskFuture<?> future1 = upgraded.submit(Cancellation.UNCANCELABLE_TOKEN, task1, null);
        TaskFuture<?> future2 = upgraded.submit(Cancellation.UNCANCELABLE_TOKEN, task2, null);
        manualExecutor.tryExecuteOne();
        upgraded.shutdown();
        manualExecutor.tryExecuteOne();

        verify(task1).execute(any(CancellationToken.class));
        verify(task2).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(task1, task2);

        assertEquals(TaskState.DONE_COMPLETED, future1.getTaskState());
        assertNull(future1.tryGetResult());
        assertNull(future1.waitAndGet(Cancellation.UNCANCELABLE_TOKEN));
        assertNull(future1.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));

        assertEquals(TaskState.DONE_COMPLETED, future2.getTaskState());
        assertNull(future2.tryGetResult());
        assertNull(future2.waitAndGet(Cancellation.UNCANCELABLE_TOKEN));
        assertNull(future2.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));

    }

    @Test(timeout = 5000)
    public void testShutdownAndCancelCancelsQueued() throws Exception {
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(true);
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(manualExecutor);
        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        TaskFuture<?> future1 = upgraded.submit(Cancellation.UNCANCELABLE_TOKEN, task1, null);
        TaskFuture<?> future2 = upgraded.submit(Cancellation.UNCANCELABLE_TOKEN, task2, null);
        manualExecutor.tryExecuteOne();
        upgraded.shutdownAndCancel();
        manualExecutor.tryExecuteOne();

        verify(task1).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(task1);
        verifyZeroInteractions(task2);

        assertEquals(TaskState.DONE_COMPLETED, future1.getTaskState());
        assertNull(future1.tryGetResult());
        assertNull(future1.waitAndGet(Cancellation.UNCANCELABLE_TOKEN));
        assertNull(future1.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));

        assertEquals(TaskState.DONE_CANCELED, future2.getTaskState());
        try {
            future2.tryGetResult();
            fail("Expected cancellation.");
        } catch (OperationCanceledException ex) {
        }
        try {
            future2.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
            fail("Expected cancellation.");
        } catch (OperationCanceledException ex) {
        }
        try {
            future2.waitAndGet(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
            fail("Expected cancellation.");
        } catch (OperationCanceledException ex) {
        }
    }

    @Test(timeout = 5000)
    public void testAwaitAfterImmediateShutdown() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        upgraded.shutdown();
        assertTrue(upgraded.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));
    }

    @Test(timeout = 5000)
    public void testAwaitAfterShutdown() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, mock(CancelableTask.class), null);
        upgraded.shutdown();
        assertTrue(upgraded.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));
    }

    @Test(timeout = 5000)
    public void testAwaitShutdownTimeouts() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, mock(CancelableTask.class), null);
        upgraded.shutdown();
        assertTrue(upgraded.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, 100, TimeUnit.NANOSECONDS));
    }

    @Test(timeout = 5000)
    public void testToString() {
        // There is nothing to test in the format of toString() but at least
        // we can verify that toString() does not throw an exception.
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        assertNotNull(upgraded.toString());
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
