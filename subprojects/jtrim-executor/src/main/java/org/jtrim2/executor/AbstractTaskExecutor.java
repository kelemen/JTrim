package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;

/**
 * Defines a convenient abstract base class for {@link TaskExecutor} implementations.
 * <P>
 * {@code AbstractTaskExecutor} defines default implementations for all
 * the {@code execute} methods which all rely on the protected
 * {@link #submitTask(CancellationToken, SubmittedTask) submitTask}
 * method. Only this {@code submitTask} method is needed to be implemented by
 * subclasses to actually schedule a task. Note that all the {@code execute}
 * methods rely directly the {@code submitTask} method and
 * overriding any of them has no effect on the others (i.e.: they don't call
 * each other). For further details on how to implement the {@code submitTask}
 * method: see its documentation.
 */
public abstract class AbstractTaskExecutor implements TaskExecutor {
    /**
     * Implementations must override this method to actually execute submitted
     * tasks.
     * <P>
     * Assuming no cancellation requests, implementations must call
     * {@link SubmittedTask#execute(CancellationToken) submittedTask.execute}.
     * <P>
     * Cancellation requests can be detected using the provided
     * {@code CancellationToken} and if an implementation chooses not to even
     * try to execute {@code task}, it must only call
     * {@link SubmittedTask#cancel() submittedTask.cancel}.
     * <P>
     * Implementations must always complete the passed task in some way. Either
     * by calling its {@code execute} or its {@code completeExceptionally} (or {@code cancel})
     * method.
     *
     * @param cancelToken the {@code CancellationToken} which can be checked by
     *   implementations if the currently submitted task has been canceled.
     *   Also this is the {@code CancellationToken} implementations should pass
     *   to {@code task}. This argument cannot be {@code null}.
     * @param submittedTask the task to be executed. Implementations must execute this task at most
     *   once by calling its {@link SubmittedTask#execute(CancellationToken) execute} method.
     *   If the execute method is not called, an exceptional return state must be set via
     *   the {@link SubmittedTask#completeExceptionally(Throwable) completeExceptionally}
     *   method. This argument cannot be {@code null}.
     */
    protected abstract void submitTask(CancellationToken cancelToken, SubmittedTask<?> submittedTask);

    /**
     * {@inheritDoc }
     */
    @Override
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {
        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(function, "function");

        if (cancelToken.isCanceled()) {
            return CancelableTasks.canceledComplationStage();
        }

        SubmittedTask<V> submittedTask = new SubmittedTask<>(function);
        submitTask(cancelToken, submittedTask);

        return submittedTask.getFuture();
    }

    /**
     * Defines the submitted task to be executed by subclasses of {@link AbstractTaskExecutor}.
     * <P>
     * Implementations of {@code AbstractTaskExecutor} must eventually complete the {@code SubmittedTask}
     * in a way: {@link #execute(CancellationToken) execute},
     * {@link #completeExceptionally(Throwable) completeExceptionally} or {@link #cancel() cancel}.
     *
     * @param <V> the type of the returned value of the submitted function
     *
     * @see AbstractTaskExecutor#submitTask(CancellationToken, SubmittedTask)
     */
    public static final class SubmittedTask<V> {
        private final AtomicReference<CancelableFunction<? extends V>> functionRef;
        private final CompletableFuture<V> future;

        /**
         * Creates a new {@code SubmittedTask} with the given underlying function.
         *
         * @param function the actual function to be calculated. This argument cannot be
         *   {@code null}.
         */
        public SubmittedTask(CancelableFunction<? extends V> function) {
            this.functionRef = new AtomicReference<>(Objects.requireNonNull(function, "function"));
            this.future = new CompletableFuture<>();
        }

        /**
         * Executes the submitted tasks and completes this {@code SubmittedTask}. This method
         * never throws an exception. If the submitted task fails, the task will be completed
         * exceptionally.
         *
         * @param cancelToken the {@code CancellationToken} passed to the submitted task.
         *   This argument cannot be {@code null}.
         */
        public void execute(CancellationToken cancelToken) {
            CancelableFunction<? extends V> currentFunction = functionRef.getAndSet(null);
            if (currentFunction == null) {
                return;
            }

            CancelableTasks.complete(cancelToken, currentFunction, future);
        }

        /**
         * Returns the future of the submitted task.
         *
         * @return the future of the submitted task. This method never returns {@code null}.
         */
        public CompletableFuture<V> getFuture() {
            return future;
        }

        /**
         * Completes the submitted task exceptionally with the given exception.
         *
         * @param ex the exception to complete the submitted task with. This argument
         *   cannot be {@code null}.
         */
        public void completeExceptionally(Throwable ex) {
            functionRef.set(null);
            future.completeExceptionally(ex);
        }

        /**
         * Completes the submitted task exceptionally with an {@link OperationCanceledException}.
         */
        public void cancel() {
            completeExceptionally(new OperationCanceledException());
        }
    }
}
