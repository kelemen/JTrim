package org.jtrim2.executor;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.executor.MockCleanup;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class SyncTaskExecutorTest {
    private void testExceptionWithCleanup(TaskExecutor executor, boolean wrappedCancel) throws Exception {
        CancellationToken cancelToken = Cancellation.createCancellationSource().getToken();
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        Exception exception = new Exception();

        doThrow(exception).when(task).execute(any(CancellationToken.class));

        executor.execute(cancelToken, task)
                .whenComplete(MockCleanup.toCleanupTask(cleanup));

        InOrder inOrder = inOrder(task, cleanup);
        if (wrappedCancel) {
            inOrder.verify(task).execute(any(CancellationToken.class));
        }
        else {
            inOrder.verify(task).execute(cancelToken);
        }
        inOrder.verify(cleanup).cleanup(isNull(), same(exception));
        inOrder.verifyNoMoreInteractions();
    }

    private void testExceptionInRunnable(TaskExecutor executor) throws Exception {
        Runnable task = mock(Runnable.class);

        doThrow(new TestException()).when(task).run();

        try (LogCollector logs = LogTests.startCollecting()) {
            executor.execute(task);
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }

        verify(task).run();
        verifyNoMoreInteractions(task);
    }

    private void testCancelledWithCleanup(TaskExecutor executor) throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        executor.execute(Cancellation.CANCELED_TOKEN, task)
                .whenComplete(MockCleanup.toCleanupTask(cleanup));

        verifyZeroInteractions(task);
        verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
        verifyNoMoreInteractions(cleanup);
    }

    private void testCancelledWithoutCleanup(TaskExecutor executor) throws Exception {
        CancelableTask task = mock(CancelableTask.class);

        executor.execute(Cancellation.CANCELED_TOKEN, task);

        verifyZeroInteractions(task);
    }

    private void testSimpleWithCleanup(TaskExecutor executor, boolean wrappedCancel) throws Exception {
        CancellationToken cancelToken = Cancellation.createCancellationSource().getToken();
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        executor.execute(cancelToken, task)
                .whenComplete(MockCleanup.toCleanupTask(cleanup));

        InOrder inOrder = inOrder(task, cleanup);
        if (wrappedCancel) {
            inOrder.verify(task).execute(any(CancellationToken.class));
        }
        else {
            inOrder.verify(task).execute(cancelToken);
        }
        inOrder.verify(cleanup).cleanup(null, null);
        inOrder.verifyNoMoreInteractions();
    }

    private void testSimpleWithoutCleanup(TaskExecutor executor, boolean wrappedCancel) throws Exception {
        CancellationToken cancelToken = Cancellation.createCancellationSource().getToken();
        CancelableTask task = mock(CancelableTask.class);

        executor.execute(cancelToken, task);

        if (wrappedCancel) {
            verify(task).execute(any(CancellationToken.class));
        }
        else {
            verify(task).execute(cancelToken);
        }
        verifyNoMoreInteractions(task);
    }

    @Test
    public void testExceptionWithCleanup() throws Exception {
        testExceptionWithCleanup(SyncTaskExecutor.getSimpleExecutor(), false);
        testExceptionWithCleanup(SyncTaskExecutor.getDefaultInstance(), true);
        testExceptionWithCleanup(new SyncTaskExecutor(), true);
    }

    @Test
    public void testExceptionInRunnable() throws Exception {
        testExceptionInRunnable(SyncTaskExecutor.getSimpleExecutor());
        testExceptionInRunnable(SyncTaskExecutor.getDefaultInstance());
        testExceptionInRunnable(new SyncTaskExecutor());
    }

    @Test
    public void testCancelledWithCleanup() throws Exception {
        testCancelledWithCleanup(SyncTaskExecutor.getSimpleExecutor());
        testCancelledWithCleanup(SyncTaskExecutor.getDefaultInstance());
        testCancelledWithCleanup(new SyncTaskExecutor());
    }

    @Test
    public void testCancelledWithoutCleanup() throws Exception {
        testCancelledWithoutCleanup(SyncTaskExecutor.getSimpleExecutor());
        testCancelledWithoutCleanup(SyncTaskExecutor.getDefaultInstance());
        testCancelledWithoutCleanup(new SyncTaskExecutor());
    }

    @Test
    public void testSimpleWithCleanup() throws Exception {
        testSimpleWithCleanup(SyncTaskExecutor.getSimpleExecutor(), false);
        testSimpleWithCleanup(SyncTaskExecutor.getDefaultInstance(), true);
        testSimpleWithCleanup(new SyncTaskExecutor(), true);
    }

    @Test
    public void testSimpleWithoutCleanup() throws Exception {
        testSimpleWithoutCleanup(SyncTaskExecutor.getSimpleExecutor(), false);
        testSimpleWithoutCleanup(SyncTaskExecutor.getDefaultInstance(), true);
        testSimpleWithoutCleanup(new SyncTaskExecutor(), true);
    }

    @Test
    public void testMonitoredValues() {
        final SyncTaskExecutor executor = new SyncTaskExecutor();

        assertFalse(executor.isExecutingInThis());
        assertEquals(0, executor.getNumberOfExecutingTasks());
        assertEquals(0, executor.getNumberOfQueuedTasks());

        final List<Long> numberOfQueuedTasks = new LinkedList<>();
        final List<Long> numberOfExecutingTasks = new LinkedList<>();
        final List<Boolean> inContext = new LinkedList<>();

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            numberOfQueuedTasks.add(executor.getNumberOfQueuedTasks());
            numberOfExecutingTasks.add(executor.getNumberOfExecutingTasks());
            inContext.add(executor.isExecutingInThis());
        });

        assertFalse(executor.isExecutingInThis());
        assertEquals(0, executor.getNumberOfExecutingTasks());
        assertEquals(0, executor.getNumberOfQueuedTasks());

        assertEquals(Arrays.asList(0L), numberOfQueuedTasks);
        assertEquals(Arrays.asList(1L), numberOfExecutingTasks);
        assertEquals(Arrays.asList(true), inContext);
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
