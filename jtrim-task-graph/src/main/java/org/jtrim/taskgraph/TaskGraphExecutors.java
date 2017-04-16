package org.jtrim.taskgraph;

import org.jtrim.taskgraph.impl.CollectingTaskGraphBuilder;
import org.jtrim.taskgraph.impl.CollectingTaskGraphDefConfigurer;
import org.jtrim.taskgraph.impl.RestrictableTaskGraphExecutor;
import org.jtrim.taskgraph.impl.TaskExecutionRestrictionStrategies;
import org.jtrim.taskgraph.impl.TaskExecutionRestrictionStrategyFactory;
import org.jtrim.utils.ExceptionHelper;

public final class TaskGraphExecutors {
    public static TaskGraphDefConfigurer newEagerExecutor() {
        return newRestrictableExecutor(TaskExecutionRestrictionStrategies.eagerStrategy());
    }

    public static TaskGraphDefConfigurer newWeakLeafRestricterExecutor(int maxRetainedLeafNodes) {
        return newRestrictableExecutor(
                TaskExecutionRestrictionStrategies.weakLeafsOfEndNodeRestrictingStrategy(maxRetainedLeafNodes));
    }

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
