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
import org.jtrim.event.CountDownEvent;
import org.jtrim.taskgraph.DirectedGraph;
import org.jtrim.taskgraph.TaskGraphExecutionResult;
import org.jtrim.taskgraph.TaskGraphExecutor;
import org.jtrim.taskgraph.TaskGraphExecutorProperties;
import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.utils.ExceptionHelper;

public final class EagerTaskGraphExecutor implements TaskGraphExecutor {
    private static final Logger LOGGER = Logger.getLogger(EagerTaskGraphExecutor.class.getName());

    private final DirectedGraph<TaskNodeKey<?, ?>> dependencyGraph;
    private final DirectedGraph<TaskNodeKey<?, ?>> forwardGraph;
    private final Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes;

    private final TaskGraphExecutorProperties.Builder properties;

    public EagerTaskGraphExecutor(
            DirectedGraph<TaskNodeKey<?, ?>> dependencyGraph,
            Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes) {

        this.dependencyGraph = dependencyGraph;
        this.forwardGraph = dependencyGraph.reverseGraph();

        this.nodes = new HashMap<>(nodes);
        this.properties = new TaskGraphExecutorProperties.Builder();

        this.nodes.forEach((key, value) -> {
            ExceptionHelper.checkNotNullArgument(key, "nodes.key");
            ExceptionHelper.checkNotNullArgument(value, "nodes.value");
        });
    }

    @Override
    public TaskGraphExecutorProperties.Builder properties() {
        return properties;
    }

    @Override
    public CompletionStage<TaskGraphExecutionResult> execute(CancellationToken cancelToken) {
        dependencyGraph.checkNotCyclic();

        GraphExecutor executor = new GraphExecutor(
                cancelToken, properties.build(), nodes, dependencyGraph, forwardGraph);
        return executor.execute();
    }

    private static final class GraphExecutor {
        private final TaskGraphExecutorProperties properties;
        private final ConcurrentMap<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes;

        private final DirectedGraph<TaskNodeKey<?, ?>> dependencyGraph;
        private final DirectedGraph<TaskNodeKey<?, ?>> forwardGraph;

        private volatile boolean errored;
        private volatile boolean canceled;
        private final CompletableFuture<TaskGraphExecutionResult> executeResult;

        private final CancellationSource cancel;

        public GraphExecutor(
                CancellationToken cancelToken,
                TaskGraphExecutorProperties properties,
                Map<TaskNodeKey<?, ?>, TaskNode<?, ?>> nodes,
                DirectedGraph<TaskNodeKey<?, ?>> dependencyGraph,
                DirectedGraph<TaskNodeKey<?, ?>> forwardGraph) {

            this.properties = properties;
            this.nodes = new ConcurrentHashMap<>(nodes);
            this.dependencyGraph = dependencyGraph;
            this.forwardGraph = forwardGraph;

            this.errored = false;
            this.canceled = false;
            this.executeResult = new CompletableFuture<>();
            this.cancel = Cancellation.createChildCancellationSource(cancelToken);
        }

        private Collection<TaskNodeKey<?, ?>> getEndNodes() {
            Collection<TaskNodeKey<?, ?>> result = new ArrayList<>();

            nodes.keySet().forEach((key) -> {
                if (!forwardGraph.hasChildren(key)) {
                    result.add(key);
                }
            });

            return result;
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
                    if (!node.hasResult()) {
                        canceled = true;
                        finishForwardNodes(node.getKey());
                    }

                    completeEvent.dec();
                    removeNode(node.getKey());
                });
            });

            getEndNodes().forEach(this::scheduleNode);
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

        private void ensureScheduled(TaskNode<?, ?> node) {
            node.ensureScheduleComputed(getCancelToken(), this::onError);
        }

        private void scheduleNode(TaskNodeKey<?, ?> nodeKey) {
            TaskNode<?, ?> node = nodes.get(nodeKey);
            if (node != null) {
                scheduleNode(node);
            }
        }

        private void scheduleNode(TaskNode<?, ?> node) {
            Collection<TaskNode<?, ?>> dependencies = getDependencies(node.getKey());
            if (dependencies.isEmpty()) {
                ensureScheduled(node);
                return;
            }

            CountDownEvent doneEvent = new CountDownEvent(dependencies.size(), () -> {
                ensureScheduled(node);
            });
            dependencies.forEach((dependency) -> {
                dependency.addOnComputed(doneEvent::dec);
                scheduleNode(dependency);
            });
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
