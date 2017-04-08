package org.jtrim.taskgraph;

public interface TaskFactoryDefiner {
    public <R, I> void defineFactory(TaskFactoryKey<R, I> defKey, TaskFactorySetup<R, I> setup);

    public default <R, I> void defineSimpleFactory(TaskFactoryKey<R, I> defKey, TaskFactory<R, I> setup) {
        defineFactory(defKey, (properties) -> setup);
    }

    public default <R, I> void defineSimpleFactory(Class<R> resultType, Class<I> factoryArgType, TaskFactory<R, I> setup) {
        defineFactory(new TaskFactoryKey<>(resultType, factoryArgType), (properties) -> setup);
    }

    public default <R> void defineSimpleConstFactory(Class<R> resultType, TaskFactory<R, Void> setup) {
        defineFactory(new TaskFactoryKey<>(resultType, Void.class), (properties) -> setup);
    }
}
