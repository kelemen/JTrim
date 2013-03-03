package org.jtrim.concurrent;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see TaskExecutors#debugExecutor(TaskExecutor)
 *
 * @author Kelemen Attila
 */
final class DebugTaskExecutor implements TaskExecutor {
    private static final Logger LOGGER = Logger.getLogger(DebugTaskExecutor.class.getName());

    private final TaskExecutor wrappedExecutor;

    public DebugTaskExecutor(TaskExecutor wrappedExecutor) {
        ExceptionHelper.checkNotNullArgument(wrappedExecutor, "wrappedExecutor");
        this.wrappedExecutor = wrappedExecutor;
    }

    @Override
    public void execute(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
        wrappedExecutor.execute(cancelToken, new DebugTaskWrapper(task), cleanupTask);
    }

    private static final class DebugTaskWrapper implements CancelableTask {
        private final CancelableTask task;

        public DebugTaskWrapper(CancelableTask task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            this.task = task;
        }

        @Override
        public void execute(CancellationToken cancelToken) throws Exception {
            try {
                task.execute(cancelToken);
            } catch (OperationCanceledException ex) {
                throw ex;
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Uncaught exception in a task: " + task, ex);
                throw ex;
            }
        }
    }
}
