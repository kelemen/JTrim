package org.jtrim.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class ExecuteWithCleanupTask implements Runnable {
    private final AtomicReference<CancelableTask> taskRef;
    private final CancellationToken cancelToken;
    private final ListenerRef listenerRef;
    private final CleanupTask cleanupTask;

    public ExecuteWithCleanupTask(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");

        this.cancelToken = cancelToken;
        this.cleanupTask = cleanupTask;
        this.taskRef = new AtomicReference<>(task);
        this.listenerRef = cancelToken.addCancellationListener(new Runnable() {
            @Override
            public void run() {
                taskRef.set(null);
            }
        });
    }

    @Override
    public void run() {
        try {
            Tasks.executeTaskWithCleanup(
                    cancelToken,
                    taskRef.getAndSet(null),
                    cleanupTask);
        } finally {
            listenerRef.unregister();
        }
    }

}
