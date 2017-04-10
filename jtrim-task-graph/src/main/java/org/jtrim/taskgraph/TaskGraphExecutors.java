package org.jtrim.taskgraph;

import org.jtrim.taskgraph.impl.CollectingTaskGraphBuilder;
import org.jtrim.taskgraph.impl.CollectingTaskGraphDefConfigurer;
import org.jtrim.taskgraph.impl.EagerTaskGraphExecutor;

public final class TaskGraphExecutors {
    public static TaskGraphDefConfigurer newDefaultExecutor() {
        return new CollectingTaskGraphDefConfigurer((configs) -> {
            return new CollectingTaskGraphBuilder(configs, EagerTaskGraphExecutor::new);
        });
    }

    private TaskGraphExecutors() {
        throw new AssertionError();
    }
}
