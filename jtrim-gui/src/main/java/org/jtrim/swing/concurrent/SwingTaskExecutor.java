package org.jtrim.swing.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class SwingTaskExecutor extends DelegatedTaskExecutorService {
    private static volatile TaskExecutorService defaultInstance
            = TaskExecutors.asUnstoppableExecutor(new SwingTaskExecutor());

    public static TaskExecutorService getDefaultInstance() {
        return defaultInstance;
    }

    public static TaskExecutor getSimpleExecutor(boolean alwaysInvokeLater) {
        return alwaysInvokeLater
                ? LazyExecutor.INSTANCE
                : EagerExecutor.INSTANCE;
    }

    public static TaskExecutor getStrictExecutor(boolean alwaysInvokeLater) {
        // We silently assume that SwingUtilities.invokeLater
        // invokes the tasks in the order they were scheduled.
        // This is not documented but it still seems safe to assume.
        return alwaysInvokeLater
                ? LazyExecutor.INSTANCE
                : new StrictEagerExecutor();
    }

    private enum LazyExecutor implements TaskExecutor {
        INSTANCE;

        @Override
        public void execute(
                final CancellationToken cancelToken,
                final CancelableTask task,
                final CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
                    executor.execute(cancelToken, task, cleanupTask);
                }
            });
        }
    }

    private enum EagerExecutor implements TaskExecutor {
        INSTANCE;

        @Override
        public void execute(
                CancellationToken cancelToken,
                CancelableTask task,
                CleanupTask cleanupTask) {

            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            if (SwingUtilities.isEventDispatchThread()) {
                TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
                executor.execute(cancelToken, task, cleanupTask);
            }
            else {
                LazyExecutor.INSTANCE.execute(cancelToken, task, cleanupTask);
            }
        }
    }

    private static class StrictEagerExecutor implements TaskExecutor {
        // We assume that there are always less than Integer.MAX_VALUE
        // concurrent tasks.
        // Having more than this would surely make the application unusable
        // anyway (since these tasks run on the single Event Dispatch Thread,
        // this would make it more outrageous).
        private final AtomicInteger currentlyExecuting = new AtomicInteger(0);

        @Override
        public void execute(
                final CancellationToken cancelToken,
                final CancelableTask task,
                final CleanupTask cleanupTask) {
            ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
            ExceptionHelper.checkNotNullArgument(task, "task");

            boolean canInvokeNow = currentlyExecuting.get() == 0;
            // Tasks that are scheduled concurrently this call,
            // does not matter if they run after this task.
            // This executor only guarantees that A task happens before
            // B task, if scheduling A happens before scheduling B
            // (which implies that they does not run concurrently).

            if (canInvokeNow && SwingUtilities.isEventDispatchThread()) {
                TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
                executor.execute(cancelToken, task, cleanupTask);
            }
            else {
                currentlyExecuting.incrementAndGet();

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TaskExecutor executor = SyncTaskExecutor.getSimpleExecutor();
                            executor.execute(cancelToken, task, cleanupTask);
                        } finally {
                            currentlyExecuting.decrementAndGet();
                        }
                    }
                });
            }
        }
    }

    public SwingTaskExecutor() {
        this(true);
    }

    public SwingTaskExecutor(boolean alwaysInvokeLater) {
        super(TaskExecutors.upgradeExecutor(getStrictExecutor(alwaysInvokeLater)));
    }

    private static void checkWaitOnEDT() {
        if (SwingUtilities.isEventDispatchThread()) {
            // Waiting on the EDT would be a good way to cause a dead-lock.
            throw new IllegalStateException("Cannot wait for termination on the Event Dispatch Thread.");
        }
    }

    @Override
    public void awaitTermination(CancellationToken cancelToken) {
        checkWaitOnEDT();
        wrappedExecutor.awaitTermination(cancelToken);
    }

    @Override
    public boolean awaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        checkWaitOnEDT();
        return wrappedExecutor.awaitTermination(cancelToken, timeout, unit);
    }
}