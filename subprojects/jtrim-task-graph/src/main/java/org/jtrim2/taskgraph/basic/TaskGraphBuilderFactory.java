package org.jtrim2.taskgraph.basic;

import java.util.Collection;
import org.jtrim2.taskgraph.TaskFactoryConfig;
import org.jtrim2.taskgraph.TaskGraphBuilder;

/**
 * Defines factory creating {@code TaskGraphBuilder} for a list of task
 * node factory definitions.
 *
 * <h2>Thread safety</h2>
 * The method of this interface can be called from multiple threads
 * concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this class are not required to be <I>synchronization transparent</I>.
 *
 * @see CollectingTaskGraphDefConfigurer
 * @see org.jtrim2.taskgraph.TaskGraphExecutors
 * @see TaskGraphBuilder
 */
public interface TaskGraphBuilderFactory {
    /**
     * Returns a new {@code TaskGraphBuilder} able to build a graph using the given
     * task node factories.
     *
     * @param configs the task node factory definitions. This argument cannot be {@code null}
     *   and may not contain {@code null} elements.
     * @return a new {@code TaskGraphBuilder} able to build a graph using the given
     *   task node factories. This method never returns {@code null}.
     */
    public TaskGraphBuilder createGraphBuilder(
            Collection<? extends TaskFactoryConfig<?, ?>> configs);
}
