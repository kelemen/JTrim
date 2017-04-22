package org.jtrim.taskgraph.basic;

import org.jtrim.taskgraph.TaskNodeKey;

public interface TaskExecutionRestrictionStrategyFactory {
    public TaskExecutionRestrictionStrategy buildStrategy(
            DependencyDag<TaskNodeKey<?, ?>> taskGraph,
            Iterable<? extends RestrictableNode> restrictableNodes);
}
