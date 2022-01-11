package org.jtrim2.taskgraph.basic;

import org.jtrim2.taskgraph.TaskNodeKey;

/**
 * Defines a strategy which can restrict nodes from being executed in order to
 * prevent too much concurrent resource usage. The strategy can block
 * tasks from being scheduled for execution by not releasing the associated
 * {@link RestrictableNode}.
 * <P>
 * All implementations of this strategy must eventually release all nodes to
 * be scheduled for execution, otherwise task execution will never complete.
 * <P>
 * The strategy may rely on the task graph executor, that it will eventually execute a task
 * (i.e., will call {@link #setNodeComputed(TaskNodeKey) setNodeComputed} with the task nodes' key),
 * if the task and all of its dependencies were released by the strategy.
 *
 * <h2>Thread safety</h2>
 * The method of this interface can be called from multiple threads
 * concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this class are not required to be <I>synchronization transparent</I>.
 *
 * @see RestrictableTaskGraphExecutor
 * @see TaskExecutionRestrictionStrategyFactory
 */
public interface TaskExecutionRestrictionStrategy {
    /**
     * The task graph executor calls this method for every computed task node or task nodes
     * decided to be not computed (possibly because one of its dependencies failed).
     * <P>
     * The task graph executor may not call this method multiple times with the same
     * key.
     *
     * @param nodeKey the {@code TaskNodeKey} identifying the executed node. This
     *   argument cannot be {@code null}.
     */
    public void setNodeComputed(TaskNodeKey<?, ?> nodeKey);
}
