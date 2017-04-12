package org.jtrim.taskgraph;

import java.util.concurrent.CompletionStage;
import org.jtrim.cancel.CancellationToken;

public interface TaskGraphBuilder {
    public void addNode(TaskNodeKey<?, ?> nodeKey);

    public TaskGraphBuilderProperties.Builder properties();

    public CompletionStage<TaskGraphExecutor> buildGraph(CancellationToken cancelToken);
}
