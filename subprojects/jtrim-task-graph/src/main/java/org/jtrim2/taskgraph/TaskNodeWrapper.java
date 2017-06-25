package org.jtrim2.taskgraph;

import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableFunction;

/**
 * Defines a task node wrapping another task node. The wrapper can do something before
 * or after the actual task, transform its result or even avoid calling the wrapped task.
 *
 * @see TaskExecutorAop#wrapNode(TaskFactoryDefiner, TaskNodeWrapper)
 */
public interface TaskNodeWrapper {
    /**
     * Creates the task node function relying on the given wrapped task node. The wrapped
     * task node must be created by this method if it wants to actually execute the wrapped
     * task node.
     *
     * @param <R> the return type of the task nodes created by this method
     * @param <I> the type of the task node factory argument available in
     *   {@code nodeDef.factoryArg()}
     * @param cancelToken the {@code CancellationToken} which can be checked
     *   periodically by this method to detect cancellation requests. This
     *   argument cannot be {@code null}.
     * @param nodeDef the properties used to create a task node. This argument cannot
     *   be {@code null}. This argument may only be used until this method returns.
     *   This argument cannot be {@code null}.
     * @param factoryKey the factory key identifying the factory to be wrapped.
     *   This argument cannot be {@code null}.
     * @param wrappedFactory the factory creating the wrapped task node doing the
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
    public <R, I> CancelableFunction<R> createTaskNode(
            CancellationToken cancelToken,
            TaskNodeCreateArgs<I> nodeDef,
            TaskFactoryKey<R, I> factoryKey,
            TaskFactory<R, I> wrappedFactory) throws Exception;
}
