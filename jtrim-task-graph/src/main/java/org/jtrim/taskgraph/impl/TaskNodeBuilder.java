package org.jtrim.taskgraph.impl;

import org.jtrim.cancel.CancellationToken;
import org.jtrim.taskgraph.TaskInputBinder;
import org.jtrim.taskgraph.TaskNodeKey;

public interface TaskNodeBuilder {
    public <R> NodeTaskRef<R> createNode(
            CancellationToken cancelToken,
            TaskNodeKey<R, ?> nodeKey,
            TaskInputBinder inputBinder) throws Exception;

    public <R, I> TaskNode<R, I> addAndBuildNode(TaskNodeKey<R, I> nodeKey);
}
