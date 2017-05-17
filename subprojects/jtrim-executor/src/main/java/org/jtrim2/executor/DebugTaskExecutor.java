package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;

/**
 * @see TaskExecutors#debugExecutor(TaskExecutor)
 */
final class DebugTaskExecutor implements TaskExecutor {
    private static final Logger LOGGER = Logger.getLogger(DebugTaskExecutor.class.getName());

    private final TaskExecutor wrappedExecutor;

    public DebugTaskExecutor(TaskExecutor wrappedExecutor) {
        Objects.requireNonNull(wrappedExecutor, "wrappedExecutor");
        this.wrappedExecutor = wrappedExecutor;
    }

    @Override
    public void execute(Runnable command) {
        wrappedExecutor.execute(new DebugRunnableWrapper(command));
    }

    @Override
    public CompletionStage<Void> execute(CancellationToken cancelToken, CancelableTask task) {
        return wrappedExecutor.execute(cancelToken, new DebugTaskWrapper(task));
    }

    @Override
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {
        return wrappedExecutor.executeFunction(cancelToken, new DebugFunctionWrapper<>(function));
    }

    static final class DebugFunctionWrapper<V> implements CancelableFunction<V> {
        private final CancelableFunction<? extends V> function;

        public DebugFunctionWrapper(CancelableFunction<? extends V> function) {
            Objects.requireNonNull(function, "function");
            this.function = function;
        }

        @Override
        public V execute(CancellationToken cancelToken) throws Exception {
            try {
                return function.execute(cancelToken);
            } catch (OperationCanceledException ex) {
                throw ex;
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Uncaught exception in a task: " + function, ex);
                throw ex;
            }
        }
    }

    static final class DebugTaskWrapper implements CancelableTask {
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

    static final class DebugRunnableWrapper implements Runnable {
        private final Runnable task;

        public DebugRunnableWrapper(Runnable task) {
            Objects.requireNonNull(task, "task");
            this.task = task;
        }

        @Override
        public void run() {
            try {
                task.run();
            } catch (OperationCanceledException ex) {
                throw ex;
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Uncaught exception in a task: " + task, ex);
                throw ex;
            }
        }
    }
}
