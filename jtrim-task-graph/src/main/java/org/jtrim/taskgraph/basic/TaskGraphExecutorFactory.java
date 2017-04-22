package org.jtrim.taskgraph.basic;

import org.jtrim.taskgraph.TaskGraphExecutor;
import org.jtrim.taskgraph.TaskNodeKey;

public interface TaskGraphExecutorFactory {
    public TaskGraphExecutor createExecutor(
            DependencyDag<TaskNodeKey<?, ?>> taskGraph,
            Iterable<? extends TaskNode<?, ?>> nodes);
}
