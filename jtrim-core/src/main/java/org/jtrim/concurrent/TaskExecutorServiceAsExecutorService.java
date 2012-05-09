package org.jtrim.concurrent;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see ExecutorConverter#asExecutorService(TaskExecutorService)
 *
 * @author Kelemen Attila
 */
final class TaskExecutorServiceAsExecutorService extends AbstractExecutorService {
    // It is accessed in the factory method, when attempting to convert it back.
    final TaskExecutorService executor;

    public TaskExecutorServiceAsExecutorService(TaskExecutorService executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        this.executor = executor;
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        executor.shutdownAndCancel();
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN, timeout, unit);
    }

    @Override
    public void execute(final Runnable command) {
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                command.run();
            }
        }, null);
    }
}
