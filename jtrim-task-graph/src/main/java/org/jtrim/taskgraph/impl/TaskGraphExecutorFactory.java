package org.jtrim.taskgraph.impl;

import java.util.Map;
import org.jtrim.taskgraph.DirectedGraph;
import org.jtrim.taskgraph.TaskGraphExecutor;
import org.jtrim.taskgraph.TaskNodeKey;

public interface TaskGraphExecutorFactory {
    public TaskGraphExecutor createExecutor(
            DirectedGraph<TaskNodeKey<?, ?>> dependencyGraph,
            Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes);
}
