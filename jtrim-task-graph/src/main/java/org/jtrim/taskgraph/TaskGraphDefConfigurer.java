package org.jtrim.taskgraph;

public interface TaskGraphDefConfigurer {
    public TaskFactoryDefiner factoryGroupDefiner(TaskFactoryGroupConfigurer groupConfigurer);

    public TaskGraphBuilder build();
}
