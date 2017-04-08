package org.jtrim.taskgraph;

public interface TaskGraphDefConfigurer {
    public TaskGraphExecProperties.Builder properties();

    public TaskFactoryDefiner factoryGroupDefiner(TaskFactoryGroupConfigurer groupConfigurer);

    public TaskGraphBuilder build();
}
