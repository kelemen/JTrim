package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;

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
        return (cancelToken) -> { };
    }


    /**
     * Returns a {@code CancelableTask} which will execute the specified
     * {@code CancelableTask} (forwarding the arguments passed to it) but will
     * execute the specified {@code CancelableTask} only once. The specified
     * task will not be executed more than once even if it is called multiple
     * times concurrently (and is allowed to be called concurrently).
     * <P>
     * Calling the {@code execute} method of the returned {@code Runnable} multiple times
     * will only result in a single execution and every other call (not actually
     * executing the specified {@code CancelableTask}) will silently return without doing anything.
     *
     * @param task the {@code CancelableTask} to which calls are to be forwarded
     *   by the returned {@code CancelableTask}. This method cannot be
     *   {@code null}.
     * @return the {@code CancelableTask} which will execute the specified
     *   {@code CancelableTask} but will execute the specified
     *   {@code CancelableTask} only once. This method never returns
     *   {@code null}.
     */
    public static CancelableTask runOnceCancelableTask(CancelableTask task) {
        return new RunOnceCancelableTask(task, false);
    }

    /**
     * Returns a {@code CancelableTask} which will execute the specified
     * {@code CancelableTask} (forwarding the arguments passed to it) but will
     * execute the specified {@code CancelableTask} only once, failing on
     * multiple execute attempts. The specified task will not be executed more
     * than once even if it is called multiple times concurrently
     * (and is allowed to be called concurrently).
     * <P>
     * Attempting to call the {@code run} method of the returned
     * {@code Runnable} multiple times will cause an
     * {@code IllegalStateException} to be thrown after the first attempt.
     *
     * @param task the {@code CancelableTask} to which calls are to be forwarded
     *   by the returned {@code CancelableTask}. This method cannot be
     *   {@code null}.
     * @return the {@code CancelableTask} which will execute the specified
     *   {@code CancelableTask} but will execute the specified
     *   {@code CancelableTask} only once. This method never returns
     *   {@code null}.
     */
    public static CancelableTask runOnceCancelableTaskStrict(CancelableTask task) {
        return new RunOnceCancelableTask(task, true);
    }

    /**
     * Returns a {@code CompletionStage} which was already completed exceptionally
     * with an {@link OperationCanceledException}.
     *
     * @param <T> the type of the result of the {@code CompletionStage}
     * @return a {@code CompletionStage} which was already completed exceptionally
     *   with an {@link OperationCanceledException}. This method never returns {@code null}.
     */
    public static <T> CompletionStage<T> canceledComplationStage() {
        CompletableFuture<T> result = new CompletableFuture<>();
        result.completeExceptionally(OperationCanceledException.withoutStackTrace());

        return result;
    }

    static <V> void complete(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function,
            CompletableFuture<V> future) {
        try {
            if (cancelToken.isCanceled()) {
                future.completeExceptionally(OperationCanceledException.withoutStackTrace());
                return;
            }

            V result = function.execute(cancelToken);
            future.complete(result);
        } catch (Throwable ex) {
            future.completeExceptionally(ex);
        }
    }

    static void executeAndLogError(Runnable task) {
        try {
            task.run();
        } catch (OperationCanceledException ex) {
            // Cancellation is a normal event
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "An ignored exception of an asynchronous task have been thrown.", ex);
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
            } else {
                task.execute(cancelToken);
            }
        }

        @Override
        public String toString() {
            final String strValueCaption = "Idempotent task";
            CancelableTask currentTask = taskRef.get();
            if (currentTask != null) {
                return strValueCaption + "{" + currentTask + "}";
            } else {
                return strValueCaption + "{Already executed}";
            }
        }
    }

    private CancelableTasks() {
        throw new AssertionError();
    }
}
