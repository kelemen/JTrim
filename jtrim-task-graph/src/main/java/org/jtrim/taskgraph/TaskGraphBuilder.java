package org.jtrim.taskgraph;

import java.util.function.Consumer;
import org.jtrim.cancel.CancellationToken;

public interface TaskGraphBuilder {
    public void addNode(TaskNodeKey<?, ?> nodeKey);

    public void execute(CancellationToken cancelToken, Consumer<? super TaskGraphExecutionResult> onTerimateAction);
}
