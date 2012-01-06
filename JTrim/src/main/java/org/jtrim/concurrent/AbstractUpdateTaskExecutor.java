/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public abstract class AbstractUpdateTaskExecutor implements UpdateTaskExecutor {
    private volatile boolean stopped;
    private final AtomicReference<TaskExecutor> executorTask;
    private final AtomicReference<Runnable> currentTask;

    public AbstractUpdateTaskExecutor() {
        this.stopped = false;
        this.executorTask = new AtomicReference<>(null);
        this.currentTask = new AtomicReference<>(null);
    }

    protected abstract void runTask(Runnable task);

    @Override
    public void execute(Runnable task) {
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

            runTask(newExecutorTask);

            // This is not important, just another effort
            // trying to cancel the current task.
            if (stopped) {
                newExecutorTask.cancel();
            }
        }
    }

    @Override
    public void shutdown() {
        stopped = true;

        TaskExecutor executor = executorTask.get();
        if (executor != null) {
            executor.cancel();
        }
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
