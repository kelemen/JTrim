package org.jtrim.concurrent;

import java.util.concurrent.Executor;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see ExecutorConverter#asExecutor(TaskExecutor)
 *
 * @author Kelemen Attila
 */
final class TaskExecutorAsExecutor implements Executor {
    // It is accessed in the factory method, when attempting to convert it back.
    final TaskExecutor executor;

    public TaskExecutorAsExecutor(TaskExecutor executor) {
        ExceptionHelper.checkNotNullArgument(executor, "executor");
        this.executor = executor;
    }

    @Override
    public void execute(final Runnable command) {
        ExceptionHelper.checkNotNullArgument(command, "command");

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            command.run();
        }, null);
    }
}
