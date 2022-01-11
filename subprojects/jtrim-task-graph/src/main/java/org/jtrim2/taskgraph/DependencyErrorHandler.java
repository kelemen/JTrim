package org.jtrim2.taskgraph;

import org.jtrim2.cancel.CancellationToken;

/**
 * Defines an error handler to be called instead of the associated node computation when
 * the node is not being executed due to a failed dependency.
 *
 * <h2>Thread safety</h2>
 * Instances must have the same thread-safety property as the computation of the associated
 * task node.
 *
 * <h3>Synchronization transparency</h3>
 * Instances are not expected to be synchronization transparent and are called in the
 * same context as the associated computation would have been.
 *
 * @see TaskNodeProperties#tryGetDependencyErrorHandler()
 */
public interface DependencyErrorHandler {
    /**
     * Called when the associated node cannot be executed due to a dependency failure. This handler is
     * called on the same executor the associated node's computation would have been called on.
     * <P>
     * The graph execution is not considered completed before this method returns.
     *
     * @param cancelToken the cancellation token signaling the cancellation of the associated
     *   task graph execution. This argument cannot be {@code null}.
     * @param nodeKey the {@code TaskNodeKey} identifying the node associated with the failure.
     *   This argument cannot be {@code null}.
     * @param error the error causing the failure of the node execution. This argument cannot be
     *   {@code null}.
     *
     * @throws Exception thrown in case of some serious failure. The thrown exception will be
     *   suppressed by the error causing this method to be called.
     */
    public void handleDependencyError(
            CancellationToken cancelToken,
            TaskNodeKey<?, ?> nodeKey,
            Throwable error) throws Exception;
}
