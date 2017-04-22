package org.jtrim.taskgraph;

import java.util.concurrent.CompletionStage;
import org.jtrim.cancel.CancellationToken;

/**
 * Defines a executor which executes an already built task execution graph. It is not required
 * that the graph is already physically built before prior calling the {@code execute} method.
 * However, a particular instance of {@code TaskGraphExecutor} always implies an actual graph
 * (even if it was not built yet) to be executed.
 * <P>
 * It is possible to declare some nodes whose output are to be passed to the
 * {@code CompletionStage} of the task graph execution via the {@link #properties() properties()}
 * of the {@code TaskGraphExecutor}.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are not expected to be callable from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not required to be <I>synchronization transparent</I>.
 *
 * @see TaskGraphExecutors
 */
public interface TaskGraphExecutor {
    /**
     * Returns the properties used for executing the task graph. The properties must be set
     * before calling the {@link #execute(CancellationToken) execute} method.
     *
     * @return the properties used for executing the task graph. This method never returns
     *   {@code null}.
     */
    public TaskGraphExecutorProperties.Builder properties();

    /**
     * Starts executing the associated task graph and will notify the returned {@code CompletionStage}
     * once task execution terminates.
     * <P>
     * If the {@link TaskGraphExecutorProperties#isDeliverResultOnFailure() deliverResultOnFailure}
     * property is {@code true}, the {@code CompletionStage} will never be completed exceptionally.
     * However, if it is {@code false}, the possible exceptional results are:
     * <ul>
     *  <li>{@link TaskGraphExecutionException}: At least one node failed with an exception.</li>
     *  <li>
     *   {@link org.jtrim.cancel.OperationCanceledException OperationCanceledException}:
     *   The execution was canceled before it could have been completed and no nodes failed
     *   with an unexpected exception (i.e., not {@code OperationCanceledException}).
     *  </li>
     *  <li>
     *   Any other exception: When some unexpected issues prevented the task graph execution
     *   to complete.
     *  </li>
     * </ul>//TaskGraphExecutionException
     *
     * @param cancelToken the {@code CancellationToken} which can be used to cancel the execution
     *   of the task graph. The framework will make a best effort to cancel the execution.
     *   However, there is no guarantee that cancellation request will be fulfilled. If cancellation succeeds,
     *   the {@code CompletionStage} will complete exceptionally with an
     *   {@link org.jtrim.cancel.OperationCanceledException OperationCanceledException}.
     * @return the {@code CompletionStage} which is notified whenever the task graph execution terminates
     *   (normally or abnormally). This method never returns {@code null}.
     */
    public CompletionStage<TaskGraphExecutionResult> execute(CancellationToken cancelToken);
}
