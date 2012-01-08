package org.jtrim.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jtrim.utils.*;

/**
 *
 * @author Kelemen Attila
 */
public final class GenericUpdateTaskExecutor implements UpdateTaskExecutor {
    private volatile boolean stopped;
    private final AtomicReference<TaskExecutor> executorTask;
    private final AtomicReference<Runnable> currentTask;
    private final Executor taskExecutor;

    public GenericUpdateTaskExecutor(Executor taskExecutor) {
        ExceptionHelper.checkNotNullArgument(taskExecutor, "taskExecutor");

        this.stopped = false;
        this.executorTask = new AtomicReference<>(null);
        this.currentTask = new AtomicReference<>(null);
        this.taskExecutor = taskExecutor;
    }

    @Override
    public final void execute(Runnable task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        if (stopped) {
            return;
        }

        Runnable oldTask = currentTask.getAndSet(task);
        if (oldTask == null) {
            // Notice that if oldTask is null,
            // executorTask can be considered null as well.
            // (if it is not null, it just started executing a task)
            TaskExecutor newExecutorTask = new TaskExecutor();
            executorTask.set(newExecutorTask);

            taskExecutor.execute(newExecutorTask);

            // This is not important, just another effort
            // trying to cancel the current task.
            if (stopped) {
                newExecutorTask.cancel();
            }
        }
    }

    @Override
    public final void shutdown() {
        stopped = true;

        // It is only a best effor cancel.
        TaskExecutor executor = executorTask.get();
        if (executor != null) {
            executor.cancel();
        }
    }

    @Override
    public String toString() {
        return "GenericUpdateTaskExecutor{" + taskExecutor + '}';
    }

    private class TaskExecutor implements Runnable {
        private volatile boolean canceled;

        public TaskExecutor() {
            this.canceled = false;
        }

        @Override
        public void run() {
            Runnable task = currentTask.getAndSet(null);

            // If executorTask does not hold "this"
            // a new task was scheduled already, right after
            // the "currentTask.getAndSet" call.
            executorTask.compareAndSet(this, null);

            if (task != null && !canceled) {
                task.run();
            }
        }

        public void cancel() {
            canceled = true;
        }
    }
}
