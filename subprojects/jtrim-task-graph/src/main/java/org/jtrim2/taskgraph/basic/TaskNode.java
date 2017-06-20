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
import org.jtrim2.taskgraph.TaskErrorHandler;
import org.jtrim2.taskgraph.TaskNodeKey;

/**
 * Defines a task node which can be computed once. The task node can also be marked
 * as finished externally.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are allowed to be used by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
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
            if (AsyncTasks.isCanceled(ex.getCause())) {
                throw new OperationCanceledException();
            }
            throw ex;
        }
    }
}
