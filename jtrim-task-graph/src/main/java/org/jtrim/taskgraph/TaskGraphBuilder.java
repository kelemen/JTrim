package org.jtrim.taskgraph;

import java.util.concurrent.CompletionStage;
import org.jtrim.cancel.CancellationToken;

/**
 * Defines the initial nodes of the task graph. Additional nodes will be automatically
 * created based on the input needs of the task nodes. That is, {@link #addNode(TaskNodeKey) adding a task node key}
 * will force the graph execution framework to create a task node with factory specified by the key; when
 * the factory defines inputs for the action it creates, it will recursively spawn new nodes.
 *
 * <h3>Thread safety</h3>
 * The methods of this interface may not be used by multiple threads concurrently, unless otherwise noted.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not <I>synchronization transparent</I> in general.
 *
 * @see TaskGraphDefConfigurer
 * @see TaskGraphExecutor
 */
public interface TaskGraphBuilder {
    /**
     * Adds a node to the task execution graph. Adding a node will force the execution framework
     * to at least execute that node.
     * <P>
     * It is allowed to call {@code addNode} concurrently with another {@code addNode} call. However,
     * the {@code addNode} method may not be called concurrently with any other method of this
     * interface.
     *
     * @param nodeKey the {@code TaskNodeKey} identifying the task node to be created. The
     *   task node key must not be added multiple times and there must be a task node factory
     *   able to create nodes based on this key. This argument cannot be {@code null}.
     */
    public void addNode(TaskNodeKey<?, ?> nodeKey);

    /**
     * Returns the properties used when building the task graph. The properties must be set
     * before calling the {@link #buildGraph(CancellationToken) buildGraph} method.
     *
     * @return the properties used when building the task graph. This method never returns
     *   {@code null}.
     */
    public TaskGraphBuilderProperties.Builder properties();

    /**
     * Starts building a task graph and will notify the returned {@code CompletionStage} once the
     * graph is built and is ready to be executed.
     * <P>
     * Adding further nodes after calling this method does not affect the {@code TaskGraphExecutor}
     * to be created.
     *
     * @param cancelToken the {@code CancellationToken} which can be used to cancel the building
     *   of the task graph. The framework will make a best effort to cancel the building of the task graph.
     *   However, there is no guarantee that cancellation request will be fulfilled. If cancellation succeeds,
     *   the {@code CompletionStage} will complete exceptionally with an
     *   {@link org.jtrim.cancel.OperationCanceledException OperationCanceledException}.
     * @return the {@code CompletionStage} which can be used to receive the {@code TaskGraphExecutor} and
     *   actually start the task graph execution. This method never returns {@code null}.
     */
    public CompletionStage<TaskGraphExecutor> buildGraph(CancellationToken cancelToken);
}
