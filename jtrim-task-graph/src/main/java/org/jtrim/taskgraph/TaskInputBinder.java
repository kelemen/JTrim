package org.jtrim.taskgraph;

public interface TaskInputBinder {
    public <I, A> TaskInputRef<I> bindInput(TaskNodeKey<I, A> defKey);

    public default <I, A> TaskInputRef<I> bindInput(Class<I> inputType, Class<A> factoryArgType, A factoryArg) {
        TaskFactoryKey<I, A> factoryKey = new TaskFactoryKey<>(inputType, factoryArgType);
        return bindInput(new TaskNodeKey<>(factoryKey, factoryArg));
    }
}
