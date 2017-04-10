package org.jtrim.taskgraph;

import org.jtrim.cancel.CancellationToken;

public interface TaskGraphBuilder {
    public void addNode(TaskNodeKey<?, ?> nodeKey);

    public TaskGraphBuilderProperties.Builder properties();

    public TaskGraphFuture<TaskGraphExecutor> buildGraph(CancellationToken cancelToken);
}
