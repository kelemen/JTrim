package org.jtrim2.executor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;

final class SyncNonRecursiveExecutor implements TaskExecutor {
    private final ThreadLocal<Deque<Runnable>> taskQueueRef;

    public SyncNonRecursiveExecutor() {
        this.taskQueueRef = new ThreadLocal<>();
    }

    @Override
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {

        CompletableFuture<V> future = new CompletableFuture<>();
        execute(() -> CancelableTasks.complete(cancelToken, function, future));
        return future;
    }

    @Override
    public void execute(Runnable command) {
        Deque<Runnable> taskQueue = taskQueueRef.get();
        if (taskQueue != null) {
            taskQueue.addLast(command);
            return;
        }

        taskQueue = new ArrayDeque<>();
        taskQueueRef.set(taskQueue);

        try {
            CancelableTasks.executeAndLogError(command);
            for (Runnable nextTask = taskQueue.pollFirst(); nextTask != null; nextTask = taskQueue.pollFirst()) {
                nextTask.run();
            }
        } finally {
            taskQueueRef.remove();
        }
    }
}
