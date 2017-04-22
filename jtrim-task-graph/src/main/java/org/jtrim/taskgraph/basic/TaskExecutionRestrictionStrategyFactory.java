package org.jtrim.taskgraph.basic;

import org.jtrim.taskgraph.TaskNodeKey;

/**
 * Defines a factory of {@link TaskExecutionRestrictionStrategy} creating task
 * execution restriction strategy for a particular task graph.
 *
 * <h3>Thread safety</h3>
 * The method of this interface can be called from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not required to be <I>synchronization transparent</I>.
 *
 * @see RestrictableTaskGraphExecutor
 * @see TaskExecutionRestrictionStrategy
 */
public interface TaskExecutionRestrictionStrategyFactory {
    /**
     * Creates a strategy for the given task execution graph. The task execution
     * strategy may (and usually must) release some of the nodes before returning
     * task built strategy.
     *
     * @param taskGraph the task execution graph defining the dependencies
     *   between tasks. This argument cannot be {@code null}.
     * @param restrictableNodes the nodes this strategy is allowed to restrict
     *   from being scheduled for execution. If this list does not contain
     *   all nodes, it must be assumed that they were automatically released
     *   for execution. This argument cannot be {@code null} and cannot contain
     *   {@code null} elements.
     * @return a strategy for the given task execution graph. This method never
     *   returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if the strategy cannot allow
     *   scheduling the task graph for execution without breaking its
     *   resource constraint guarantees
     */
    public TaskExecutionRestrictionStrategy buildStrategy(
            DependencyDag<TaskNodeKey<?, ?>> taskGraph,
            Iterable<? extends RestrictableNode> restrictableNodes);
}
