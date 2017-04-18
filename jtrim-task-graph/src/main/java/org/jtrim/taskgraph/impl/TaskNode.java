package org.jtrim.taskgraph.impl;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.taskgraph.TaskErrorHandler;
import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.utils.ExceptionHelper;

public final class TaskNode<R, I> {
    private final TaskNodeKey<R, I> key;
    private final AtomicReference<NodeTaskRef<R>> nodeTaskRefRef;

    private final CompletableFuture<R> taskFuture;

    public TaskNode(TaskNodeKey<R, I> key, NodeTaskRef<R> nodeTask) {
        ExceptionHelper.checkNotNullArgument(key, "key");
        ExceptionHelper.checkNotNullArgument(nodeTask, "nodeTask");

        this.key = key;
        this.nodeTaskRefRef = new AtomicReference<>(nodeTask);
        this.taskFuture = new CompletableFuture<>();
    }

    public TaskNodeKey<R, I> getKey() {
        return key;
    }

    public CompletableFuture<R> taskFuture() {
        return taskFuture;
    }

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

            compute(cancelToken, nodeTaskRef, (canceled, error) -> {
                completeTask(canceled, error);
                if (isError(canceled, error)) {
                    errorHandler.onError(key, error);
                }
            });
        } catch (Throwable ex) {
            propagateFailure(ex);
            errorHandler.onError(key, ex);
            throw ex;
        }
    }

    private void completeTask(boolean canceled, Throwable error) {
        if (error != null) {
            propagateFailure(error);
        }
        else if (canceled) {
            cancel();
        }
        else if (!taskFuture.isDone()) {
            // This should never happen with a properly implemented executor.
            propagateFailure(new IllegalStateException("Completed with unknown error."));
        }
    }

    private void compute(CancellationToken cancelToken, NodeTaskRef<R> nodeTaskRef, CleanupTask cleanup) {
        TaskExecutor executor = nodeTaskRef.getProperties().getExecutor();
        executor.execute(cancelToken, (CancellationToken taskCancelToken) -> {
            R result = nodeTaskRef.compute(taskCancelToken);
            taskFuture.complete(result);
        }, cleanup);
    }

    public void cancel() {
        nodeTaskRefRef.set(null);
        taskFuture.completeExceptionally(new OperationCanceledException());
    }

    private void propagateFailure(Throwable error) {
        taskFuture.completeExceptionally(error);
    }

    public boolean hasResult() {
        return taskFuture.isDone() && !taskFuture.isCompletedExceptionally();
    }

    public R getResult() {
        if (!taskFuture.isDone()) {
            throw new IllegalStateException("Trying to retrieve result of node before computation: " + key);
        }

        try {
            return taskFuture.getNow(null);
        } catch (CancellationException ex) {
            throw new OperationCanceledException(ex);
        }
    }

    private static boolean isError(boolean canceled, Throwable error) {
        if (canceled && (error instanceof OperationCanceledException)) {
            return false;
        }
        return error != null;
    }
}
