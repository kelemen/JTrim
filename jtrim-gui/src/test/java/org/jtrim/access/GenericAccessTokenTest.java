package org.jtrim.access;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.CancelableWaits;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.InterruptibleWait;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;
import org.junit.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class GenericAccessTokenTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
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
        final GenericAccessToken<?> token = create("");
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
        assertFalse(token.isReleased());

        final AtomicReference<Throwable> verifyError = new AtomicReference<>(null);
        ContextAwareTaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                try {
                    token.release();
                    verifyZeroInteractions(listener1, listener2, listener3, listener4);
                } catch (Throwable ex) {
                    verifyError.set(ex);
                }
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
    }

    @Test
    public void testReleaseListenerPreRelease() {
        AccessToken<?> token = create("");
        Runnable listener1 = mock(Runnable.class);
        Runnable listener2 = mock(Runnable.class);
        Runnable listener3 = mock(Runnable.class);
        Runnable listener4 = mock(Runnable.class);

        doThrow(RuntimeException.class).when(listener2).run();
        doThrow(RuntimeException.class).when(listener3).run();

        token.addReleaseListener(listener1);
        token.addReleaseListener(listener2);
        token.addReleaseListener(listener3);
        token.addReleaseListener(listener4);

        verifyZeroInteractions(listener1, listener2, listener3, listener4);

        assertFalse(token.isReleased());
        try {
            token.release();
        } catch (RuntimeException ex) {
            // Ignore exceptions of listeners.
        }
        assertTrue(token.isReleased());
        verify(listener1).run();
        verify(listener2).run();
        verify(listener3).run();
        verify(listener4).run();
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
        asyncTest(1, new TestMethod() {
            @Override
            public void doTest(GenericAccessToken<?> token, ContextAwareTaskExecutor executor) {
                final CountDownLatch latch = new CountDownLatch(1);
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        CancelableWaits.await(cancelToken, new InterruptibleWait() {
                            @Override
                            public void await() throws InterruptedException {
                                latch.await();
                            }
                        });
                    }
                }, null);

                token.release();
                latch.countDown();

                token.tryAwaitRelease(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.SECONDS);
            }
        });
    }

    @Test
    public void testReleaseConcurrentWithTasks() {
        asyncTest(1, new TestMethod() {
            @Override
            public void doTest(GenericAccessToken<?> token, ContextAwareTaskExecutor executor) {
                final CountDownLatch latch = new CountDownLatch(1);
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        CancelableWaits.await(cancelToken, new InterruptibleWait() {
                            @Override
                            public void await() throws InterruptedException {
                                latch.await();
                            }
                        });
                    }
                }, null);
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), null);

                token.release();
                latch.countDown();

                token.tryAwaitRelease(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.SECONDS);
            }
        });
    }

    @Test
    public void testReleaseAndCancelPriorSubmit() throws Exception {
        GenericAccessToken<?> token = create("");
        final ContextAwareTaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

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
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor();
        final ContextAwareTaskExecutor executor = token.createExecutor(manualExecutor);

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
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor();
        final ContextAwareTaskExecutor executor = token.createExecutor(manualExecutor);

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

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), null);
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
        asyncTest(1, new TestMethod() {
            @Override
            public void doTest(final GenericAccessToken<?> token, ContextAwareTaskExecutor executor) {
                final AtomicBoolean preNotCanceled = new AtomicBoolean(false);
                final AtomicBoolean postCanceled = new AtomicBoolean(false);

                final WaitableSignal taskCompleted = new WaitableSignal();
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) throws Exception {
                        preNotCanceled.set(!cancelToken.isCanceled());
                        token.releaseAndCancel();
                        postCanceled.set(cancelToken.isCanceled());
                        taskCompleted.signal();
                    }
                }, null);

                taskCompleted.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

                assertTrue(preNotCanceled.get());
                assertTrue(postCanceled.get());
            }
        });
    }

    @Test
    public void testContextAwareness() {
        GenericAccessToken<?> token = create("");
        final ContextAwareTaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        final AtomicBoolean inContextTask = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                inContextTask.set(executor.isExecutingInThis());
            }
        }, new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) {
                inContextCleanupTask.set(executor.isExecutingInThis());
            }
        });

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask.get());
        assertTrue(inContextCleanupTask.get());
    }

    @Test
    public void testRecursiveContextAwarenessInTask() {
        GenericAccessToken<?> token = create("");
        final ContextAwareTaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        final AtomicBoolean inContextTask = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        inContextTask.set(executor.isExecutingInThis());
                    }
                }, new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) {
                        inContextCleanupTask.set(executor.isExecutingInThis());
                    }
                });
            }
        }, null);

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask.get());
        assertTrue(inContextCleanupTask.get());
    }

    @Test
    public void testRecursiveContextAwarenessInCleanup() {
        GenericAccessToken<?> token = create("");
        final ContextAwareTaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

        final AtomicBoolean inContextTask = new AtomicBoolean(false);
        final AtomicBoolean inContextCleanupTask = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        inContextTask.set(executor.isExecutingInThis());
                    }
                }, new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) {
                        inContextCleanupTask.set(executor.isExecutingInThis());
                    }
                });
            }
        });

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask.get());
        assertTrue(inContextCleanupTask.get());
    }

    @Test
    public void testExceptionInTask() throws Exception {
        GenericAccessToken<?> token = create("");
        final ContextAwareTaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());

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

    private static void asyncTest(int threadCount, TestMethod method) {
        GenericAccessToken<?> token = create("");
        TaskExecutorService threadPool = new ThreadPoolTaskExecutor("TEST-POOL", threadCount);
        try {
            method.doTest(token, token.createExecutor(threadPool));
        } catch (Throwable ex) {
            ExceptionHelper.rethrow(ex);
        } finally {
            threadPool.shutdown();
            if (!threadPool.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, 20, TimeUnit.SECONDS)) {
                throw new OperationCanceledException("timeout");
            }
        }
    }

    private static interface TestMethod {
        public void doTest(GenericAccessToken<?> token, ContextAwareTaskExecutor executor) throws Throwable;
    }

    private static final class ManualTaskExecutor implements TaskExecutor {
        private final Lock mainLock;
        private final List<TaskExecutorJob> jobs;

        public ManualTaskExecutor() {
            this.mainLock = new ReentrantLock();
            this.jobs = new LinkedList<>();
        }

        public boolean tryExecuteOne() {
            TaskExecutorJob job;
            mainLock.lock();
            try {
                job = jobs.isEmpty() ? null : jobs.remove(0);
            } finally {
                mainLock.unlock();
            }

            if (job != null) {
                try {
                    job.execute();
                } catch (Throwable ex) {
                    throw new TaskExecutionException(ex);
                }
                return true;
            }
            else {
                return false;
            }
        }

        public int executeCurrentlySubmitted() {
            TaskExecutorJob[] currentJobs;
            mainLock.lock();
            try {
                currentJobs = jobs.toArray(new TaskExecutorJob[jobs.size()]);
                jobs.clear();
            } finally {
                mainLock.unlock();
            }

            TaskExecutionException toThrow = null;
            for (TaskExecutorJob job: currentJobs) {
                try {
                    job.execute();
                } catch (Throwable ex) {
                    if (toThrow == null) toThrow = new TaskExecutionException(ex);
                    else toThrow.addSuppressed(ex);
                }
            }

            if (toThrow != null) {
                throw toThrow;
            }

            return currentJobs.length;
        }

        @Override
        public void execute(
                CancellationToken cancelToken,
                CancelableTask task,
                CleanupTask cleanupTask) {

            TaskExecutorJob job = new TaskExecutorJob(cancelToken, task, cleanupTask);
            mainLock.lock();
            try {
                jobs.add(job);
            } finally {
                mainLock.unlock();
            }
        }
    }

    private static final class TaskExecutorJob {
        private final CancellationToken cancelToken;
        private final CancelableTask task;
        private final CleanupTask cleanupTask;

        public TaskExecutorJob(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            this.cancelToken = cancelToken;
            this.task = task;
            this.cleanupTask = cleanupTask;
        }

        public void execute() throws Exception {
            boolean canceled = false;
            Throwable taskError = null;

            try {
                task.execute(cancelToken);
            } catch (OperationCanceledException ex) {
                canceled = true;
                taskError = ex;
            } catch (Throwable ex) {
                taskError = ex;
            } finally {
                cleanupTask.cleanup(canceled, taskError);
            }
        }
    }
}
