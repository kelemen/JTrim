package org.jtrim2.taskgraph.basic;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.taskgraph.DependencyErrorHandler;
import org.jtrim2.taskgraph.TaskErrorHandler;
import org.jtrim2.taskgraph.TaskNodeKey;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines a task node which can be computed once. The task node can also be marked
 * as finished externally.
 *
 * <h2>Thread safety</h2>
 * Methods of this class are allowed to be used by multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this interface are not <I>synchronization transparent</I> unless otherwise
 * noted.
 *
 * @param <R> the return type of the task nodes created by the defined task node factory
 * @param <I> the type of the argument passed to the task node factory when requested for
 *   a node to be created
 *
 * @see CollectingTaskGraphBuilder
 * @see TaskGraphExecutorFactory
 */
public final class TaskNode<R, I> {
    private final TaskNodeKey<R, I> key;
    private final AtomicReference<NodeTaskRef<R>> nodeTaskRefRef;
    private volatile boolean scheduled;

    private final CompletableFuture<R> taskFuture;

    /**
     * Creates a new {@code TaskNode} with the given node key and task.
     *
     * @param key the key uniquely identifying this node in the task graph.
     *   This argument cannot be {@code null}.
     * @param nodeTask the task of the action with the properties of the task node.
     *   This argument cannot be {@code null}.
     */
    public TaskNode(TaskNodeKey<R, I> key, NodeTaskRef<R> nodeTask) {
        this(key, nodeTask, new CompletableFuture<>());
    }

    /**
     * Creates a new {@code TaskNode} with the given node key and task.
     *
     * @param key the key uniquely identifying this node in the task graph.
     *   This argument cannot be {@code null}.
     * @param nodeTask the task of the action with the properties of the task node.
     *   This argument cannot be {@code null}.
     * @param taskFuture the {@code CompletableFuture} for which the output of this
     *   task will be set once, it completes. This argument cannot be {@code null}.
     */
    public TaskNode(TaskNodeKey<R, I> key, NodeTaskRef<R> nodeTask, CompletableFuture<R> taskFuture) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(nodeTask, "nodeTask");
        Objects.requireNonNull(taskFuture, "taskFuture");

