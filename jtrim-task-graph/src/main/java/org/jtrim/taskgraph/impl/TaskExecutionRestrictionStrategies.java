package org.jtrim.taskgraph.impl;

public final class TaskExecutionRestrictionStrategies {
    public static TaskExecutionRestrictionStrategyFactory eagerStrategy() {
        return EagerTaskExecutionRestrictionStrategyBuilder.EAGER;
    }

    public static TaskExecutionRestrictionStrategyFactory weakLeafsOfEndNodeRestrictingStrategy(
            int maxRetainedLeafNodes) {
        return new WeakLeafsOfEndNodeRestrictingStrategy(maxRetainedLeafNodes);
    }

    private TaskExecutionRestrictionStrategies() {
        throw new AssertionError();
    }
}
