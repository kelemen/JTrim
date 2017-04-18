package org.jtrim.taskgraph;

public interface TaskGraphExecutionResult {
    public <R> R getResult(TaskNodeKey<R, ?> key);
}
