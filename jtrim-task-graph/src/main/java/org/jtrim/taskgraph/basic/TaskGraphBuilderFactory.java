package org.jtrim.taskgraph.basic;

import java.util.Collection;
import org.jtrim.taskgraph.TaskFactoryConfig;
import org.jtrim.taskgraph.TaskGraphBuilder;

public interface TaskGraphBuilderFactory {
    public TaskGraphBuilder createGraphBuilder(
            Collection<? extends TaskFactoryConfig<?, ?>> configs);
}
