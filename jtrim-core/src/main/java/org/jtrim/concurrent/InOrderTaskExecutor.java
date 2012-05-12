package org.jtrim.concurrent;

import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class InOrderTaskExecutor implements TaskExecutor {
    // FIXME: This implementation should not rely on TaskScheduler
    //        because TaskScheduler knows nothing about cancellation and will
    //        keep referencing the submitted task even if it has been canceled.

    private final TaskExecutor executor;
    private final DispatchTask dispatchTask;
    private final TaskScheduler taskScheduler;

    public InOrderTaskExecutor(TaskExecutor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        this.executor = executor;
        this.taskScheduler
                = new TaskScheduler(SyncTaskExecutor.getSimpleExecutor());
        this.dispatchTask = new DispatchTask(taskScheduler);
    }

    @Override
    public void execute(
            final CancellationToken cancelToken,
            final CancelableTask task,
            final CleanupTask cleanupTask) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");

        taskScheduler.scheduleTask(new Runnable() {
            @Override
            public void run() {
                Tasks.executeTaskWithCleanup(cancelToken, task, cleanupTask);
            }
        });
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, dispatchTask, null);
    }

    private static class DispatchTask implements CancelableTask {
        private final TaskScheduler taskScheduler;

        public DispatchTask(TaskScheduler taskScheduler) {
            this.taskScheduler = taskScheduler;
        }

        @Override
        public void execute(CancellationToken cancelToken) {
            taskScheduler.dispatchTasks();
        }
    }
}
