package org.jtrim.concurrent;

import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see TaskExecutors#contextAware(TaskExecutor)
 * @author Kelemen Attila
 */
final class ContextAwareWrapper implements ContextAwareTaskExecutor {
    private final TaskExecutor wrapped;
    // null if not in context, running in context otherwise.
    private final ThreadLocal<Object> inContext;

    public ContextAwareWrapper(TaskExecutor wrapped) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");
        this.wrapped = wrapped;
        this.inContext = new ThreadLocal<>();
    }

    @Override
    public boolean isExecutingInThis() {
        Object result = inContext.get();
        if (result == null) {
            inContext.remove();
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    public void execute(
            CancellationToken cancelToken,
            final CancelableTask task,
            final CleanupTask cleanupTask) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkNotNullArgument(task, "task");

        CancelableTask contextTask = new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                Object prevValue = inContext.get();
                try {
                    if (prevValue == null) {
                        inContext.set(true);
                    }
                    task.execute(cancelToken);
                } finally {
                    if (prevValue == null) {
                        inContext.remove();
                    }
                }
            }
        };

        if (cleanupTask != null) {
            CleanupTask contextCleanup = new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) throws Exception {
                    Object prevValue = inContext.get();
                    try {
                        if (prevValue == null) {
                            inContext.set(true);
                        }
                        cleanupTask.cleanup(canceled, error);
                    } finally {
                        if (prevValue == null) {
                            inContext.remove();
                        }
                    }
                }
            };
            wrapped.execute(cancelToken, contextTask, contextCleanup);
        }
        else {
            wrapped.execute(cancelToken, contextTask, null);
        }
    }
}
