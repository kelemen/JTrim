package org.jtrim.taskgraph.impl;

import org.jtrim.taskgraph.DependencyDag;
import org.jtrim.taskgraph.TaskNodeKey;

enum EagerTaskExecutionRestrictionStrategyBuilder implements TaskExecutionRestrictionStrategyFactory {
    EAGER;

    @Override
    public TaskExecutionRestrictionStrategy buildStrategy(
            DependencyDag<TaskNodeKey<?, ?>> taskGraph,
            Iterable<? extends RestrictableNode> restrictableNodes) {
        restrictableNodes.forEach(RestrictableNode::release);
        return NoOpStrategy.EAGER;
    }

    private enum NoOpStrategy implements TaskExecutionRestrictionStrategy {
        EAGER;

        @Override
        public void setNodeComputed(TaskNodeKey<?, ?> nodeKey) {
        }
    }
}
