package org.jtrim2.taskgraph;

/**
 * Defines a callback notified whenever a failure occurs while trying to create or execute a task node.
 * This callback can be specified for a {@link TaskGraphBuilder} or a {@link TaskGraphExecutor}.
 *
 * <h2>Thread safety</h2>
 * The method of this interface is expected to be safely callable by multiple threads
 * concurrently. The method should have no expectation on the thread and context from which the
 * callback is called.
 *
 * <h3>Synchronization transparency</h3>
 * The method of this interface is not expected to be completely <I>synchronization transparent</I>.
 * However, the callback must be aware that it can be called from many context. Callers of the
 * callback must not hold locks while calling the callback.
 *
 * @see TaskGraphBuilderProperties#getNodeCreateErrorHandler()
 * @see TaskGraphExecutorProperties#getComputeErrorHandler()
 */
public interface TaskErrorHandler {
    /**
     * Called whenever a failure occurs while trying to create or execute a task node.
     * This method has the responsibility of logging the error. If it fails to do so, the actual
     * error will not visible to the outside world (though its implications are detectable).
     *
     * @param nodeKey the {@code TaskNodeKey} identifying the node associated with the failure.
     *   This argument cannot be {@code null}.
     * @param error the error causing the failure of the node creation. This argument cannot be
     *   {@code null}.
     */
    public void onError(TaskNodeKey<?, ?> nodeKey, Throwable error);
}
