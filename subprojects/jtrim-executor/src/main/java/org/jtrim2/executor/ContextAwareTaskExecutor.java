package org.jtrim2.executor;

/**
 * Defines a {@link TaskExecutor} which is able to decide whether the currently
 * running code is executing in the context of the executor or not.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface are required to be safely accessible from
 * multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I> because they may execute tasks, handlers added
 * to {@code CompletionStage}, etc.
 */
public interface ContextAwareTaskExecutor extends TaskExecutor {
    /**
     * Returns {@code true} if the calling code is executing in the context of
     * this executor. That is, it is executed by a task submitted to this
     * executor.
     * <P>
     * This method can be used to check that a method call is executing in the
     * context it was designed for.
     *
     * @return {@code true} if the calling code is executing in the context of
     *   this executor, {@code false} otherwise
     */
    public boolean isExecutingInThis();
}
