package org.jtrim2.taskgraph.basic;

import java.util.Objects;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.taskgraph.TaskNodeProperties;

/**
 * Defines the fully built task of a node.
 *
 * <h2>Thread safety</h2>
 * The {@link #getProperties() properties} of a {@code NodeTaskRef} can be safely
 * used by multiple threads concurrently. However, the {@link #compute(CancellationToken) compute}
 * method may not be called multiple times (implying that it cannot be called by
 * multiple threads concurrently).
 *
 * <h3>Synchronization transparency</h3>
 * The {@link #getProperties() getProperties} method is <I>synchronization transparent</I> but the
 * {@link #compute(CancellationToken) compute} method is not.
 *
 * @param <R> the return type of the task nodes
 */
public final class NodeTaskRef<R> {
    private final TaskNodeProperties properties;
    private final CancelableFunction<? extends R> task;

    /**
     * Creates a {@code NodeTaskRef} with the given properties and task action.
     *
     * @param properties the properties of the task node associated with this task.
     *   This argument cannot be {@code null}.
     * @param task the task action of the associated task node. This argument cannot be {@code null}.
     */
    public NodeTaskRef(TaskNodeProperties properties, CancelableFunction<? extends R> task) {
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(task, "task");

        this.properties = properties;
        this.task = task;
    }

    /**
     * Executes the task action and returns its output.
     * <P>
     * Note this method may only be called at most once.
     *
     * @param cancelToken the {@code CancellationToken} which might
     *   signal that the computation is to be canceled. The task
     *   may respond to such computation with an {@code OperationCanceledException}
     *   or can ignore the cancellation and complete the action. This argument
     *   cannot be {@code null}.
     * @return the output of the computation as defined by the associated task node.
     *   This method may return {@code null}, if the associated task node
     *   allows {@code null} results.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if the task
     *   detects that it was canceled (usually by checking the provided
     *   {@code CancellationToken})
     * @throws Exception thrown if some irrecoverable error occurs
     */
    public R compute(CancellationToken cancelToken) throws Exception {
        return task.execute(cancelToken);
    }

    /**
     * Returns the properties associated with the task node.
     *
     * @return the properties associated with the task node. This method
     *   never returns {@code null}.
     */
    public TaskNodeProperties getProperties() {
        return properties;
    }
}
