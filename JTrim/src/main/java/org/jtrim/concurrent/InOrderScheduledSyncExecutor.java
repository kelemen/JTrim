/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent;

import java.util.concurrent.Executor;

/**
 *
 * @author Kelemen Attila
 */
public final class InOrderScheduledSyncExecutor implements Executor {
    private final TaskScheduler taskScheduler;

    public InOrderScheduledSyncExecutor() {
        taskScheduler = TaskScheduler.newSyncScheduler();
    }

    @Override
    public void execute(Runnable command) {
        taskScheduler.scheduleTask(command);
        taskScheduler.dispatchTasks();
    }

    public boolean isCurrentThreadExecuting() {
        return taskScheduler.isCurrentThreadDispatching();
    }
}
