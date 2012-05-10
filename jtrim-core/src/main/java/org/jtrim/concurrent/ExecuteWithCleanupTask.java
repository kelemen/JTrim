/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class ExecuteWithCleanupTask implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ExecuteWithCleanupTask.class.getName());

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
        Throwable error = null;
        boolean canceled = true;
        try {
            CancelableTask task = taskRef.getAndSet(null);
            if (task != null) {
                task.execute(cancelToken);
                canceled = false;
            }
        } catch (OperationCanceledException ex) {
            error = ex;
        } catch (Throwable ex) {
            error = ex;
            canceled = false;
        } finally {
            try {
                listenerRef.unregister();
            } finally {
                try {
                    cleanupTask.cleanup(canceled, error);
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE,
                            "A cleanup task has thrown an exception", ex);
                }
            }
        }
    }

}
