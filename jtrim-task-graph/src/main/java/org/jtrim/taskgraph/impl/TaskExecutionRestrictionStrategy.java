package org.jtrim.taskgraph.impl;

import org.jtrim.taskgraph.TaskNodeKey;

public interface TaskExecutionRestrictionStrategy {
    public void setNodeComputed(TaskNodeKey<?, ?> nodeKey);
}
