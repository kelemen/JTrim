package org.jtrim2.executor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.Executor;

final class SyncNonRecursiveExecutor implements Executor {
    private final ThreadLocal<Deque<Runnable>> taskQueueRef;

    public SyncNonRecursiveExecutor() {
        this.taskQueueRef = new ThreadLocal<>();
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command, "command");

        Deque<Runnable> taskQueue = taskQueueRef.get();
        if (taskQueue == null) {
            taskQueue = new ArrayDeque<>();
            taskQueueRef.set(taskQueue);
            try {
                for (Runnable nextTask = command; nextTask != null; nextTask = taskQueue.pollFirst()) {
                    nextTask.run();
                }
            } finally {
                taskQueueRef.remove();
            }
        } else {
            taskQueue.addLast(command);
        }
    }
}
