package org.jtrim2.executor;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;

/**
 * @see TaskExecutors#debugExecutor(TaskExecutor)
 *
 * @author Kelemen Attila
 */
final class DebugTaskExecutor implements TaskExecutor {
    private static final Logger LOGGER = Logger.getLogger(DebugTaskExecutor.class.getName());

    private final TaskExecutor wrappedExecutor;

    public DebugTaskExecutor(TaskExecutor wrappedExecutor) {
        Objects.requireNonNull(wrappedExecutor, "wrappedExecutor");
        this.wrappedExecutor = wrappedExecutor;
    }

    @Override
    public void execute(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
        wrappedExecutor.execute(cancelToken, new DebugTaskWrapper(task), cleanupTask);
    }

    private static final class DebugTaskWrapper implements CancelableTask {
        private final CancelableTask task;

        public DebugTaskWrapper(CancelableTask task) {
            Objects.requireNonNull(task, "task");
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
