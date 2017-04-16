package org.jtrim.taskgraph.impl;

import org.jtrim.taskgraph.DependencyDag;
import org.jtrim.taskgraph.TaskNodeKey;

public interface TaskExecutionRestrictionStrategyFactory {
    public TaskExecutionRestrictionStrategy buildStrategy(
            DependencyDag<TaskNodeKey<?, ?>> taskGraph,
            Iterable<? extends RestrictableNode> restrictableNodes);
}