package org.jtrim2.concurrent;

/**
 * Defines a {@link TaskExecutor} which is able to provide a few statistical
 * information about the currently queued tasks. This information cannot be used
 * for synchronization purposes and should be considered unreliable. However,
 * you can base some decision on these methods: For example, if the
 * {@link #getNumberOfQueuedTasks() number of queued tasks} exceed a predefined
 * constant, you may decide to schedule tasks slower to the executor.
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
public interface MonitorableTaskExecutor extends ContextAwareTaskExecutor {
    /**
     * Returns the approximate number of tasks currently queued to this
     * executor. The queued tasks are not currently executing but are scheduled
     * to be executed in the future.
     * <P>
     * Note that the value returned by this method should be considered
     * unreliable and cannot be used for synchronization purposes.
     *
     * @return the approximate number of tasks currently queued to this
     *   executor. This method always returns a value greater than or equal to
     *   zero.
     */
    public long getNumberOfQueuedTasks();

    /**
     * Returns the approximate number of tasks currently being executed.
     * <P>
     * Note that the value returned by this method should be considered
     * unreliable and cannot be used for synchronization purposes.
     *
     * @return the approximate number of tasks currently being executed. This
     *   method always returns a value greater than or equal to zero.
     */
    public long getNumberOfExecutingTasks();
}
