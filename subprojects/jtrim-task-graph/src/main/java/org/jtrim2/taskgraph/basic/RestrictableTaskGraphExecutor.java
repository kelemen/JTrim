package org.jtrim2.taskgraph.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.CountDownEvent;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.taskgraph.BuiltGraph;
import org.jtrim2.taskgraph.ExecutionResultType;
import org.jtrim2.taskgraph.TaskGraphExecutionException;
import org.jtrim2.taskgraph.TaskGraphExecutionResult;
import org.jtrim2.taskgraph.TaskGraphExecutor;
import org.jtrim2.taskgraph.TaskGraphExecutorProperties;
import org.jtrim2.taskgraph.TaskNodeKey;
import org.jtrim2.taskgraph.TaskSkippedException;

/**
 * Defines an implementation of {@code TaskGraphExecutor} allowing to restrict task node
 * execution using an externally provided custom {@link TaskExecutionRestrictionStrategy strategy}.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are not expected to be callable from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are not <I>synchronization transparent</I>.
 *
 * @see TaskExecutionRestrictionStrategies
 */
public final class RestrictableTaskGraphExecutor implements TaskGraphExecutor {
    private static final Logger LOGGER = Logger.getLogger(RestrictableTaskGraphExecutor.class.getName());

    private final AtomicReference<StaticInput> staticInputRef;
    private final TaskGraphExecutorProperties.Builder properties;