        this.key = key;
        this.nodeTaskRefRef = new AtomicReference<>(nodeTask);
        this.taskFuture = taskFuture;
        this.scheduled = false;
    }

    /**
     * Returns the {@code TaskNodeKey} uniquely identifying this task in the task graph.
     * <P>
     * This method is <I>synchronization transparent</I>.
     *
     * @return the {@code TaskNodeKey} uniquely identifying this task in the task graph.
     *   This method never returns {@code null}.
     */
    public TaskNodeKey<R, I> getKey() {
        return key;
    }

    /**
     * Returns the {@code CompletableFuture} holding the result of the output of
     * this task node.
     * <P>
     * Note: It is preferable to call the {@link #cancel() cancel} or the
     * {@link #propagateFailure(Throwable) propagateFailure} method to directly completing
     * the returned {@code CompletableFuture} because the mentioned methods will also
     * remove the reference to the actual task to be executed.
     *
     * @return the {@code CompletableFuture} holding the result of the output of
     *   this task node. This method never returns {@code null}.
     */
    public CompletableFuture<R> taskFuture() {
        return taskFuture;
    }

    /**
     * Returns {@code true} if this task properly scheduled. The return value is only meaningful
     * after the completion of this task node and can be used to detect if the node was scheduled
     * or was completed externally.
     *
     * @return {@code true} if this task properly scheduled, {@code false} otherwise
     */
    public boolean wasScheduled() {
        return scheduled;
    }

    /**
     * Schedules this task node for computation if it was not scheduled yet. This
     * method is idempotent: That is, once it has been called, subsequent calls do
     * nothing.
     *
     * @param cancelToken the {@code CancellationToken} which can signal that a task
     *   execution is to be canceled. There is no guarantee that the cancellation
     *   will not be ignored. This argument cannot be {@code null}.
     */
    public void ensureScheduleComputed(CancellationToken cancelToken) {
        ensureScheduleComputed(cancelToken, (nodeKey, error) -> { });
    }

    /**
     * Schedules this task node for computation if it was not scheduled yet. This
     * method is idempotent: That is, once it has been called, subsequent calls do
     * nothing.
     *
     * @param cancelToken the {@code CancellationToken} which can signal that a task
     *   execution is to be canceled. There is no guarantee that the cancellation
     *   will not be ignored. This argument cannot be {@code null}.
     * @param errorHandler the callback to be notified in case the task encounters an error.
     *   This argument cannot be {@code null}.
     */
    public void ensureScheduleComputed(CancellationToken cancelToken, TaskErrorHandler errorHandler) {
        NodeTaskRef<R> nodeTaskRef = nodeTaskRefRef.getAndSet(null);
        if (nodeTaskRef == null) {
            return;
        }

        try {
            scheduled = true;

            if (cancelToken.isCanceled()) {
                cancel();
                return;
            }

            compute(cancelToken, nodeTaskRef).whenComplete((result, error) -> {
                completeTask(error);
                if (AsyncTasks.isError(error)) {
                    errorHandler.onError(key, error);
                }
            });
        } catch (Throwable ex) {
            propagateFailure(ex);
            errorHandler.onError(key, ex);
            throw ex;
        }
    }

    private void completeTask(Throwable error) {
        if (error != null) {
            propagateFailure(error);
        } else if (!taskFuture.isDone()) {
            // This should never happen with a properly implemented executor.
            propagateFailure(new IllegalStateException("Completed with unknown error."));
        }
    }

    private CompletionStage<Void> compute(CancellationToken cancelToken, NodeTaskRef<R> nodeTaskRef) {
        TaskExecutor executor = nodeTaskRef.getProperties().getExecutor();
        return executor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
            R result = nodeTaskRef.compute(taskCancelToken);
            taskFuture.complete(result);
        });
    }

    /**
     * Cancels the computation of this node if it was not computed yet.
     * If this task node was already completed, this method does nothing.
     */
    public void cancel() {
        propagateFailure(OperationCanceledException.withoutStackTrace());
    }

    /**
     * Completes this task node exceptionally but calling the
     * {@link org.jtrim2.taskgraph.TaskNodeProperties dependency error handler} first (if there is any).
     * If this task node was already scheduled for execution normally, this method does nothing.
     * <P>
     * Note that cancellation affects the dependency error handler as well. That is, if execution
     * was canceled, the dependency error handler might not get executed.
     * <P>
     * Calling this method does not count as scheduled for the {@link #wasScheduled() wasScheduled} flag.
     *
     * @param cancelToken the cancellation token which can signal cancellation for the
     *   dependency error handler. This argument cannot be {@code null}.
     * @param error the error to forward to complete this node with. This argument cannot be
     *   {@code null}. This argument cannot be {@code null}.
     */
    @SuppressWarnings("ThrowableResultIgnored")
    public void propagateDependencyFailure(CancellationToken cancelToken, Throwable error) {
        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(error, "error");

        NodeTaskRef<R> nodeTaskRef = nodeTaskRefRef.getAndSet(null);
        if (nodeTaskRef == null) {
            // The task was already scheduled so, we ignore dependency failure notification.
            // Also, this should not happen when used reasonably.
            return;
        }

        DependencyErrorHandler errorHandler = nodeTaskRef.getProperties().tryGetDependencyErrorHandler();
        if (errorHandler == null) {
            propagateFailure(error);
            return;
        }

        try {
            if (cancelToken.isCanceled()) {
                cancel();
                return;
            }

            TaskExecutor executor = nodeTaskRef.getProperties().getExecutor();
            executor.execute(cancelToken, taskCancelToken -> {
                errorHandler.handleDependencyError(taskCancelToken, key, error);
            }).whenComplete((result, taskError) -> {
                propagateSuppressed(error, taskError);
            });
        } catch (Throwable ex) {
            propagateSuppressed(error, ex);
            throw ex;
        }
    }

    private void propagateSuppressed(Throwable error, Throwable suppressed) {
        try {
            if (suppressed != null && suppressed != error) {
                error.addSuppressed(suppressed);
            }
        } finally {
            propagateFailure(error);
        }
    }

    /**
     * Completes this task node exceptionally with the given error if it was not
     * completed yet. If this task node was already completed, this method does nothing.
     *
     * @param error the error with which this task node is to be completed with.
     *   This argument cannot be {@code null}.
     */
    public void propagateFailure(Throwable error) {
        nodeTaskRefRef.set(null);
        taskFuture.completeExceptionally(error);
    }

    /**
     * Returns {@code true} if this task node has already completed successfully
     * and has its result set. That is, if this method returns {@code true},
     * subsequent calls to {@link #getResult() } will succeed without throwing an
     * exception.
     *
     * @return {@code true} if this task node has already completed successfully
     *   and has its result set, {@code false} otherwise
     */
    public boolean hasResult() {
        return taskFuture.isDone() && !taskFuture.isCompletedExceptionally();
    }

    /**
     * Returns the output of this node or throws an exception if it was completed exceptionally.
     * <P>
     * This method may only be called after it is known that this task node has been completed.
     *
     * @return the output of this node or throws an exception if it was completed exceptionally.
     *   This method may return {@code null}, if the task's output was {@code null}.
     *
     * @throws java.util.concurrent.CompletionException thrown if this task node has been completed exceptionally
     *   (except if it was completed with {@code CancellationException}
     *   or {@code OperationCanceledException}. The cause of the {@code CompletionException} contains the exception
     *   thrown during the computation.
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if computation was canceled
     *   before this node could have been computed
     * @throws IllegalStateException thrown if this task node has not been completed yet
     */
    public R getResult() {
        return getExpectedResultNow(key, taskFuture);
    }

    /**
     * Returns the output from the given {@code CompletableFuture} translating
     * {@code CancellationException} to {@code OperationCanceledException}.
     * <P>
     * This method differs from {@link #getResultNow(CompletableFuture) getResultNow} from throwing
     * an {@code IllegalStateException} if the given {@code CompletableFuture} has not been completed yet.
     *
     * @param <R> the type of the result of the computation associated with the given
     *   {@code CompletableFuture}
     * @param key the key to be added to the error message of the possible {@code IllegalStateException}.
     * @param future the {@code CompletableFuture}.
     * @return the result of with which the given {@code CompletableFuture} has been completed with.
     *   This method may return {@code null}, if the {@code CompletableFuture} was completed normally
     *   with {@code null}.
     *
     * @throws java.util.concurrent.CompletionException thrown if the given {@code CompletableFuture}
     *   has been completed exceptionally (except if it was completed with {@code CancellationException}
     *   or {@code OperationCanceledException}. The cause of the {@code CompletionException} contains the exception
     *   thrown during the computation.
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if the given {@code CompletableFuture}
     *   was completed with a {@code CancellationException} or an {@code OperationCanceledException}
     * @throws IllegalStateException if the given {@code CompletableFuture} has not been completed yet
     */
    public static <R> R getExpectedResultNow(TaskNodeKey<?, ?> key, CompletableFuture<? extends R> future) {
        if (!future.isDone()) {
            throw new IllegalStateException("Trying to retrieve result of node before computation: " + key);
        }

        return getResultNow(future);
    }

    /**
     * Returns the output from the given {@code CompletableFuture} translating
     * {@code CancellationException} to {@code OperationCanceledException}.
     *
     * @param <R> the type of the result of the computation associated with the given
     *   {@code CompletableFuture}
     * @param future the {@code CompletableFuture}.
     * @return the result of with which the given {@code CompletableFuture} has been completed with
     *   or {@code null} if the given {@code CompletableFuture} has been completed yet
     *
     * @throws java.util.concurrent.CompletionException thrown if the given {@code CompletableFuture}
     *   has been completed exceptionally (except if it was completed with {@code CancellationException}
     *   or {@code OperationCanceledException}. The cause of the {@code CompletionException} contains the exception
     *   thrown during the computation.
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if the given {@code CompletableFuture}
     *   was completed with a {@code CancellationException} or an {@code OperationCanceledException}
     */
    public static <R> R getResultNow(CompletableFuture<? extends R> future) {
        try {
            return future.getNow(null);
        } catch (CancellationException ex) {
            throw new OperationCanceledException(ex);
        } catch (CompletionException ex) {
            if (AsyncTasks.isCanceled(ex)) {
                throw ExceptionHelper.throwUnchecked(AsyncTasks.unwrap(ex));
            }
            throw ex;
        }
    }
}
