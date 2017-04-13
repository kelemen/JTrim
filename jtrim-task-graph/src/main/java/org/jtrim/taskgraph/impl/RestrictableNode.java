package org.jtrim.taskgraph.impl;

import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.utils.ExceptionHelper;

public final class RestrictableNode {
    private final TaskNodeKey<?, ?> nodeKey;
    private final Runnable releaseAction;

    public RestrictableNode(TaskNodeKey<?, ?> nodeKey, Runnable releaseAction) {
        ExceptionHelper.checkNotNullArgument(nodeKey, "nodeKey");
        ExceptionHelper.checkNotNullArgument(releaseAction, "releaseAction");

        this.nodeKey = nodeKey;
        this.releaseAction = releaseAction;
    }

    public TaskNodeKey<?, ?> getNodeKey() {
        return nodeKey;
    }

    public Runnable getReleaseAction() {
        return releaseAction;
    }

    public void release() {
        releaseAction.run();
    }

    @Override
    public String toString() {
        return "RestrictableNode{" + nodeKey + '}';
    }
}
