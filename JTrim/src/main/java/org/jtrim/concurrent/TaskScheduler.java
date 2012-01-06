/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class TaskScheduler {

    public static TaskScheduler newSyncScheduler() {
        return new TaskScheduler(SyncTaskExecutor.getSimpleExecutor());
    }

    private final Executor executor;
    private final ReentrantLock dispatchLock;
    private final BlockingQueue<Runnable> toDispatch;

    public TaskScheduler(Executor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");

        this.executor = executor;
        this.dispatchLock = new ReentrantLock();
        this.toDispatch = new LinkedBlockingQueue<>();
    }

    public void scheduleTask(Runnable task) {
        ExceptionHelper.checkNotNullArgument(task, "task");

        toDispatch.add(task);
    }

    public void scheduleTasks(List<? extends Runnable> tasks) {
        for (Runnable task: tasks) {
            scheduleTask(task);
        }
    }

    public void dispatchTasks() {
        if (isCurrentThreadDispatching()) {
            // Tasks will be dispatched there.
            return;
        }

        while (!toDispatch.isEmpty()) {
            if (dispatchLock.tryLock()) {
                try {
                    Runnable task = toDispatch.poll();
                    if (task != null) {
                        executor.execute(task);
                    }
                } finally {
                    dispatchLock.unlock();
                }
            }
            else {
                return;
            }
        }
    }

    public boolean isCurrentThreadDispatching() {
        return dispatchLock.isHeldByCurrentThread();
    }
}
