package org.jtrim2.executor;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;

/**
 * @see TaskExecutors#debugExecutorService(TaskExecutorService)
 */
final class DebugTaskExecutorService extends DelegatedTaskExecutorService {
    private static final Logger LOGGER = Logger.getLogger(DebugTaskExecutorService.class.getName());

    public DebugTaskExecutorService(TaskExecutorService wrappedExecutor) {
        super(wrappedExecutor);
    }

    @Override
    public TaskFuture<?> submit(
            CancellationToken cancelToken,
            final CancelableTask task,
            CleanupTask cleanupTask) {
        return super.submit(cancelToken, new DebugTaskWrapper(task), cleanupTask);
    }

    @Override
    public <V> TaskFuture<V> submit(
            CancellationToken cancelToken,
            CancelableFunction<V> task,
            CleanupTask cleanupTask) {
        return super.submit(cancelToken, new DebugFunctionWrapper<>(task), cleanupTask);
    }

    @Override
    public void execute(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
        super.execute(cancelToken, new DebugTaskWrapper(task), cleanupTask);
    }

    private static final class DebugFunctionWrapper<V> implements CancelableFunction<V> {
        private final CancelableFunction<V> task;

        public DebugFunctionWrapper(CancelableFunction<V> task) {
            Objects.requireNonNull(task, "task");
            this.task = task;
        }

        @Override
        public V execute(CancellationToken cancelToken) throws Exception {
            try {
                return task.execute(cancelToken);
            } catch (OperationCanceledException ex) {
                throw ex;
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Uncaught exception in a task: " + task, ex);
                throw ex;
            }
        }
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
