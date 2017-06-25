package org.jtrim2.taskgraph;

/**
 * Defines a task node factory wrapping another task node factory. The wrapper can do something before
 * or after the actual factory, transform its result or even avoid calling the wrapped task node factory.
 *
 * @see TaskExecutorAop#wrapFactory(TaskFactoryDefiner, TaskFactoryWrapper)
 */
public interface TaskFactoryWrapper {
    /**
     * Creates the task node factory relying on the given wrapped task node factory.
     * The wrapped task node factory must be created by this method if it wants to actually
     * execute the wrapped task node factory.
     *
     * @param <R> the return type of the task nodes created by this method
     * @param <I> the type of the task node factory argument available in
     *   {@code nodeDef.factoryArg()}
     * @param properties the properties of the node factory to be created.
     *   This argument cannot be {@code null}.
     * @param factoryKey the factory key identifying the factory to be wrapped.
     *   This argument cannot be {@code null}.
     * @param wrapped the factory creating the wrapped task node factory doing the
     *   actual work. This argument cannot be {@code null}.
     * @return the function executed by the task node to be created. This method
     *   may never return {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if the task
     *   detects that it was canceled (usually by checking the provided
     *   {@code CancellationToken})
     * @throws Exception thrown if some irrecoverable error occurs. Note that such an
     *   exception usually means that the complete task graph execution will fail.
     */
    public <R, I> TaskFactory<R, I> createTaskNode(
            TaskFactoryProperties properties,
            TaskFactoryKey<R, I> factoryKey,
            TaskFactorySetup<R, I> wrapped) throws Exception;
}
