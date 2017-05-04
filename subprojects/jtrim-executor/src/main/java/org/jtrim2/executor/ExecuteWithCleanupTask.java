package org.jtrim2.executor;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class ExecuteWithCleanupTask implements Runnable {
    private final AtomicReference<CancelableTask> taskRef;
    private final boolean mayInterruptTask;
    private final CancellationToken cancelToken;
    private final ListenerRef listenerRef;
    private final CleanupTask cleanupTask;

    public ExecuteWithCleanupTask(
            boolean mayInterruptTask,
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");

        this.mayInterruptTask = mayInterruptTask;
        this.cancelToken = cancelToken;
        this.cleanupTask = cleanupTask;
        this.taskRef = new AtomicReference<>(task);
        this.listenerRef = cancelToken.addCancellationListener(() -> {
            taskRef.set(null);
        });
    }

    private void doTask() {
        CancelableTasks.executeTaskWithCleanup(cancelToken, taskRef.getAndSet(null), cleanupTask);
    }

    @Override
    public void run() {
        try {
            if (mayInterruptTask) {
                Cancellation.doAsCancelable(cancelToken, (CancellationToken taskCancelToken) -> {
                    doTask();
                    return null;
                });
            }
            else {
                doTask();
            }
        } finally {
            listenerRef.unregister();
        }
    }

}
