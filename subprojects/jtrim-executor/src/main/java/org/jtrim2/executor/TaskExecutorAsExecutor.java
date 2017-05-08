package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.Executor;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;

/**
 * @see ExecutorConverter#asExecutor(TaskExecutor)
 *
 * @author Kelemen Attila
 */
final class TaskExecutorAsExecutor implements Executor {
    // It is accessed in the factory method, when attempting to convert it back.
    final TaskExecutor executor;

    public TaskExecutorAsExecutor(TaskExecutor executor) {
        Objects.requireNonNull(executor, "executor");
        this.executor = executor;
    }

    @Override
    public void execute(final Runnable command) {
        Objects.requireNonNull(command, "command");

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            command.run();
        }, null);
    }
}
