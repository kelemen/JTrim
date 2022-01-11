package org.jtrim2.executor;

/**
 * Defines a {@link TaskExecutorService} which is able to decide whether the
 * currently running code is executing in the context of the executor or not.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface are required to be safely accessible from
 * multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I> because they may execute tasks, completion handlers
 * tasks, etc.
 */
public interface ContextAwareTaskExecutorService
extends
        TaskExecutorService,
        ContextAwareTaskExecutor {
}
