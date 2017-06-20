package org.jtrim2.executor;

import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;

/**
 * @see TaskExecutors#debugExecutorService(TaskExecutorService)
 */
final class DebugTaskExecutorService extends DelegatedTaskExecutorService {
    public DebugTaskExecutorService(TaskExecutorService wrappedExecutor) {
        super(wrappedExecutor);
    }

    @Override
    public void execute(Runnable command) {
        wrappedExecutor.execute(new DebugTaskExecutor.DebugRunnableWrapper(command));
    }

    @Override
    public CompletionStage<Void> executeStaged(Runnable task) {
        return wrappedExecutor.executeStaged(new DebugTaskExecutor.DebugRunnableWrapper(task));
    }

    @Override
    public CompletionStage<Void> execute(CancellationToken cancelToken, CancelableTask task) {
        return wrappedExecutor.execute(cancelToken, new DebugTaskExecutor.DebugTaskWrapper(task));
    }

    @Override
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {
        return wrappedExecutor.executeFunction(cancelToken, new DebugTaskExecutor.DebugFunctionWrapper<>(function));
    }
}
