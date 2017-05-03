package org.jtrim2.concurrent;

/**
 * Defines a {@link TaskExecutorService} which is able to decide whether the
 * currently running code is executing in the context of the executor or not.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safely accessible from
 * multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I> because they may execute tasks, cleanup
 * tasks, etc.
 *
 * @author Kelemen Attila
 */
public interface ContextAwareTaskExecutorService
extends
        TaskExecutorService,
        ContextAwareTaskExecutor {
}
