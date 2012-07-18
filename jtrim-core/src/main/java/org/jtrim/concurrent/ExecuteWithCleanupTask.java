package org.jtrim.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.InterruptibleTask;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

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
        this.listenerRef = cancelToken.addCancellationListener(new Runnable() {
            @Override
            public void run() {
                taskRef.set(null);
            }
        });
    }

    private void doTask() {
        Tasks.executeTaskWithCleanup(cancelToken, taskRef.getAndSet(null), cleanupTask);
    }

    @Override
    public void run() {
        try {
            if (mayInterruptTask) {
                Cancellation.doAsCancelable(cancelToken, new InterruptibleTask<Void>() {
                    @Override
                    public Void execute(CancellationToken cancelToken) {
                        doTask();
                        return null;
                    }
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
