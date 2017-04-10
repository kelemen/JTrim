package org.jtrim.taskgraph;

import org.jtrim.cancel.CancellationToken;

public interface TaskGraphExecutor {
    public TaskGraphExecutorProperties.Builder properties();

    public TaskGraphFuture<TaskGraphExecutionResult> execute(CancellationToken cancelToken);
}
