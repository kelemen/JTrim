package org.jtrim2.taskgraph;

/**
 * Defines a deferred creator of a task node factory. Implementations
 * of this interface must be repeatable without lasting side effect. Despite
 * being repeatable, this method might require external resources which may become
 * unavailable, making true repeatability infeasible. So, failures are allowed
 * by the contract of this interface.
 *
 * <h2>Thread safety</h2>
 * The method of this interface must be safely callable concurrently
 * from multiple threads.
 *
 * <h3>Synchronization transparency</h3>
 * The method of this interface is not required to be <I>synchronization transparent</I>.
 * However, the method of this interface must expected to be called from any thread.
 *
 * @param <R> the return type of the task nodes created by the defined task node factory
 * @param <I> the type of the argument passed to the task node factory when requested for
 *   a node to be created
 */
public interface TaskFactorySetup<R, I> {
    /**
     * Creates a task node factory given the properties of the factory.
     *
     * @param properties the properties of the node factory to be created.
     *   This argument cannot be {@code null}.
     * @return the task node factory. This method may never return {@code null}.
     *
     * @throws Exception thrown in case of failing to create the task node factory.
     *   Not however, that such an exception is considered as a fatal failure most likely
     *   leading to complete task graph execution to fail.
     */
    public TaskFactory<R, I> setup(TaskFactoryProperties properties) throws Exception;
}
