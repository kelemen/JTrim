package org.jtrim.taskgraph;

public interface TaskGraphExecutionResult {
    public ExecutionResultType getResultType();

    public <R> R getResult(TaskNodeKey<R, ?> key);
}
