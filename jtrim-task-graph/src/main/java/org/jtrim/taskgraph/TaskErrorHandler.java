package org.jtrim.taskgraph;

public interface TaskErrorHandler {
    public void onError(TaskNodeKey<?, ?> nodeKey, Throwable error);
}
