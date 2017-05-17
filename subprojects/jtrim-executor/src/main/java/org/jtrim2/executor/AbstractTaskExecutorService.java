package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;

/**
 * Defines a convenient abstract base class for {@link TaskExecutorService}
 * implementations.
 * <P>
 * {@code AbstractTaskExecutorService} extends {@code AbstractTaskExecutor} with
 * automatically canceling the task if the executor was shut down, so implementations
 * does not need to check for shut down.
 */
public abstract class AbstractTaskExecutorService
extends
        AbstractTaskExecutor
implements
        TaskExecutorService {

    /**
     * {@inheritDoc }
     */
    @Override
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {
        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(function, "function");

        if (isShutdown()) {
            return CancelableTasks.canceledComplationStage();
        }

        return super.executeFunction(cancelToken, function);
    }
}
