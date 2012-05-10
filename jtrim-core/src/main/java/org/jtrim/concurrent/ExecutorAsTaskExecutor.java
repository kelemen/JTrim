package org.jtrim.concurrent;

import java.util.concurrent.Executor;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see ExecutorConverter#asTaskExecutor(Executor)
 *
 * @author Kelemen Attila
 */
final class ExecutorAsTaskExecutor implements TaskExecutor {
    // It is accessed in the factory method, when attempting to convert it back.
    final Executor executor;

    public ExecutorAsTaskExecutor(Executor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        this.executor = executor;
    }

    @Override
    public void execute(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {

        executor.execute(
                new ExecuteWithCleanupTask(cancelToken, task, cleanupTask));
    }
}
