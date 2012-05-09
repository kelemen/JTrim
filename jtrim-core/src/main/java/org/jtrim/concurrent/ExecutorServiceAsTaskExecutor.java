package org.jtrim.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
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
final class ExecutorServiceAsTaskExecutor implements TaskExecutor {
    private static final Logger LOGGER = Logger.getLogger(ExecutorServiceAsTaskExecutor.class.getName());

    private final ExecutorService executor;

    public ExecutorServiceAsTaskExecutor(ExecutorService executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        this.executor = executor;
    }

    private void executeWithoutCleanup(
            final CancellationToken cancelToken,
            final CancelableTask task) {

        final AtomicBoolean executed = new AtomicBoolean(false);
        final AtomicReference<ListenerRef> listenerRefRef = new AtomicReference<>(null);
        final Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    task.execute(cancelToken);
                } finally {
                    executed.set(true);
                    ListenerRef ref = listenerRefRef.getAndSet(null);
                    if (ref != null) {
                        ref.unregister();
                    }
                }
            }
        });
        if (future != null) {
            ListenerRef listenerRef = cancelToken.addCancellationListener(new Runnable() {
                @Override
                public void run() {
                    future.cancel(true);
                }
            });
            listenerRefRef.set(listenerRef);
            if (executed.get()) {
                listenerRef.unregister();
            }
        }
    }

    private void executeWithCleanup(
            final CancellationToken cancelToken,
            final CancelableTask task,
            final CleanupTask cleanupTask) {

        executor.execute(new ExecuteWithCleanupTask(cancelToken, task, cleanupTask));
    }

    @Override
    public void execute(
            final CancellationToken cancelToken,
            final CancelableTask task,
            final CleanupTask cleanupTask) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");

        if (cleanupTask == null) {
            executeWithoutCleanup(cancelToken, task);
        }
        else {
            executeWithCleanup(cancelToken, task, cleanupTask);
        }
    }

    private static class ExecuteWithCleanupTask implements Runnable {
        private final AtomicReference<CancelableTask> taskRef;
        private final CancellationToken cancelToken;
        private final ListenerRef listenerRef;
        private final CleanupTask cleanupTask;

        public ExecuteWithCleanupTask(
                CancellationToken cancelToken,
                CancelableTask task,
                CleanupTask cleanupTask) {
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
                                "A cleanup task has thrown an exception",
                                ex);
                    }
                }
            }
        }
    }
}
