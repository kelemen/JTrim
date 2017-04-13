package org.jtrim.taskgraph.impl;

import java.util.Map;
import org.jtrim.taskgraph.DependencyDag;
import org.jtrim.taskgraph.TaskGraphExecutor;
import org.jtrim.taskgraph.TaskNodeKey;

public interface TaskGraphExecutorFactory {
    public TaskGraphExecutor createExecutor(
            DependencyDag<TaskNodeKey<?, ?>> taskGraph,
            Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes);
}
