package org.jtrim2.taskgraph;

import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableFunction;

/**
 * Defines a factory for creating task node function for the task execution graph.
 * Implementations of this interface must be repeatable without lasting side effect. Despite
 * being repeatable, this method might require external resources which may become
 * unavailable, making true repeatability infeasible. So, failures are allowed
 * by the contract of this interface.
 * <P>
 * The task node factory may {@link TaskInputBinder#bindInput(TaskNodeKey) bind inputs}
 * to be used by the returned task node function and set custom properties for the task
 * node to be created.
 *
 * <h3>Thread safety</h3>
 * The method of this interface must be safely callable concurrently
 * from multiple threads.
 *
 * <h4>Synchronization transparency</h4>
 * The method of this interface is not required to be <I>synchronization transparent</I>.
 * However, the method of this interface must expected to be called from any thread.
 *
 * @param <R> the return type of the task nodes created by the defined task node factory
 * @param <I> the type of the argument passed to the task node factory when requested for
 *   a node to be created
 *
 * @see TaskGraphDefConfigurer
 */
public interface TaskFactory<R, I> {
    /**
     * Creates the task node function, binds input for that function and may set
     * properties of the task node to be created.
     *
     * @param cancelToken the {@code CancellationToken} which can be checked
     *   periodically by this method to detect cancellation requests. This
     *   argument cannot be {@code null}.
     * @param nodeDef the properties used to create a task node. This argument cannot
     *   be {@code null}. This argument may only be used until this method returns.
     *   This argument cannot be {@code null}.
     * @return the function executed by the task node to be created. This method
     *   may never return {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if the task
     *   detects that it was canceled (usually by checking the provided
     *   {@code CancellationToken})
     * @throws Exception thrown if some irrecoverable error occurs. Note that such an
     *   exception usually means that the complete task graph execution will fail.
     */
    public CancelableFunction<R> createTaskNode(
            CancellationToken cancelToken,
            TaskNodeCreateArgs<I> nodeDef) throws Exception;
}
