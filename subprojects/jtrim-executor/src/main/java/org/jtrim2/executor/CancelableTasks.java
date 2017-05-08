package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.TaskState;

/**
 * Defines static methods to return simple, convenient cancelable task related instances.
 * <P>
 * This class cannot be inherited nor instantiated.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 */
public final class CancelableTasks {
    private static final Logger LOGGER = Logger.getLogger(CancelableTasks.class.getName());

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

    private static class RunOnceCancelableTask implements CancelableTask {
        private final boolean failOnReRun;
        private final AtomicReference<CancelableTask> taskRef;

        public RunOnceCancelableTask(CancelableTask task, boolean failOnReRun) {
            Objects.requireNonNull(task, "task");
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

    private CancelableTasks() {
        throw new AssertionError();
    }
}
