package org.jtrim.taskgraph.impl;

import java.util.Map;
import org.jtrim.taskgraph.TaskGraphExecutor;
import org.jtrim.taskgraph.TaskNodeGraph;
import org.jtrim.taskgraph.TaskNodeKey;

public interface TaskGraphExecutorFactory {
    public TaskGraphExecutor createExecutor(
            TaskNodeGraph dependencyGraph,
            Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes);
}
