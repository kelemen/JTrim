
package org.jtrim2.executor;

/**
 * Defines a {@link TaskExecutor} which is able to provide a few statistical
 * information about the currently queued tasks. This information cannot be used
 * for synchronization purposes and should be considered unreliable. However,
 * you can base some decision on these methods: For example, if the
 * {@link #getNumberOfQueuedTasks() number of queued tasks} exceed a predefined
 * constant, you may decide to schedule tasks slower to the executor.
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
public interface MonitorableTaskExecutorService
extends
        ContextAwareTaskExecutorService,
        MonitorableTaskExecutor {
}
