package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;

/**
 * A {@code TaskExecutor} implementation forwarding tasks to another task
 * executor an being able to determine if tasks submitted to the
 * {@code ContextAwareWrapper} run in the the context of the same
 * {@code ContextAwareWrapper}.
 * <P>
 * It is also possible to create a task executor which shares execution context
 * with the {@code ContextAwareWrapper} via the
 * {@link #sameContextExecutor(TaskExecutor) sameContextExecutor} method.
 * <P>
 * This class may only be instantiated by the static factory methods in
 * {@link TaskExecutors}:
 * {@link TaskExecutors#contextAware(TaskExecutor) TaskExecutors.contextAware} and
 * {@link TaskExecutors#contextAwareIfNecessary(TaskExecutor) TaskExecutors.contextAwareIfNecessary}.
 *
 * <h2>Thread safety</h2>
 * Methods of this class are safely accessible from multiple threads
 * concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Method of this class are not <I>synchronization transparent</I> unless
 * otherwise noted.
 *
 * @see TaskExecutors#contextAware(TaskExecutor)
 * @see TaskExecutors#contextAwareIfNecessary(TaskExecutor)
 */
public final class ContextAwareWrapper implements ContextAwareTaskExecutor {
    private final TaskExecutor wrapped;
    // null if not in context, running in context otherwise.
    private final ThreadLocal<Object> inContext;

    ContextAwareWrapper(TaskExecutor wrapped) {
        this(wrapped, new ThreadLocal<>());
    }

    private ContextAwareWrapper(TaskExecutor wrapped, ThreadLocal<Object> inContext) {
        Objects.requireNonNull(wrapped, "wrapped");
        assert inContext != null;

        this.wrapped = wrapped;
        this.inContext = inContext;
    }

    /**
     * Returns an executor which submits tasks to the given executor and
     * executes tasks in the same context as this executor. That is, calling
     * the {@link #isExecutingInThis() isExecutingInThis} method of the
     * returned executor is equivalent to calling the {@code isExecutingInThis}
     * of this executor.
     * <P>
     * Note that this method may or may not return a new instance.
     *
     * @param executor the specified executor to which the returned executor
     *   will submit tasks to. This argument cannot be {@code null}.
     * @return an executor which submits tasks to the given executor and
     *   executes tasks in the same context as this executor. This method never
     *   returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public ContextAwareWrapper sameContextExecutor(TaskExecutor executor) {
        if (executor == this.wrapped) {
            return this;
        } else {
            return new ContextAwareWrapper(executor, inContext);
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * This method is <I>synchronization transparent</I>.
     */
    @Override
    public boolean isExecutingInThis() {
        Object result = inContext.get();
        if (result == null) {
            inContext.remove();
            return false;
        } else {
            return true;
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <V> CompletionStage<V> executeFunction(
            CancellationToken cancelToken,
            CancelableFunction<? extends V> function) {
        Objects.requireNonNull(function, "function");

        return wrapped.executeFunction(cancelToken, (CancellationToken taskCancelToken) -> {
            Object prevValue = inContext.get();
            try {
                if (prevValue == null) {
                    inContext.set(true);
                }
                return function.execute(taskCancelToken);
            } finally {
                if (prevValue == null) {
                    inContext.remove();
                }
            }
        });
    }
}
