package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.Executor;
import org.jtrim2.cancel.CancellationToken;

/**
 * @see ExecutorConverter#asTaskExecutor(Executor)
 *
 * @author Kelemen Attila
 */
final class ExecutorAsTaskExecutor implements TaskExecutor {
    // It is accessed in the factory method, when attempting to convert it back.
    final Executor executor;
    private final boolean mayInterruptTask;

    public ExecutorAsTaskExecutor(Executor executor, boolean mayInterruptTask) {
        Objects.requireNonNull(executor, "executor");
        this.executor = executor;
        this.mayInterruptTask = mayInterruptTask;
    }

    @Override
    public void execute(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {

        executor.execute(new ExecuteWithCleanupTask(
                mayInterruptTask, cancelToken, task, cleanupTask));
    }
}
