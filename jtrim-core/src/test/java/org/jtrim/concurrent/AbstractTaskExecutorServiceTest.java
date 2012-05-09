package org.jtrim.concurrent;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.jtrim.cancel.*;
import org.jtrim.event.*;
import org.jtrim.utils.ExceptionHelper;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class AbstractTaskExecutorServiceTest {

    public AbstractTaskExecutorServiceTest() {
    }

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

    @Test
    public void testExecuteNoCleanup() {
        ManualExecutor executor = new ManualExecutor();

        final AtomicInteger executeCount = new AtomicInteger(0);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN,
                new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                assertFalse(cancelToken.isCanceled());
                executeCount.incrementAndGet();
            }
        }, null);

        executor.executeSubmittedTasks();
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
    }

    @Test
    public void testExecuteWithCleanup() {
        ManualExecutor executor = new ManualExecutor();

        final AtomicInteger executeCount = new AtomicInteger(0);
        final AtomicInteger cleanupCount = new AtomicInteger(0);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN,
                new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                assertFalse(cancelToken.isCanceled());
                assertEquals("Task was executed after cleanup", 0, cleanupCount.intValue());
                executeCount.incrementAndGet();
            }
        },
                new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) throws Exception {
                assertFalse(canceled);
                assertNull(error);
                assertEquals("Cleanup was executed before task", 1, executeCount.intValue());
                cleanupCount.incrementAndGet();
            }
        });

        executor.executeSubmittedTasks();
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
        assertEquals("Unexpected number of cleanup", 1, cleanupCount.intValue());
    }

    @Test
    public void testSubmitNoCleanup() {
        ManualExecutor executor = new ManualExecutor();

        final AtomicInteger executeCount = new AtomicInteger(0);
        TaskFuture<?> future = executor.submit(Cancellation.UNCANCELABLE_TOKEN,
                new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                assertFalse(cancelToken.isCanceled());
                executeCount.incrementAndGet();
            }
        }, null);

        assertNull(future.tryGetResult());
        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertNull(future.tryGetResult());
        assertSame(TaskState.DONE_COMPLETED, future.getTaskState());
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
        future.waitAndGet(Cancellation.CANCELED_TOKEN);
        future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test
    public void testSubmitWithCleanup() {
        ManualExecutor executor = new ManualExecutor();

        final AtomicInteger executeCount = new AtomicInteger(0);
        final AtomicInteger cleanupCount = new AtomicInteger(0);
        TaskFuture<?> future = executor.submit(Cancellation.UNCANCELABLE_TOKEN,
                new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                assertFalse(cancelToken.isCanceled());
                assertEquals("Task was executed after cleanup", 0, cleanupCount.intValue());
                executeCount.incrementAndGet();
            }
        },
                new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) throws Exception {
                assertFalse(canceled);
                assertNull(error);
                assertEquals("Cleanup was executed before task", 1, executeCount.intValue());
                cleanupCount.incrementAndGet();
            }
        });

        assertNull(future.tryGetResult());
        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertNull(future.tryGetResult());
        assertSame(TaskState.DONE_COMPLETED, future.getTaskState());
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
        assertEquals("Unexpected number of cleanup", 1, cleanupCount.intValue());
        future.waitAndGet(Cancellation.CANCELED_TOKEN);
        future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test
    public void testSubmitFunctionNoCleanup() {
        ManualExecutor executor = new ManualExecutor();

        final AtomicInteger executeCount = new AtomicInteger(0);
        final Object taskResult = "TASK-RESULT";

        TaskFuture<?> future = executor.submit(Cancellation.UNCANCELABLE_TOKEN,
                new CancelableFunction<Object>() {
            @Override
            public Object execute(CancellationToken cancelToken) {
                assertFalse(cancelToken.isCanceled());
                executeCount.incrementAndGet();
                return taskResult;
            }
        }, null);

        assertNull(future.tryGetResult());
        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_COMPLETED, future.getTaskState());
        assertSame(taskResult, future.tryGetResult());
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
        future.waitAndGet(Cancellation.CANCELED_TOKEN);
        future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test
    public void testSubmitFunctionWithCleanup() {
        ManualExecutor executor = new ManualExecutor();

        final AtomicInteger executeCount = new AtomicInteger(0);
        final AtomicInteger cleanupCount = new AtomicInteger(0);
        final Object taskResult = "TASK-RESULT";

        TaskFuture<?> future = executor.submit(Cancellation.UNCANCELABLE_TOKEN,
                new CancelableFunction<Object>() {
            @Override
            public Object execute(CancellationToken cancelToken) {
                assertFalse(cancelToken.isCanceled());
                assertEquals("Task was executed after cleanup", 0, cleanupCount.intValue());
                executeCount.incrementAndGet();
                return taskResult;
            }
        },
                new CleanupTask() {
            @Override
            public void cleanup(boolean canceled, Throwable error) throws Exception {
                assertFalse(canceled);
                assertNull(error);
                assertEquals("Cleanup was executed before task", 1, executeCount.intValue());
                cleanupCount.incrementAndGet();
            }
        });

        assertNull(future.tryGetResult());
        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_COMPLETED, future.getTaskState());
        assertSame(taskResult, future.tryGetResult());
        assertEquals("Unexpected number of execution", 1, executeCount.intValue());
        assertEquals("Unexpected number of cleanup", 1, cleanupCount.intValue());
        future.waitAndGet(Cancellation.CANCELED_TOKEN);
        future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test
    public void testCanceledSubmit() {
        ManualExecutor executor = new ManualExecutor();

        final AtomicInteger executeCount = new AtomicInteger(0);
        TaskFuture<?> future = executor.submit(Cancellation.CANCELED_TOKEN,
                new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                executeCount.incrementAndGet();
            }
        }, null);

        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        assertEquals("Unexpected number of execution", 0, executeCount.intValue());
    }

    @Test(expected = OperationCanceledException.class)
    public void testCanceledSubmitFuture() {
        ManualExecutor executor = new ManualExecutor();

        final AtomicInteger executeCount = new AtomicInteger(0);
        TaskFuture<?> future = executor.submit(Cancellation.CANCELED_TOKEN,
                new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                executeCount.incrementAndGet();
            }
        }, null);

        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        future.tryGetResult();
    }

    @Test
    public void testPostSubmitCanceledSubmit() {
        ManualExecutor executor = new ManualExecutor();

        CancellationSource cancelSource = new CancellationSource();
        final AtomicInteger executeCount = new AtomicInteger(0);
        TaskFuture<?> future = executor.submit(cancelSource.getToken(),
                new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                executeCount.incrementAndGet();
            }
        }, null);
        cancelSource.getController().cancel();

        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        assertEquals("Unexpected number of execution", 0, executeCount.intValue());
    }

    @Test(expected = TaskExecutionException.class)
    public void testSubmitError() {
        ManualExecutor executor = new ManualExecutor();

        TaskFuture<?> future = executor.submit(Cancellation.UNCANCELABLE_TOKEN,
                new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                throw new UnsupportedOperationException();
            }
        }, null);

        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_ERROR, future.getTaskState());

        try {
            future.tryGetResult();
        } catch (TaskExecutionException ex) {
            assertEquals(UnsupportedOperationException.class, ex.getCause().getClass());
            throw ex;
        }
    }

    @Test
    public void testUnregisterListener() {
        ManualExecutor executor = new ManualExecutor();

        RegCounterCancelToken cancelToken = new RegCounterCancelToken();
        TaskFuture<?> future = executor.submit(cancelToken,
                new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
            }
        }, null);

        assertSame(TaskState.NOT_STARTED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_COMPLETED, future.getTaskState());
        assertEquals("Remaining registered listener.",
                0, cancelToken.getRegistrationCount());
    }

    @Test
    public void testUnregisterListenerPreCancel() {
        ManualExecutor executor = new ManualExecutor();

        RegCounterCancelToken cancelToken = new RegCounterCancelToken(
                Cancellation.CANCELED_TOKEN);

        TaskFuture<?> future = executor.submit(cancelToken,
                new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
            }
        }, null);

        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        assertEquals("Remaining registered listener.",
                0, cancelToken.getRegistrationCount());
    }

    @Test
    public void testUnregisterListenerPostCancel() {
        ManualExecutor executor = new ManualExecutor();

        CancellationSource cancelSource = new CancellationSource();
        RegCounterCancelToken cancelToken = new RegCounterCancelToken(
                cancelSource.getToken());

        TaskFuture<?> future = executor.submit(cancelToken,
                new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
            }
        }, null);
        cancelSource.getController().cancel();

        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        executor.executeSubmittedTasks();
        assertSame(TaskState.DONE_CANCELED, future.getTaskState());
        assertEquals("Remaining registered listener.",
                0, cancelToken.getRegistrationCount());
    }

    @Test
    public void testAwaitTerminate() {
        ManualExecutor executor = new ManualExecutor();
        executor.shutdown();
        executor.awaitTermination(Cancellation.CANCELED_TOKEN);
        executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
    }

    private static class RegCounterCancelToken implements CancellationToken {
        private final AtomicLong regCounter;
        private final CancellationToken wrappedToken;

        public RegCounterCancelToken() {
            this(Cancellation.UNCANCELABLE_TOKEN);
        }

        public RegCounterCancelToken(CancellationToken wrappedToken) {
            this.regCounter = new AtomicLong(0);
            this.wrappedToken = wrappedToken;
        }

        @Override
        public ListenerRef addCancellationListener(Runnable listener) {
            final ListenerRef result
                    = wrappedToken.addCancellationListener(listener);

            regCounter.incrementAndGet();

            return new ListenerRef() {
                private final AtomicBoolean registered
                        = new AtomicBoolean(true);

                @Override
                public boolean isRegistered() {
                    return result.isRegistered();
                }

                @Override
                public void unregister() {
                    if (registered.getAndSet(false)) {
                        result.unregister();
                        regCounter.decrementAndGet();
                    }
                }
            };
        }

        @Override
        public boolean isCanceled() {
            return wrappedToken.isCanceled();
        }

        @Override
        public void checkCanceled() {
            wrappedToken.checkCanceled();
        }

        public long getRegistrationCount() {
            return regCounter.get();
        }
    }

    private static class SubmittedTask {
        public final CancellationToken cancelToken;
        public final CancellationController cancelController;
        public final CancelableTask task;
        public final Runnable cleanupTask;
        public final boolean hasUserDefinedCleanup;

        public SubmittedTask(
                CancellationToken cancelToken,
                CancellationController cancelController,
                CancelableTask task,
                Runnable cleanupTask,
                boolean hasUserDefinedCleanup) {

            this.cancelToken = cancelToken;
            this.cancelController = cancelController;
            this.task = task;
            this.cleanupTask = cleanupTask;
            this.hasUserDefinedCleanup = hasUserDefinedCleanup;
        }
    }

    private class ManualExecutor extends AbstractTaskExecutorService {
        private ListenerManager<Runnable, Void> listeners = new CopyOnTriggerListenerManager<>();
        private boolean shuttedDown = false;

        private final List<SubmittedTask> submittedTasks = new LinkedList<>();

        public void executeSubmittedTasks() {
            if (shuttedDown) {
                while (!submittedTasks.isEmpty()) {
                    SubmittedTask task = submittedTasks.remove(0);
                    task.cleanupTask.run();
                }
            }
            else {
                while (!submittedTasks.isEmpty()) {
                    SubmittedTask task = submittedTasks.remove(0);
                    task.task.execute(task.cancelToken);
                    task.cleanupTask.run();
                }
            }
        }

        @Override
        protected void submitTask(
                CancellationToken cancelToken,
                CancellationController cancelController,
                CancelableTask task,
                Runnable cleanupTask,
                boolean hasUserDefinedCleanup) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(cancelController, "cancelController");
            ExceptionHelper.checkNotNullArgument(task, "task");
            ExceptionHelper.checkNotNullArgument(cleanupTask, "cleanupTask");

            submittedTasks.add(new SubmittedTask(cancelToken, cancelController,
                    task, cleanupTask, hasUserDefinedCleanup));
        }

        @Override
        public void shutdown() {
            shuttedDown = true;

            ListenerManager<Runnable, Void> currentListeners = listeners;
            if (currentListeners != null) {
                listeners = null;
                currentListeners.onEvent(new EventDispatcher<Runnable, Void>() {
                    @Override
                    public void onEvent(Runnable eventListener, Void arg) {
                        eventListener.run();
                    }
                }, null);
            }
        }

        @Override
        public void shutdownAndCancel() {
            shutdown();
        }

        @Override
        public boolean isShutdown() {
            return shuttedDown;
        }

        @Override
        public boolean isTerminated() {
            return shuttedDown;
        }

        @Override
        public ListenerRef addTerminateListener(Runnable listener) {
            if (listeners == null) {
                listener.run();
                return UnregisteredListenerRef.INSTANCE;
            }
            else {
                return listeners.registerListener(listener);
            }
        }

        @Override
        public boolean awaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
            return shuttedDown;
        }
    }
}
