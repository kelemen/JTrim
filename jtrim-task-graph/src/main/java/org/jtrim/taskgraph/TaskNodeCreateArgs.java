package org.jtrim.taskgraph;

import org.jtrim.utils.ExceptionHelper;

public final class TaskNodeCreateArgs<T> {
    private final T factoryArg;
    private final TaskInputBinder inputs;
    private final TaskNodeProperties.Builder properties;

    public TaskNodeCreateArgs(T argument, TaskNodeProperties defaults, TaskInputBinder inputs) {
        ExceptionHelper.checkNotNullArgument(inputs, "inputs");

        this.factoryArg = argument;
        this.inputs = inputs;
        this.properties = new TaskNodeProperties.Builder(defaults);
    }

    public T factoryArg() {
        return factoryArg;
    }

    public TaskInputBinder inputs() {
        return inputs;
    }

    public TaskNodeProperties.Builder properties() {
        return properties;
    }
}
