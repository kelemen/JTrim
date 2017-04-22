package org.jtrim.taskgraph.basic;

import java.util.Collection;
import org.jtrim.taskgraph.TaskFactoryConfig;
import org.jtrim.taskgraph.TaskGraphBuilder;

/**
 * Defines factory creating {@code TaskGraphBuilder} for a list of task
 * node factory definitions.
 *
 * <h3>Thread safety</h3>
 * The method of this interface can be called from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not required to be <I>synchronization transparent</I>.
 *
 * @see CollectingTaskGraphDefConfigurer
 * @see TaskGraphExecutors
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
