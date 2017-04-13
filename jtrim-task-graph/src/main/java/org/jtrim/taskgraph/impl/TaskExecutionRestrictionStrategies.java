package org.jtrim.taskgraph.impl;

public final class TaskExecutionRestrictionStrategies {
    public static TaskExecutionRestrictionStrategyFactory eagerStrategy() {
        return EagerTaskExecutionRestrictionStrategyBuilder.EAGER;
    }

    private TaskExecutionRestrictionStrategies() {
        throw new AssertionError();
    }
}
