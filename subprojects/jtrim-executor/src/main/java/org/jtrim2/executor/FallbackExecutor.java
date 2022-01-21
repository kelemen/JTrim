package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;

final class FallbackExecutor extends DelegatedTaskExecutorService implements MonitorableTaskExecutorService {
    private final MonitorableTaskExecutorService monitorableWrappedExecutor;

    public FallbackExecutor(MonitorableTaskExecutorService wrappedExecutor) {
        super(wrappedExecutor);
        this.monitorableWrappedExecutor = wrappedExecutor;
    }

    @Override
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {

        try {
            return wrappedExecutor.executeFunction(cancelToken, function);
        } catch (FallbackException ex) {
            return ex.fallback(wrappedExecutor).executeFunction(cancelToken, function);
        }
    }

    @Override
    public CompletionStage<Void> execute(CancellationToken cancelToken, CancelableTask task) {
        try {
            return wrappedExecutor.execute(cancelToken, task);
        } catch (FallbackException ex) {
            return ex.fallback(wrappedExecutor).execute(cancelToken, task);
        }
    }

    @Override
    public void execute(Runnable command) {
        try {
            wrappedExecutor.execute(command);
        } catch (FallbackException ex) {
            ex.fallback(wrappedExecutor).execute(command);
        }
    }

    @Override
    public CompletionStage<Void> executeStaged(Runnable task) {
        try {
            return wrappedExecutor.executeStaged(task);
        } catch (FallbackException ex) {
            return ex.fallback(wrappedExecutor).executeStaged(task);
        }
    }

    @Override
    public boolean isExecutingInThis() {
        return monitorableWrappedExecutor.isExecutingInThis();
    }

    @Override
    public long getNumberOfQueuedTasks() {
        return monitorableWrappedExecutor.getNumberOfQueuedTasks();
    }

    @Override
    public long getNumberOfExecutingTasks() {
        return monitorableWrappedExecutor.getNumberOfExecutingTasks();
    }

    public static class FallbackException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final TaskExecutor failingExecutor;
        private final TaskExecutor fallbackExecutor;

        public FallbackException(TaskExecutor failingExecutor, TaskExecutor fallbackExecutor) {
            super("fallback", null, false, false);

            this.failingExecutor = Objects.requireNonNull(failingExecutor, "failingExecutor");
            this.fallbackExecutor = Objects.requireNonNull(fallbackExecutor, "fallbackExecutor");
        }

        public TaskExecutor fallback(TaskExecutor wrappedExecutor) {
            if (wrappedExecutor != failingExecutor) {
                throw this;
            }
            return fallbackExecutor;
        }
    }
}
