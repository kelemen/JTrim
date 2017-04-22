package org.jtrim.taskgraph.basic;

import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableFunction;
import org.jtrim.taskgraph.TaskNodeProperties;
import org.jtrim.utils.ExceptionHelper;

public final class NodeTaskRef<R> {
    private final TaskNodeProperties properties;
    private final CancelableFunction<? extends R> task;

    public NodeTaskRef(TaskNodeProperties properties, CancelableFunction<? extends R> task) {
        ExceptionHelper.checkNotNullArgument(properties, "properties");
        ExceptionHelper.checkNotNullArgument(task, "task");

        this.properties = properties;
        this.task = task;
    }

    public R compute(CancellationToken cancelToken) throws Exception {
        return task.execute(cancelToken);
    }

    public TaskNodeProperties getProperties() {
        return properties;
    }
}
