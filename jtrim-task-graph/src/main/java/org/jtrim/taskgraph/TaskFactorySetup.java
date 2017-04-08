package org.jtrim.taskgraph;

public interface TaskFactorySetup<R, T> {
    public TaskFactory<R, T> setup(TaskFactoryProperties properties) throws Exception;
}
