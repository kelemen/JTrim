package org.jtrim2.taskgraph;

/**
 * Allows to define task node factories to build a task graph from. Note that having a task factory
 * defined not in itself will cause a node to be created. It will just add the possibility for a node
 * to be created.
 * <P>
 * Task factories are uniquely identified by their associated {@link TaskFactoryKey} within the
 * same {@link TaskGraphDefConfigurer}.
 *
 * <h3>Thread safety</h3>
 * The methods of this interface may not necessarily safe to be used from multiple
 * threads concurrently (though some implementations might choose to allow concurrent access).
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not <I>synchronization transparent</I> in general.
 *
 * @see TaskGraphDefConfigurer
 */
public interface TaskFactoryDefiner {
    /**
     * Adds deferred task node factory definition. If you do not need to create the task node factory lazily,
     * you should consider using the more convenient
     * {@link #defineSimpleFactory(TaskFactoryKey, TaskFactory) defineSimpleFactory} method. The deferred
     * task node factory creation is useful if the creating the a factory is not cheap and might not
     * be needed.
     * <P>
     * If a task node factory was already defined with the given key, it will be replaced by
     * the new definition. Even if, it was defined via a {@code defineSimpleFactory} method,
     * or another {@code TaskFactoryDefiner} of the same {@link TaskGraphDefConfigurer}.
     *
     * @param <R> the return type of the task nodes created by the defined task node factory.
     * @param <I> the type of the argument passed to the task node factory when requested for
     *   a node to be created
     * @param defKey the key uniquely identifying a task node factory. This argument cannot be
     *   {@code null}.
     * @param setup the call creating the task node factory (i.e., the factory of the task node factory).
     *   This argument cannot be {@code null}.
     * @return the task node factory definition previously associated with the given factory key,
     *   or {@code null} if no factory was overwritten
     *
     * @see #defineSimpleFactory(TaskFactoryKey, TaskFactory) defineSimpleFactory
     */
    public <R, I> TaskFactoryConfig<R, I> defineFactory(
            TaskFactoryKey<R, I> defKey,
            TaskFactorySetup<R, I> setup);

    /**
     * Adds a task node factory.
     * <P>
     * If a task node factory was already defined with the given key, it will be replaced by
     * the new definition. Even if, it was defined via another {@code defineSimpleFactory} method,
     * or another {@code TaskFactoryDefiner} of the same {@link TaskGraphDefConfigurer}.
     * <P>
     * The default implementation simply delegates to the most generic
     * {@link #defineFactory(TaskFactoryKey, TaskFactorySetup) defineFactory} method.
     *
     * @param <R> the return type of the task nodes created by the defined task node factory.
     * @param <I> the type of the argument passed to the task node factory when requested for
     *   a node to be created
     * @param defKey the key uniquely identifying a task node factory. This argument cannot be
     *   {@code null}.
     * @param taskFactory the task node factory. This argument cannot be {@code null}.
     * @return the task node factory definition previously associated with the given factory key,
     *   or {@code null} if no factory was overwritten
     *
     * @see #defineFactory(TaskFactoryKey, TaskFactorySetup) defineFactory
     */
    public default <R, I> TaskFactoryConfig<R, I> defineSimpleFactory(
            TaskFactoryKey<R, I> defKey,
            TaskFactory<R, I> taskFactory) {
        return defineFactory(defKey, (properties) -> taskFactory);
    }

    /**
     * Adds a task node factory. This method is the same as calling the other
     * {@link #defineSimpleFactory(TaskFactoryKey, TaskFactory) defineSimpleFactory} with a {@code null}
     * {@link TaskFactoryKey#getKey() custom key}.
     * <P>
     * If a task node factory was already defined with the given key, it will be replaced by
     * the new definition. Even if, it was defined via another {@code defineSimpleFactory} method,
     * or another {@code TaskFactoryDefiner} of the same {@link TaskGraphDefConfigurer}.
     * <P>
     * The default implementation simply delegates to the most generic
     * {@link #defineFactory(TaskFactoryKey, TaskFactorySetup) defineFactory} method.
     *
     * @param <R> the return type of the task nodes created by the defined task node factory.
     * @param <I> the type of the argument passed to the task node factory when requested for
     *   a node to be created
     * @param resultType the return type of the task nodes created by the defined task node factory.
     *   This argument is part of the key uniquely identifying the task node factory.
     *   This argument cannot be {@code null}.
     * @param factoryArgType the type of the argument passed to the task node factory when requested for
     *   a node to be created. This argument is part of the key uniquely identifying the task node factory.
     *   This argument cannot be {@code null}.
     * @param taskFactory the task node factory. This argument cannot be {@code null}.
     * @return the task node factory definition previously associated with the given factory key,
     *   or {@code null} if no factory was overwritten
     *
     * @see #defineFactory(TaskFactoryKey, TaskFactorySetup) defineFactory
     */
    public default <R, I> TaskFactoryConfig<R, I> defineSimpleFactory(
            Class<R> resultType,
            Class<I> factoryArgType,
            TaskFactory<R, I> taskFactory) {

        return defineFactory(new TaskFactoryKey<>(resultType, factoryArgType), (properties) -> taskFactory);
    }
}
