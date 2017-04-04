package org.jtrim.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines static methods to return simple, convenient task related instances.
 * <P>
 * This class cannot be inherited nor instantiated.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class Tasks {
    private static final Logger LOGGER = Logger.getLogger(Tasks.class.getName());

    /**
     * Returns a {@code Runnable} whose {@code run()} method does nothing but
     * returns immediately to the caller.
     *
     * @return a {@code Runnable} whose {@code run()} method does nothing but
     *   returns immediately to the caller. This method never returns
     *   {@code null}.
     */
    public static Runnable noOpTask() {
        return NoOp.INSTANCE;
    }

    /**
     * Returns a {@code CancelableTask} whose {@code execute} method does
     * nothing but returns immediately to the caller.
     *
     * @return a {@code CancelableTask} whose {@code execute} method does
     *   nothing but returns immediately to the caller. This method never
     *   returns {@code null}.
     */
    public static CancelableTask noOpCancelableTask() {
        return CancelableNoOp.INSTANCE;
    }

    /**
     * Returns a {@code Runnable} which will execute the specified
     * {@code Runnable} but will execute the specified {@code Runnable} only
     * once. The specified task will not be executed more than once even if
     * it is called multiple times concurrently (and is allowed to be called
     * concurrently).
     * <P>
     * What happens when the returned task is attempted to be executed depends
     * on the {@code failOnReRun} argument. If the argument is {@code true},
     * attempting to call the {@code run} method of the returned
     * {@code Runnable} multiple times will cause an
     * {@code IllegalStateException} to be thrown. If the argument is
     * {@code false}, calling the {@code run} method of the returned
     * {@code Runnable} multiple times will only result in a single execution
     * and every other call (not actually executing the specified
     * {@code Runnable}) will silently return without doing anything.
     *
     * @param task the {@code Runnable} to which calls are to be forwarded by
     *   the returned {@code Runnable}. This method cannot be {@code null}.
     * @param failOnReRun if {@code true} multiple calls to the {@code run()}
     *   method of the returned {@code Runnable} will cause an
     *   {@code IllegalStateException} to be thrown. If this argument is
     *   {@code false} subsequent calls after the first call to the
     *   {@code run()} method of the returned {@code Runnable} will silently
     *   return without doing anything.
     * @return the {@code Runnable} which will execute the specified
     *   {@code Runnable} but will execute the specified {@code Runnable} only
     *   once. This method never returns {@code null}.
     */
    public static Runnable runOnceTask(
            Runnable task, boolean failOnReRun) {

        return new RunOnceTask(task, failOnReRun);
    }

    /**
     * Returns a {@code CancelableTask} which will execute the specified
     * {@code CancelableTask} (forwarding the arguments passed to it) but will
     * execute the specified {@code CancelableTask} only once. The specified
     * task will not be executed more than once even if it is called multiple
     * times concurrently (and is allowed to be called concurrently).
     * <P>
     * What happens when the returned task is attempted to be executed depends
     * on the {@code failOnReRun} argument. If the argument is {@code true},
     * attempting to call the {@code execute} method of the returned
     * {@code CancelableTask} multiple times will cause an
     * {@code IllegalStateException} to be thrown. If the argument is
     * {@code false}, calling the {@code CancelableTask} method of the returned
     * {@code CancelableTask} multiple times will only result in a single
     * execution and every other call (not actually executing the specified
     * {@code CancelableTask}) will silently return without doing anything.
     *
     * @param task the {@code CancelableTask} to which calls are to be forwarded
     *   by the returned {@code CancelableTask}. This method cannot be
     *   {@code null}.
     * @param failOnReRun if {@code true} multiple calls to the
     *   {@code CancelableTask} method of the returned {@code CancelableTask}
     *   will cause an {@code IllegalStateException} to be thrown. If this
     *   argument is {@code false} subsequent calls after the first call to the
     *   {@code CancelableTask} method of the returned {@code CancelableTask}
     *   will silently return without doing anything.
     * @return the {@code CancelableTask} which will execute the specified
     *   {@code CancelableTask} but will execute the specified
     *   {@code CancelableTask} only once. This method never returns
     *   {@code null}.
     */
    public static CancelableTask runOnceCancelableTask(
            CancelableTask task, boolean failOnReRun) {

        return new RunOnceCancelableTask(task, failOnReRun);
    }

    /**
     * Returns a {@code TaskFuture} which is already in the
     * {@link TaskState#DONE_CANCELED} state. Note that the state of the
     * returned {@code TaskFuture} will never change.
     * <P>
     * Attempting the retrieve the result of the returned {@code TaskFuture}
     * will immediately throw an {@link OperationCanceledException} without
     * waiting.
     *
     * @param <V> the type of the result of the returned {@code TaskFuture}.
     *   Note that the returned {@code TaskFuture} will enver actually return
     *   anything of this type (not even {@code null}).
     * @return the {@code TaskFuture} which is already in the
     *   {@link TaskState#DONE_CANCELED} state. This method never returns
     *   {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <V> TaskFuture<V> canceledTaskFuture() {
        // This is safe because we never actually return any result
        // and throw an exception instead.
        return (TaskFuture<V>)CanceledTaskFuture.INSTANCE;
    }

    /**
     * Executes the specified tasks concurrently, each on a separate thread,
     * attempting to execute them as concurrently as possible. This method was
     * designed for test codes wanting to test the behaviour of multiple tasks
     * if run concurrently. This method will attempt to start the passed tasks
     * in sync (this does not imply thread-safety guarantees), so that there is
     * a better chance that they actually run concurrently.
     * <P>
     * This method will wait until all the specified tasks complete.
     * <P>
     * <B>Warning</B>: This method was <B>not</B> designed to give better
     * performance by running the tasks concurrently. Performance of this method
     * is secondary to any other purposes.
     *
     * @param tasks the tasks to be run concurrently. Each of the specified
     *   tasks will run on a dedicated thread. This argument and its elements
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the argument or any of the
     *   specified tasks is {@code null}
     * @throws TaskExecutionException thrown if any of the tasks thrown an
     *   exception. The first exception (in the order they were passed) is the
     *   cause of this exception and subsequent exceptions are suppressed
     *   (via {@code Throwable.addSuppressed}).
     */
    public static void runConcurrently(Runnable... tasks) {
        ExceptionHelper.checkNotNullElements(tasks, "tasks");

        final CountDownLatch latch = new CountDownLatch(tasks.length);
        Thread[] threads = new Thread[tasks.length];
        final Throwable[] exceptions = new Throwable[tasks.length];

        try {
            for (int i = 0; i < threads.length; i++) {
                final Runnable task = tasks[i];

                final int threadIndex = i;
                threads[i] = new Thread(() -> {
                    try {
                        latch.countDown();
                        latch.await();

                        task.run();
                    } catch (Throwable ex) {
                        exceptions[threadIndex] = ex;
                    }
                });
                try {
                    threads[i].start();
                } catch (Throwable ex) {
                    threads[i] = null;
                    throw ex;
                }
            }
        } finally {
            joinThreadsSilently(threads);
        }

        TaskExecutionException toThrow = null;
        for (int i = 0; i < exceptions.length; i++) {
            Throwable current = exceptions[i];
            if (current != null) {
                if (toThrow == null) toThrow = new TaskExecutionException(current);
                else toThrow.addSuppressed(current);
            }
        }
        if (toThrow != null) {
            throw toThrow;
        }
    }

    private static void joinThreadsSilently(Thread[] threads) {
        boolean interrupted = false;
        for (int i = 0; i < threads.length; i++) {
            if (threads[i] == null) continue;

            boolean threadStopped = false;
            while (!threadStopped) {
                try {
                    threads[i].join();
                    threadStopped = true;
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static void executeCleanup(
            CleanupTask cleanupTask,
            boolean canceled,
            Throwable error) {

        if (cleanupTask != null) {
            try {
                cleanupTask.cleanup(canceled, error);
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE,
                        "A cleanup task has thrown an exception", ex);
            }
        }
        else if (error != null && !(error instanceof OperationCanceledException)) {
            LOGGER.log(Level.SEVERE,
                    "An exception occured in a task not having a cleanup task.",
                    error);
        }
    }

    static void executeTaskWithCleanup(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {

        boolean canceled = true;
        Throwable error = null;
        try {
            if (task != null && !cancelToken.isCanceled()) {
                task.execute(cancelToken);
                canceled = false;
            }
        } catch (OperationCanceledException ex) {
            error = ex;
        } catch (Throwable ex) {
            error = ex;
            canceled = false;
        } finally {
            executeCleanup(cleanupTask, canceled, error);
        }
    }

    private static class RunOnceTask implements Runnable {
        private final boolean failOnReRun;
        private final AtomicReference<Runnable> taskRef;

        public RunOnceTask(Runnable task, boolean failOnReRun) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.taskRef = new AtomicReference<>(task);
            this.failOnReRun = failOnReRun;
        }

        @Override
        public void run() {
            Runnable task = taskRef.getAndSet(null);
            if (task == null) {
                if (failOnReRun) {
                    throw new IllegalStateException("This task is not allowed"
                            + " to be called multiple times.");
                }
            }
            else {
                task.run();
            }
        }

        @Override
        public String toString() {
            final String strValueCaption = "Idempotent task";
            Runnable currentTask = taskRef.get();
            if (currentTask != null) {
                return strValueCaption + "{" + currentTask + "}";
            }
            else {
                return strValueCaption + "{Already executed}";
            }
        }
    }

    private static class RunOnceCancelableTask implements CancelableTask {
        private final boolean failOnReRun;
        private final AtomicReference<CancelableTask> taskRef;

        public RunOnceCancelableTask(CancelableTask task, boolean failOnReRun) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.taskRef = new AtomicReference<>(task);
            this.failOnReRun = failOnReRun;
        }

        @Override
        public void execute(CancellationToken cancelToken) throws Exception {
            CancelableTask task = taskRef.getAndSet(null);
            if (task == null) {
                if (failOnReRun) {
                    throw new IllegalStateException("This task is not allowed"
                            + " to be called multiple times.");
                }
            }
            else {
                task.execute(cancelToken);
            }
        }

        @Override
        public String toString() {
            final String strValueCaption = "Idempotent task";
            CancelableTask currentTask = taskRef.get();
            if (currentTask != null) {
                return strValueCaption + "{" + currentTask + "}";
            }
            else {
                return strValueCaption + "{Already executed}";
            }
        }
    }

    private enum CanceledTaskFuture implements TaskFuture<Object> {
        INSTANCE;

        @Override
        public TaskState getTaskState() {
            return TaskState.DONE_CANCELED;
        }

        @Override
        public Object tryGetResult() {
            throw new OperationCanceledException();
        }

        @Override
        public Object waitAndGet(CancellationToken cancelToken) {
            return tryGetResult();
        }

        @Override
        public Object waitAndGet(CancellationToken cancelToken,
                long timeout, TimeUnit timeUnit) {
            return tryGetResult();
        }

        @Override
        public String toString() {
            return "CANCELED";
        }
    }

    private enum CancelableNoOp implements CancelableTask {
        INSTANCE;

        @Override
        public void execute(CancellationToken cancelToken) {
        }

        @Override
        public String toString() {
            return "NO-OP";
        }
    }

    private enum NoOp implements Runnable {
        INSTANCE;

        @Override
        public void run() { }

        @Override
        public String toString() {
            return "NO-OP";
        }
    }

    private Tasks() {
        throw new AssertionError();
    }
}
