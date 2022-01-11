package org.jtrim2.taskgraph;

/**
 * Defines the result of a task graph execution. The result is the output of a set of
 * explicitly select nodes in the task execution graph.
 *
 * <h2>Thread safety</h2>
 * Implementations of {@code TaskGraphExecutionResult} are expected to be (effectively) immutable.
 * Therefore, they must also be safely accessible from multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Implementations of {@code TaskGraphExecutionResult} are expected to be
 * <I>synchronization transparent</I>.
 *
 * @see TaskGraphExecutorProperties#getResultNodeKeys()
 * @see TaskGraphExecutor
 */
public interface TaskGraphExecutionResult {
    /**
     * Returns if the task graph execution completed completely successfully or not.
     * Note that you must have set the
     * {@link TaskGraphExecutorProperties#isDeliverResultOnFailure() deliverResultOnFailure}
     * property of the executor to {@code true} for this method to be useful. If you didn't,
     * this method will always return {@link ExecutionResultType#SUCCESS} because in every other
     * case, the result will not be provided (instead, the execution will complete exceptionally).
     *
     * @return if the task graph execution completed completely successfully or not. This
     *   method never returns {@code null}.
     *
     * @see TaskGraphExecutorProperties#isDeliverResultOnFailure()
     */
    public ExecutionResultType getResultType();

    /**
     * Returns the output of the task node identified by the given {@code TaskNodeKey}.
     *
     * @param <R> the type of the output of the node
     * @param key the {@code TaskNodeKey} identifying the node whose output is to be retrieved.
     *   This argument cannot be {@code null}.
     * @return the output of the task node identified by the given {@code TaskNodeKey}. This
     *   method may return {@code null} only if the queried node can produce a {@code null}
     *   output.
     *
     * @throws java.util.concurrent.CompletionException thrown if the task node action
     *   or one if its dependencies has failed with an exception. The cause of the
     *   {@code CompletionException} contains the exception thrown during the computation.
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if computation was canceled
     *   before this node could have been computed
     * @throws IllegalArgumentException thrown if the output of the given task is not available
     *   because it was not specified as required
     */
    public <R> R getResult(TaskNodeKey<R, ?> key);
}