    /**
     * Creates a new {@code RestrictableTaskGraphExecutor} with the given task graph,
     * nodes and task execution restriction strategy.
     *
     * @param graph the task execution graph defining the dependencies between task nodes.
     *   This argument cannot be {@code null}.
     * @param nodes the task nodes to be executed. The execution must honor the dependencies
     *   defined by the task graph. This argument cannot be {@code null} and may not contain
     *   {@code null} elements.
     * @param restrictionStrategyFactory the strategy which may restrict some nodes from being
     *   executed to prevent too much concurrent resource consumption. This argument cannot
     *   be {@code null}.
     */
    public RestrictableTaskGraphExecutor(
            DependencyDag<TaskNodeKey<?, ?>> graph,
            Iterable<? extends TaskNode<?, ?>> nodes,
            TaskExecutionRestrictionStrategyFactory restrictionStrategyFactory) {
        this.staticInputRef = new AtomicReference<>(new StaticInput(graph, nodes, restrictionStrategyFactory));
        this.properties = new TaskGraphExecutorProperties.Builder();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskGraphExecutorProperties.Builder properties() {
        return properties;
    }

    private void verifyNotExecuted(StaticInput staticInput) {
        if (staticInput == null) {
            throw new IllegalStateException("Already executed.");
        }
    }

    private StaticInput getStaticInput() {
        StaticInput staticInput = staticInputRef.get();
        verifyNotExecuted(staticInput);
        return staticInput;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public BuiltGraph getBuiltGraph() {
        StaticInput staticInput = getStaticInput();

        return new BuiltGraph(staticInput.nodes.keySet(), staticInput.graph);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <R> CompletionStage<R> futureOf(TaskNodeKey<R, ?> nodeKey) {
        Objects.requireNonNull(nodeKey, "nodeKey");

        StaticInput staticInput = getStaticInput();

        @SuppressWarnings("unchecked")
        TaskNode<R, ?> node = (TaskNode<R, ?>) staticInput.nodes.get(nodeKey);
        if (node == null) {
            throw new IllegalArgumentException("Unknown node: " + nodeKey);
        }

        return node.taskFuture();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public CompletionStage<TaskGraphExecutionResult> execute(CancellationToken cancelToken) {
        StaticInput staticInput = staticInputRef.getAndSet(null);
        verifyNotExecuted(staticInput);

        GraphExecutor executor = new GraphExecutor(cancelToken, properties.build(), staticInput);
        return executor.execute();
    }

    private static final class GraphExecutor {
        private final TaskGraphExecutorProperties properties;
        private final Map<TaskNodeKey<?, ?>, CompletableFuture<?>> requestedResults;
        private final ConcurrentMap<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes;

        private final DependencyDag<TaskNodeKey<?, ?>> graph;
        private final DirectedGraph<TaskNodeKey<?, ?>> dependencyGraph;
        private final DirectedGraph<TaskNodeKey<?, ?>> forwardGraph;

        private final TaskExecutionRestrictionStrategyFactory restrictionStrategyFactory;
        private TaskExecutionRestrictionStrategy restrictionStrategy;

        private volatile boolean errored;
        private volatile boolean canceled;
        private final CompletableFuture<TaskGraphExecutionResult> executeResult;

        private final CancellationSource cancel;

        private final TaskExecutor recursionBreakerExecutor;

        public GraphExecutor(
                CancellationToken cancelToken,
                TaskGraphExecutorProperties properties,
                StaticInput staticInput) {

            this.properties = properties;
            this.requestedResults = new ConcurrentHashMap<>();
            this.nodes = staticInput.nodes;
            this.graph = staticInput.graph;
            this.dependencyGraph = graph.getDependencyGraph();
            this.forwardGraph = graph.getForwardGraph();

            this.errored = false;
            this.canceled = false;
            this.executeResult = new CompletableFuture<>();
            this.cancel = Cancellation.createChildCancellationSource(cancelToken);
            this.restrictionStrategyFactory = staticInput.restrictionStrategyFactory;
            this.restrictionStrategy = null;
            this.recursionBreakerExecutor = TaskExecutors.syncNonRecursiveExecutor();
        }

        private void execute0() {
            List<TaskNode<?, ?>> allNodes = new ArrayList<>(nodes.values());
            if (allNodes.isEmpty()) {
                finish();
                return;
            }

            CountDownEvent completeEvent = new CountDownEvent(allNodes.size(), this::finish);
            allNodes.forEach((node) -> {
                node.taskFuture().whenComplete((result, error) -> completeNode(node, error, completeEvent));
            });

            scheduleAllNodes();
        }

        private void completeNode(TaskNode<?, ?> node, Throwable error, CountDownEvent completeEvent) {
            recursionBreakerExecutor.execute(() -> {
                completeNodeNow(node, error, completeEvent);
            });
        }

        private void completeNodeNow(TaskNode<?, ?> node, Throwable error, CountDownEvent completeEvent) {
            try {
                TaskNodeKey<?, ?> nodeKey = node.getKey();

                if (!node.hasResult()) {
                    if (!(AsyncTasks.unwrap(error) instanceof TaskSkippedException)) {
                        canceled = true;
                    }
                    finishForwardNodes(nodeKey, error);
                }

                removeNode(node.getKey());
                restrictionStrategy.setNodeComputed(nodeKey);
            } finally {
                completeEvent.dec();
            }
        }

        private void finishForwardNodes(TaskNodeKey<?, ?> key, Throwable error) {
            forwardGraph.getChildren(key).forEach((childKey) -> {
                try {
                    TaskNode<?, ?> child = nodes.get(childKey);
                    if (child != null) {
                        child.propagateFailure(error);
                    }
                } catch (Throwable ex) {
                    onError(key, ex);
                }
            });
        }

        public CompletionStage<TaskGraphExecutionResult> execute() {
            execute0();
            return executeResult;
        }

        private void finish() {
            if (properties.isDeliverResultOnFailure()) {
                deliverResults();
            } else if (errored) {
                executeResult.completeExceptionally(
                        TaskGraphExecutionException.withoutStackTrace("Computation failed", null));
            } else if (canceled) {
                executeResult.completeExceptionally(OperationCanceledException.withoutStackTrace());
            } else {
                deliverResults();
            }
        }

        private void deliverResults() {
            ExecutionResultType resultType = errored
                    ? ExecutionResultType.ERRORED
                    : (canceled ? ExecutionResultType.CANCELED : ExecutionResultType.SUCCESS);

            Set<TaskNodeKey<?, ?>> resultNodeKeys = properties.getResultNodeKeys();
            executeResult.complete(new MapTaskGraphExecutionResult(resultType, resultNodeKeys, requestedResults));
        }

        private void onError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
            try {
                errored = true;

                if (properties.isStopOnFailure()) {
                    cancel.getController().cancel();
                }

                properties.getComputeErrorHandler().onError(nodeKey, error);
            } catch (Throwable subError) {
                subError.addSuppressed(error);
                LOGGER.log(Level.SEVERE, "Error while computing node: " + nodeKey, subError);
            }
        }

        private void scheduleAllNodes() {
            List<TaskNode<?, ?>> allNodes = new ArrayList<>(nodes.values());
            List<RestrictableNode> restrictableNodes = new ArrayList<>(allNodes.size());
            List<Runnable> releaseActions = new ArrayList<>(allNodes.size());

            allNodes.forEach((node) -> {
                TaskNodeKey<?, ?> nodeKey = node.getKey();

                Collection<TaskNode<?, ?>> dependencies = getDependencies(nodeKey);
                CountDownEvent doneEvent = new CountDownEvent(dependencies.size() + 2, () -> {
                    ensureScheduled(node);
                });

                Runnable releaseOnceAction = doneEvent::dec;
                restrictableNodes.add(new RestrictableNode(nodeKey, Tasks.runOnceTask(releaseOnceAction)));
                releaseActions.add(releaseOnceAction);

                dependencies.forEach((dependency) -> {
                    Runnable doRelease = Tasks.runOnceTask(releaseOnceAction);
                    dependency.taskFuture().thenAccept((result) -> doRelease.run());
                });
            });

            assert restrictionStrategy == null;
            restrictionStrategy = restrictionStrategyFactory.buildStrategy(graph, restrictableNodes);

            releaseActions.forEach(Runnable::run);
        }

        private void ensureScheduled(TaskNode<?, ?> node) {
            node.ensureScheduleComputed(getCancelToken(), this::onError);
        }

        private Collection<TaskNode<?, ?>> getDependencies(TaskNodeKey<?, ?> nodeKey) {
            Set<TaskNodeKey<?, ?>> keys = dependencyGraph.getChildren(nodeKey);
            if (keys.isEmpty()) {
                return Collections.emptySet();
            }

            Collection<TaskNode<?, ?>> result = new ArrayList<>(keys.size());
            keys.forEach((key) -> {
                TaskNode<?, ?> dependency = nodes.get(key);
                if (dependency != null) {
                    result.add(dependency);
                }
            });
            return result;
        }

        private void removeNode(TaskNodeKey<?, ?> nodeKey) {
            TaskNode<?, ?> node = nodes.remove(nodeKey);
            if (node != null && properties.getResultNodeKeys().contains(nodeKey)) {
                requestedResults.put(nodeKey, node.taskFuture());
            }
        }

        private CancellationToken getCancelToken() {
            return cancel.getToken();
        }
    }

    private static final class MapTaskGraphExecutionResult implements TaskGraphExecutionResult {
        private static final CompletableFuture<Void> NONE = CompletableFuture.completedFuture(null);

        private final ExecutionResultType executionResultType;
        private final Set<TaskNodeKey<?, ?>> allowedKeys;
        private final Map<TaskNodeKey<?, ?>, CompletableFuture<?>> results;

        public MapTaskGraphExecutionResult(
                ExecutionResultType executionResultType,
                Set<TaskNodeKey<?, ?>> allowedKeys,
                Map<TaskNodeKey<?, ?>, CompletableFuture<?>> results) {

            this.executionResultType = executionResultType;
            this.allowedKeys = allowedKeys;
            this.results = results;
        }

        @Override
        public ExecutionResultType getResultType() {
            return executionResultType;
        }

        @Override
        public <R> R getResult(TaskNodeKey<R, ?> key) {
            Objects.requireNonNull(key, "key");

            if (!allowedKeys.contains(key)) {
                throw new IllegalArgumentException("Key was not requested as a result: " + key);
            }

            CompletableFuture<?> resultFuture = results.getOrDefault(key, NONE);
            Class<R> resultType = key.getFactoryKey().getResultType();
            return resultType.cast(TaskNode.getExpectedResultNow(key, resultFuture));
        }
    }

    private static final class StaticInput {
        public final DependencyDag<TaskNodeKey<?, ?>> graph;
        public final ConcurrentMap<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes;
        public final TaskExecutionRestrictionStrategyFactory restrictionStrategyFactory;

        public StaticInput(
                DependencyDag<TaskNodeKey<?, ?>> graph,
                Iterable<? extends TaskNode<?, ?>> nodes,
                TaskExecutionRestrictionStrategyFactory restrictionStrategyFactory) {
            this.graph = Objects.requireNonNull(graph, "graph");
            this.nodes = copyNodes(Objects.requireNonNull(nodes, "nodes"));
            this.restrictionStrategyFactory = restrictionStrategyFactory;
        }

        private static ConcurrentMap<TaskNodeKey<?, ?>, TaskNode<?, ?>> copyNodes(
                Iterable<? extends TaskNode<?, ?>> nodes) {

            ConcurrentMap<TaskNodeKey<?, ?>, TaskNode<?, ?>> result = new ConcurrentHashMap<>();
            nodes.forEach((node) -> {
                Objects.requireNonNull(node, "nodes[?]");
                result.put(node.getKey(), node);
            });
            return result;
        }
    }
}
