package org.jtrim.taskgraph.basic;

import org.jtrim.taskgraph.TaskNodeKey;

public interface TaskExecutionRestrictionStrategy {
    public void setNodeComputed(TaskNodeKey<?, ?> nodeKey);
}
