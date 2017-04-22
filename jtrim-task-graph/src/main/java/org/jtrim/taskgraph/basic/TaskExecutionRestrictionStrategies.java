package org.jtrim.taskgraph.basic;

/**
 * Defines factory methods for {@code TaskExecutionRestrictionStrategyFactory} implementations.
 *
 * @see RestrictableTaskGraphExecutor
 * @see TaskExecutionRestrictionStrategyFactory
 */
public final class TaskExecutionRestrictionStrategies {
    /**
     * Returns a strategy which will allow executing tasks as soon as possible.
     *
     * @return a strategy which will allow executing tasks as soon as possible. This
     *   method never returns {@code null}.
     */
    public static TaskExecutionRestrictionStrategyFactory eagerStrategy() {
        return EagerTaskExecutionRestrictionStrategyBuilder.EAGER;
    }

    /**
     * Returns a strategy which will limit the number of scheduled nodes based
     * on how many leaf nodes are still retained. The strategy makes the following
     * assumptions:
     * <ul>
     *  <li>All leaf nodes' output require the same amount of resources.</li>
     *  <li>Non-zero resource cost is 1.</li>
     *  <li>The output of non leaf nodes resource need is negligible.</li>
     * </ul>
     * With these assumptions, the returned strategy will ensure that the
     * given resource constraint is mostly met during the task graph execution. This
     * constraint might be broken in the following way:
     * <ul>
     *  <li>
     *   If the graph is executable with this constraint, the resource constraint
     *   will be exceeded by less than maximum number of leafs a node in the graph
     *   retains (i.e., the number of direct or indirect leaf dependencies it has).
     *  </li>
     *  <li>
     *   The strategy will break the constraint, if breaking the constraint is necessary to
     *   make the graph executable.
     *  </li>
     * </ul>
     *
     * @param maxRetainedLeafNodes the maximum allowed resource usage of the graph execution.
     *   This argument must be at least 1.
     * @return a strategy which will limit the number of scheduled nodes based
     *   on how many leaf nodes are still retained. This method never returns {@code null}.
     */
    public static TaskExecutionRestrictionStrategyFactory weakLeafsOfEndNodeRestrictingStrategy(
            int maxRetainedLeafNodes) {
        return new WeakLeafsOfEndNodeRestrictingStrategy(maxRetainedLeafNodes);
    }

    private TaskExecutionRestrictionStrategies() {
        throw new AssertionError();
    }
}
