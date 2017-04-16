package org.jtrim.taskgraph.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.Tasks;
import org.jtrim.event.CountDownEvent;
import org.jtrim.taskgraph.DependencyDag;
import org.jtrim.taskgraph.DirectedGraph;
import org.jtrim.taskgraph.TaskGraphExecutionResult;
import org.jtrim.taskgraph.TaskGraphExecutor;
import org.jtrim.taskgraph.TaskGraphExecutorProperties;
import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.utils.ExceptionHelper;

public final class RestrictableTaskGraphExecutor implements TaskGraphExecutor {
    private static final Logger LOGGER = Logger.getLogger(RestrictableTaskGraphExecutor.class.getName());

    private final DependencyDag<TaskNodeKey<?, ?>> graph;
    private final Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes;

    private final TaskGraphExecutorProperties.Builder properties;
    private final TaskExecutionRestrictionStrategyFactory restrictionStrategyFactory;

    public RestrictableTaskGraphExecutor(
            DependencyDag<TaskNodeKey<?, ?>> graph,
            Iterable<? extends TaskNode<?, ?>> nodes,
            TaskExecutionRestrictionStrategyFactory restrictionStrategyFactory) {
        ExceptionHelper.checkNotNullArgument(graph, "graph");
        ExceptionHelper.checkNotNullArgument(nodes, "nodes");
        ExceptionHelper.checkNotNullArgument(restrictionStrategyFactory, "restrictionStrategyFactory");

        this.graph = graph;
        this.nodes = copyNodes(nodes);

        this.properties = new TaskGraphExecutorProperties.Builder();
        this.restrictionStrategyFactory = restrictionStrategyFactory;
    }

    private static Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> copyNodes(Iterable<? extends TaskNode<?, ?>> nodes) {
        Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> result = new HashMap<>();
        nodes.forEach((node) -> {
            ExceptionHelper.checkNotNullArgument(node, "nodes[?]");
            result.put(node.getKey(), node);
        });
        return result;
    }

    @Override
    public TaskGraphExecutorProperties.Builder properties() {
        return properties;
    }

    @Override
    public CompletionStage<TaskGraphExecutionResult> execute(CancellationToken cancelToken) {
        GraphExecutor executor = new GraphExecutor(
                cancelToken, properties.build(), nodes, graph, restrictionStrategyFactory);
        return executor.execute();
    }

    private static final class GraphExecutor {
        private final TaskGraphExecutorProperties properties;
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

        public GraphExecutor(
                CancellationToken cancelToken,
                TaskGraphExecutorProperties properties,
                Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes,
                DependencyDag<TaskNodeKey<?, ?>> graph,
                TaskExecutionRestrictionStrategyFactory restrictionStrategyFactory) {

            this.properties = properties;
            this.nodes = new ConcurrentHashMap<>(nodes);
            this.graph = graph;
            this.dependencyGraph = graph.getDependencyGraph();
            this.forwardGraph = graph.getForwardGraph();

            this.errored = false;
            this.canceled = false;
            this.executeResult = new CompletableFuture<>();
            this.cancel = Cancellation.createChildCancellationSource(cancelToken);
            this.restrictionStrategyFactory = restrictionStrategyFactory;
            this.restrictionStrategy = null;
        }

        private void execute0() {
            List<TaskNode<?, ?>> allNodes = new ArrayList<>(nodes.values());
            if (allNodes.isEmpty()) {
                finish();
                return;
            }

            CountDownEvent completeEvent = new CountDownEvent(allNodes.size(), this::finish);
            allNodes.forEach((node) -> {
                node.addOnFinished(() -> {
                    TaskNodeKey<?, ?> nodeKey = node.getKey();

                    if (!node.hasResult()) {
                        canceled = true;
                        finishForwardNodes(nodeKey);
                    }

                    completeEvent.dec();
                    removeNode(node.getKey());

                    restrictionStrategy.setNodeComputed(nodeKey);
                });
            });

            scheduleAllNodes();
        }

        private void finishForwardNodes(TaskNodeKey<?, ?> key) {
            forwardGraph.getChildren(key).forEach((childKey) -> {
                try {
                    TaskNode<?, ?> child = nodes.get(childKey);
                    if (child != null) {
                        child.finish();
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
            TaskGraphExecutionResult.Builder result = new TaskGraphExecutionResult.Builder();
            result.setErrored(errored);
            result.setFullyCompleted(!canceled);
            executeResult.complete(result.build());
        }

        private void onError(TaskNodeKey<?, ?> nodeKey, Throwable error) {
            try {
                errored = true;

                if (properties.isStopOnFailure()) {
                    cancel.getController().cancel();
                }

                properties.getComputeErrorHandler().onError(nodeKey, error);
            } catch (Throwable subError) {
                if (subError instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
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
                restrictableNodes.add(new RestrictableNode(nodeKey, Tasks.runOnceTask(releaseOnceAction, false)));
                releaseActions.add(releaseOnceAction);

                dependencies.forEach((dependency) -> {
                    dependency.addOnComputed(releaseOnceAction);
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
            nodes.remove(nodeKey);
        }

        private CancellationToken getCancelToken() {
            return cancel.getToken();
        }
    }
}
