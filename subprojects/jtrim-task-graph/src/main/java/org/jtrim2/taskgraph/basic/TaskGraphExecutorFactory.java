package org.jtrim2.taskgraph.basic;

import org.jtrim2.taskgraph.TaskGraphExecutor;
import org.jtrim2.taskgraph.TaskNodeKey;

/**
 * Defines factory creating {@code TaskGraphExecutor} for a given task graph.
 *
 * <h2>Thread safety</h2>
 * The method of this interface can be called from multiple threads
 * concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this class are not required to be <I>synchronization transparent</I>.
 *
 * @see CollectingTaskGraphBuilder
 * @see TaskGraphExecutor
 * @see org.jtrim2.taskgraph.TaskGraphExecutors
 */
public interface TaskGraphExecutorFactory {
    /**
     * Returns a new {@code TaskGraphExecutor} able to execute the given task graph.
     * The passed task nodes are already prepared to read their input assuming that
     * their dependencies were successfully computed.
     *
     * @param taskGraph the graph defining the dependencies between task nodes.
     *   This argument cannot be {@code null}.
     * @param nodes the nodes of the graph to be executed with respect to their
     *   dependencies. This argument cannot be {@code null} and may not contain
     *   {@code null} elements.
     * @return a new {@code TaskGraphExecutor} able to execute the given task graph.
     *   This method never returns {@code null}.
     */
    public TaskGraphExecutor createExecutor(
            DependencyDag<TaskNodeKey<?, ?>> taskGraph,
            Iterable<? extends TaskNode<?, ?>> nodes);
}
