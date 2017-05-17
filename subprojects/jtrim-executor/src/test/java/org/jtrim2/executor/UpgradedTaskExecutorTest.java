package org.jtrim2.executor;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.executor.MockCleanup;
import org.jtrim2.testutils.executor.MockFunction;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class UpgradedTaskExecutorTest {
    public static class GenericTest extends GenericExecutorTests {
        public GenericTest() {
            super(Arrays.asList(() -> new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor())));
        }
    }

    @Test
    public void testExecuteTask() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                .whenComplete(MockCleanup.toCleanupTask(cleanup));

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(any(CancellationToken.class));
        inOrder.verify(cleanup).cleanup(null, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testExecuteFunction() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        Object result = "test-result-234343t4";
        MockFunction<Object> task = MockFunction.mock(result);
        MockCleanup cleanup = mock(MockCleanup.class);

        upgraded.executeFunction(Cancellation.UNCANCELABLE_TOKEN, MockFunction.toFunction(task))
                .whenComplete(MockCleanup.toCleanupTask(cleanup));

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(false);
        inOrder.verify(cleanup).cleanup(same(result), isNull(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testExecuteRunnable() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        Runnable task = mock(Runnable.class);

        upgraded.execute(task);
        verify(task).run();
    }

    @Test
    public void testExecuteExceptionInRunnable() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        Runnable task = mock(Runnable.class);

        doThrow(new TestException())
                .when(task)
                .run();

        try (LogCollector logs = LogTests.startCollecting()) {
            upgraded.execute(task);
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }

        verify(task).run();
        verifyNoMoreInteractions(task);
    }

    @Test
    public void testExecuteTaskAfterShutdown() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        upgraded.shutdown();
        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                .whenComplete(MockCleanup.toCleanupTask(cleanup));

        verifyZeroInteractions(task);
        verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
    }

    @Test
    public void testCancelByShutdown() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        MockCleanup cleanup = mock(MockCleanup.class);

        AtomicBoolean canceled = new AtomicBoolean(false);
        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            upgraded.shutdownAndCancel();
            canceled.set(cancelToken.isCanceled());
        }).whenComplete(MockCleanup.toCleanupTask(cleanup));

        verify(cleanup).cleanup(null, null);

        assertTrue(canceled.get());
    }

    @Test
    public void testExceptionInTask() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        Exception thrownException = new Exception();
        doThrow(thrownException).when(task).execute(any(CancellationToken.class));

        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                .whenComplete(MockCleanup.toCleanupTask(cleanup));

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(any(CancellationToken.class));
        inOrder.verify(cleanup).cleanup(isNull(), same(thrownException));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testShutdownExecutesQueue() throws Exception {
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(false);
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(manualExecutor);
        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, task1);
        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, task2);

        manualExecutor.tryExecuteOne();
        upgraded.shutdown();
        manualExecutor.tryExecuteOne();

        verify(task1).execute(any(CancellationToken.class));
        verify(task2).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(task1, task2);
    }

    @Test
    public void testShutdownAndCancelCancelsQueued() throws Exception {
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(true);
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(manualExecutor);
        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, task1);
        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, task2);
        manualExecutor.tryExecuteOne();
        upgraded.shutdownAndCancel();
        manualExecutor.tryExecuteOne();

        verify(task1).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(task1);
        verifyZeroInteractions(task2);
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
        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, mock(CancelableTask.class));
        upgraded.shutdown();
        assertTrue(upgraded.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));
    }

    @Test(timeout = 5000)
    public void testAwaitShutdownTimeouts() throws Exception {
        UpgradedTaskExecutor upgraded = new UpgradedTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        upgraded.execute(Cancellation.UNCANCELABLE_TOKEN, mock(CancelableTask.class));
        upgraded.shutdown();
        assertTrue(upgraded.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, 100, TimeUnit.NANOSECONDS));
    }

    @Test
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
