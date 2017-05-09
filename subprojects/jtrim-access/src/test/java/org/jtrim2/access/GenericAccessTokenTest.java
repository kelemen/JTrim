package org.jtrim2.access;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.jtrim2.cancel.CancelableWaits;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.CancelableTasks;
import org.jtrim2.executor.CleanupTask;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.ThreadPoolTaskExecutor;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class GenericAccessTokenTest {
    private static LogCollector startCollecting() {
        return LogCollector.startCollecting("org.jtrim2");
    }

    private static <T> GenericAccessToken<T> create(T id) {
        return new GenericAccessToken<>(id);
    }

    @Test
    public void testID() {
        String testID = "TEST-TOKEN-ID";
        AccessToken<?> token = create(testID);
        assertEquals(testID, token.getAccessID());
    }

    @Test
    public void testReleaseListenerAfterMultipleRelease() {
        AccessToken<?> token = create("");
        Runnable listener = mock(Runnable.class);

        token.addReleaseListener(listener);

        verifyZeroInteractions(listener);
        assertFalse(token.isReleased());
        token.release();
        token.release();
        assertTrue(token.isReleased());
        verify(listener).run();
    }

    @Test
    public void testReleaseListenerReleaseInTask() throws Throwable {
        try (LogCollector logs = startCollecting()) {
            final GenericAccessToken<?> token = create("");
            final Runnable listener1 = mock(Runnable.class);
            final Runnable listener2 = mock(Runnable.class);
            final Runnable listener3 = mock(Runnable.class);
            final Runnable listener4 = mock(Runnable.class);

            doThrow(new TestException()).when(listener2).run();
            doThrow(new TestException()).when(listener3).run();

            token.addReleaseListener(listener1);
            token.addReleaseListener(listener2);
            token.addReleaseListener(listener3);
            token.addReleaseListener(listener4);

            verifyZeroInteractions(listener1, listener2, listener3, listener4);
            assertFalse(token.isReleased());

            final AtomicReference<Throwable> verifyError = new AtomicReference<>(null);
            TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                try {
                    token.release();
                    verifyZeroInteractions(listener1, listener2, listener3, listener4);
                } catch (Throwable ex) {
                    verifyError.set(ex);
                }
            }, null);

            if (verifyError.get() != null) {
                throw verifyError.get();
            }

            assertTrue(token.isReleased());
            verify(listener1).run();
            verify(listener2).run();
            verify(listener3).run();
            verify(listener4).run();

            Throwable[] allLogged = LogCollector.extractThrowables(
                    TestException.class, logs.getExceptions(Level.SEVERE));
            assertEquals(2, allLogged.length);
        }
    }

    @Test
    public void testReleaseListenerPreRelease() {
        try (LogCollector logs = startCollecting()) {
            AccessToken<?> token = create("");
            Runnable listener1 = mock(Runnable.class);
            Runnable listener2 = mock(Runnable.class);
            Runnable listener3 = mock(Runnable.class);
            Runnable listener4 = mock(Runnable.class);

            doThrow(new TestException()).when(listener2).run();
            doThrow(new TestException()).when(listener3).run();

            token.addReleaseListener(listener1);
            token.addReleaseListener(listener2);
            token.addReleaseListener(listener3);
            token.addReleaseListener(listener4);

            verifyZeroInteractions(listener1, listener2, listener3, listener4);

            Throwable thrown = null;
            assertFalse(token.isReleased());
            try {
                token.release();
            } catch (TestException ex) {
                thrown = ex;
            }
            assertTrue(token.isReleased());
            verify(listener1).run();
            verify(listener2).run();
            verify(listener3).run();
            verify(listener4).run();

            Throwable[] allThrown = LogCollector.extractThrowables(
                    TestException.class, thrown);
            Throwable[] allLogged = LogCollector.extractThrowables(
                    TestException.class, logs.getExceptions(Level.SEVERE));
            assertEquals(2, allLogged.length + allThrown.length);
        }
    }

    @Test
    public void testReleaseListenerPostRelease() {
        AccessToken<?> token = create("");
        Runnable listener = mock(Runnable.class);

        assertFalse(token.isReleased());
        token.release();
        assertTrue(token.isReleased());

        token.addReleaseListener(listener);
        verify(listener).run();
    }

    @Test
    public void testReleaseWithExecute() throws Exception {
        AccessToken<?> token = create("");
        Runnable listener = mock(Runnable.class);
        CancelableTask task = mock(CancelableTask.class);

        TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
        verify(task).execute(any(CancellationToken.class));

        token.addReleaseListener(listener);
        token.release();
        verify(listener).run();

        verifyNoMoreInteractions(listener, task);
    }

    @Test(timeout = 10000)
    public void testAwaitRelease() {
        GenericAccessToken<String> token = create("");
        token.release();
        token.awaitRelease(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test(timeout = 10000)
    public void testReleaseConcurrentWithTask() {
        asyncTest(1, (GenericAccessToken<?> token, TaskExecutor executor) -> {
            final CountDownLatch latch = new CountDownLatch(1);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                CancelableWaits.await(cancelToken, latch::await);
            }, null);

            token.release();
            latch.countDown();

            token.tryAwaitRelease(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.SECONDS);
        });
    }

    @Test
    public void testReleaseConcurrentWithTasks() {
        asyncTest(1, (GenericAccessToken<?> token, TaskExecutor executor) -> {
            final CountDownLatch latch = new CountDownLatch(1);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                CancelableWaits.await(cancelToken, latch::await);
            }, null);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask(), null);

            token.release();
            latch.countDown();

            token.tryAwaitRelease(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.SECONDS);
        });
    }

    @Test
    public void testReleaseAndCancelPriorSubmit() throws Exception {
        GenericAccessToken<?> token = create("");
        final TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);
        CleanupTask cleanup2 = mock(CleanupTask.class);

        token.releaseAndCancel();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1, null);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2, cleanup2);

        verifyZeroInteractions(task1, task2);
        verify(cleanup2).cleanup(true, null);
    }

    @Test
    public void testReleaseAndCancelAfterSubmitBeforeExecute() throws Exception {
        GenericAccessToken<?> token = create("");
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(false);
        final TaskExecutor executor = token.createExecutor(manualExecutor);

        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);
        CleanupTask cleanup2 = mock(CleanupTask.class);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1, null);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2, cleanup2);
        token.releaseAndCancel();
        assertEquals(2, manualExecutor.executeCurrentlySubmitted());

        verifyZeroInteractions(task1, task2);
        verify(cleanup2).cleanup(eq(true), isA(OperationCanceledException.class));
    }

    @Test
    public void testReleaseListenerReleaseAfterSubmitBeforeExecute() throws Exception {
        GenericAccessToken<?> token = create("");
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(false);
        final TaskExecutor executor = token.createExecutor(manualExecutor);

        final Runnable listener1 = mock(Runnable.class);
        final Runnable listener2 = mock(Runnable.class);
        final Runnable listener3 = mock(Runnable.class);
        final Runnable listener4 = mock(Runnable.class);

        doThrow(RuntimeException.class).when(listener2).run();
        doThrow(RuntimeException.class).when(listener3).run();

        token.addReleaseListener(listener1);
        token.addReleaseListener(listener2);
        token.addReleaseListener(listener3);
        token.addReleaseListener(listener4);

        verifyZeroInteractions(listener1, listener2, listener3, listener4);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask(), null);
        token.releaseAndCancel();
        try {
            manualExecutor.executeCurrentlySubmitted();
        } catch (RuntimeException ex) {
            // Ignore exceptions of listeners.
        }

        verify(listener1).run();
        verify(listener2).run();
        verify(listener3).run();
        verify(listener4).run();
    }

    @Test
    public void testReleaseAndCancelAffectsRunningTask() {
        asyncTest(1, (final GenericAccessToken<?> token, TaskExecutor executor) -> {
            final AtomicBoolean preNotCanceled = new AtomicBoolean(false);
            final AtomicBoolean postCanceled = new AtomicBoolean(false);

            final WaitableSignal taskCompleted = new WaitableSignal();
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                preNotCanceled.set(!cancelToken.isCanceled());
                token.releaseAndCancel();
                postCanceled.set(cancelToken.isCanceled());
                taskCompleted.signal();
            }, null);

            taskCompleted.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

            assertTrue(preNotCanceled.get());
            assertTrue(postCanceled.get());
        });
    }

    @Test
    public void testContextAwareness() {
        final GenericAccessToken<?> token = create("");
        final TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        final AtomicBoolean inContextTask = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            inContextTask.set(token.isExecutingInThis());
        }, (boolean canceled, Throwable error) -> {
            inContextCleanupTask.set(token.isExecutingInThis());
        });

        assertFalse(token.isExecutingInThis());
        assertTrue(inContextTask.get());
        assertTrue(inContextCleanupTask.get());
    }

    @Test
    public void testRecursiveContextAwarenessInTask() {
        final GenericAccessToken<?> token = create("");
        final TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        final AtomicBoolean inContextTask = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken subTaskCancelToken) -> {
                inContextTask.set(token.isExecutingInThis());
            }, (boolean canceled, Throwable error) -> {
                inContextCleanupTask.set(token.isExecutingInThis());
            });
        }, null);

        assertFalse(token.isExecutingInThis());
        assertTrue(inContextTask.get());
        assertTrue(inContextCleanupTask.get());
    }

    @Test
    public void testRecursiveContextAwarenessInCleanup() {
        final GenericAccessToken<?> token = create("");
        final TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        final AtomicBoolean inContextTask = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask = new AtomicBoolean(false);

        CancelableTask noop = CancelableTasks.noOpCancelableTask();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, noop, (boolean canceled, Throwable error) -> {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                inContextTask.set(token.isExecutingInThis());
            }, (boolean subCanceled, Throwable subError) -> {
                inContextCleanupTask.set(token.isExecutingInThis());
            });
        });

        assertFalse(token.isExecutingInThis());
        assertTrue(inContextTask.get());
        assertTrue(inContextCleanupTask.get());
    }

    @Test
    public void testExceptionInTask() throws Exception {
        GenericAccessToken<?> token = create("");
        final TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        Throwable exception = new RuntimeException();
        doThrow(exception).when(task).execute(any(CancellationToken.class));

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);

        verify(cleanup).cleanup(eq(false), same(exception));
    }

    @Test
    public void testToString() {
        GenericAccessToken<String> token = create("");
        assertNotNull(token.toString());
    }

    private static void awaitTermination(TaskExecutorService executor) {
        if (!executor.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, 20, TimeUnit.SECONDS)) {
            throw new OperationCanceledException("timeout");
        }
    }

    private static void asyncTest(int threadCount, TestMethod method) {
        GenericAccessToken<?> token = create("");
        TaskExecutorService threadPool = new ThreadPoolTaskExecutor("TEST-POOL", threadCount);
        try {
            method.doTest(token, token.createExecutor(threadPool));
        } catch (Throwable ex) {
            throw ExceptionHelper.throwUnchecked(ex);
        } finally {
            threadPool.shutdown();
            awaitTermination(threadPool);
        }
    }

    private static interface TestMethod {
        public void doTest(GenericAccessToken<?> token, TaskExecutor executor) throws Throwable;
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 573260112468259165L;
    }
}
