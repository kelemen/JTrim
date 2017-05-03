package org.jtrim2.taskgraph;

import org.jtrim2.taskgraph.basic.CollectingTaskGraphBuilder;
import org.jtrim2.taskgraph.basic.CollectingTaskGraphDefConfigurer;
import org.jtrim2.taskgraph.basic.RestrictableTaskGraphExecutor;
import org.jtrim2.taskgraph.basic.TaskExecutionRestrictionStrategies;
import org.jtrim2.taskgraph.basic.TaskExecutionRestrictionStrategyFactory;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines factory methods to create a task graph executors.
 */
public final class TaskGraphExecutors {
    /**
     * Creates a task graph executor which will schedule all nodes to be computed
     * as soon as possible. That is, a node will be scheduled to be computed when
     * all of its dependencies have been computed.
     *
     * @return a task graph executor which will schedule all nodes to be computed
     *   as soon as possible. This method never returns {@code null}.
     */
    public static TaskGraphDefConfigurer newEagerExecutor() {
        return newRestrictableExecutor(TaskExecutionRestrictionStrategies.eagerStrategy());
    }

    /**
     * Creates a task graph executor which will limit the number of scheduled
     * nodes based on how many leaf nodes are still retained. This task graph executor
     * makes the following assumptions:
     * <ul>
     *  <li>All leaf nodes' output require the same amount of resources.</li>
     *  <li>Non-zero resource cost is 1.</li>
     *  <li>The output of non leaf nodes resource need is negligible.</li>
     * </ul>
     * With these assumptions, the returned task graph executor will ensure that the
     * given resource constraint is mostly met during the task graph execution. This
     * constraint might be broken in the following way:
     * <ul>
     *  <li>
     *   If the graph is executable with this constraint, the resource constraint
     *   will be exceeded by less than maximum number of leafs a node in the graph
     *   retains (i.e., the number of direct or indirect leaf dependencies it has).
     *  </li>
     *  <li>
     *   The executor will break the constraint, if breaking the constraint is necessary to
     *   make the graph executable.
     *  </li>
     * </ul>
     *
     * @param maxRetainedLeafNodes the maximum allowed resource usage of the graph execution.
     *   This argument must be at least 1.
     * @return a task graph executor which will limit the number of scheduled
     *   nodes based on how many leaf nodes are still retained. This method never
     *   returns {@code null}.
     */
    public static TaskGraphDefConfigurer newWeakLeafRestricterExecutor(int maxRetainedLeafNodes) {
        return newRestrictableExecutor(
                TaskExecutionRestrictionStrategies.weakLeafsOfEndNodeRestrictingStrategy(maxRetainedLeafNodes));
    }

    /**
     * Creates a task graph executor which will limit the scheduled nodes with the given
     * custom strategy.
     *
     * @param restrictionStrategy the custom strategy used to limit the concurrently
     *   scheduled nodes. This argument cannot be {@code null}.
     * @return a task graph executor which will limit the scheduled nodes with the given
     *   custom strategy. This method never returns {@code null}.
     */
    public static TaskGraphDefConfigurer newRestrictableExecutor(
            TaskExecutionRestrictionStrategyFactory restrictionStrategy) {
        ExceptionHelper.checkNotNullArgument(restrictionStrategy, "restrictionStrategy");

        return new CollectingTaskGraphDefConfigurer((configs) -> {
            return new CollectingTaskGraphBuilder(configs, (taskGraph, nodes) -> {
                return new RestrictableTaskGraphExecutor(taskGraph, nodes, restrictionStrategy);
            });
        });
    }

    private TaskGraphExecutors() {
        throw new AssertionError();
    }
}
