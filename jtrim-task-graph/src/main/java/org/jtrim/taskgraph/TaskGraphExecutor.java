package org.jtrim.taskgraph;

import java.util.concurrent.CompletionStage;
import org.jtrim.cancel.CancellationToken;

public interface TaskGraphExecutor {
    public TaskGraphExecutorProperties.Builder properties();

    public CompletionStage<TaskGraphExecutionResult> execute(CancellationToken cancelToken);
}
