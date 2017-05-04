package org.jtrim2.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class ExecutorServiceAsTaskExecutor implements TaskExecutor {
    private final ExecutorService executor;
    private final boolean mayInterruptTasks;

    public ExecutorServiceAsTaskExecutor(ExecutorService executor, boolean mayInterruptTasks) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        this.executor = executor;
        this.mayInterruptTasks = mayInterruptTasks;
    }

    private void executeWithoutCleanup(
            final CancellationToken cancelToken,
            final CancelableTask task) {
        final AtomicBoolean executed = new AtomicBoolean(false);
        final AtomicReference<ListenerRef> listenerRefRef = new AtomicReference<>(null);

        final Future<?> future = executor.submit(() -> {
            try {
                if (!cancelToken.isCanceled()) {
                    task.execute(cancelToken);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new OperationCanceledException(ex);
            } catch (Exception ex) {
                throw ExceptionHelper.throwUnchecked(ex);
            } finally {
                executed.set(true);
                ListenerRef ref = listenerRefRef.getAndSet(null);
                if (ref != null) {
                    ref.unregister();
                }
            }
        });

        if (future != null) {
            ListenerRef listenerRef = cancelToken.addCancellationListener(() -> {
                future.cancel(mayInterruptTasks);
            });
            listenerRefRef.set(listenerRef);
            if (executed.get()) {
                listenerRef.unregister();
            }
        }
    }

    private void executeWithCleanup(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {

        // We must not cancel the future because in that case we would not
        // execute the cleanup task.
        executor.execute(new ExecuteWithCleanupTask(
                mayInterruptTasks, cancelToken, task, cleanupTask));
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
}
