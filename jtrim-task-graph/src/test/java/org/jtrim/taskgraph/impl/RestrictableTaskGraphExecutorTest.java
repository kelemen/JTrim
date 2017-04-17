package org.jtrim.taskgraph.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.concurrent.CancelableFunction;
import org.jtrim.concurrent.ManualTaskExecutor;
import org.jtrim.concurrent.SyncTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.taskgraph.DependencyDag;
import org.jtrim.taskgraph.DirectedGraph;
import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.taskgraph.TaskNodeProperties;
import org.junit.Test;

import static org.junit.Assert.*;

public class RestrictableTaskGraphExecutorTest {
    private static TaskNodeKey<Object, Object> node(Object key) {
        return AbstractTaskExecutionRestrictionStrategyFactoryTest.node(key);
    }

    private void testExecution(
            DependencyDag<TaskNodeKey<?, ?>> graph,
            Iterable<? extends TaskNode<?, ?>> taskNodes,
            Consumer<? super RestrictableNodes> releaseAction) {

        AtomicReference<RestrictableNodes> restrictableNodesRef = new AtomicReference<>();

        Set<TaskNodeKey<?, ?>> notifiedMultipleTimes = new LinkedHashSet<>();

        RestrictableTaskGraphExecutor executor;
        executor = new RestrictableTaskGraphExecutor(graph, taskNodes, (taskGraph, restrictableNodes) -> {
            assertSame(graph, taskGraph);
            RestrictableNodes testRestrictableNodes = new RestrictableNodes(restrictableNodes);
            restrictableNodesRef.set(testRestrictableNodes);
            return (TaskNodeKey<?, ?> nodeKey) -> {
                if (!testRestrictableNodes.setComputed(nodeKey)) {
                    notifiedMultipleTimes.add(nodeKey);
                }
            };
        });

        executor.execute(Cancellation.UNCANCELABLE_TOKEN);

        releaseAction.accept(restrictableNodesRef.get());

        if (!notifiedMultipleTimes.isEmpty()) {
            throw new AssertionError("Nodes were notified multiple times: " + notifiedMultipleTimes);
        }
    }

    private static DependencyDag<TaskNodeKey<?, ?>> doubleSplitLeafsGraph() {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> graphBuilder = new DirectedGraph.Builder<>();
        graphBuilder.addNode(node("root"), (root) -> {
            root.addChild(node("child1"), (child1) -> {
                child1.addChild(node("child1.child1"));
                child1.addChild(node("child1.child2"));
            });
            root.addChild(node("child2"), (child2) -> {
                child2.addChild(node("child2.child1"));
                child2.addChild(node("child2.child2"));
            });
        });
        return new DependencyDag<>(graphBuilder.build());
    }

    private void testDoubleSplitGraph(
            TaskExecutor executor,
            Consumer<? super TestState> testAction) {
        DependencyDag<TaskNodeKey<?, ?>> graph = doubleSplitLeafsGraph();
        TaskNodes nodes = new TaskNodes(executor,
                "root", "child1", "child2", "child1.child1", "child1.child2", "child2.child1", "child2.child2");

        testExecution(graph, nodes.getAllNodes(), (restrictableNodes) -> {
            nodes.verifyNoneComputed();
            testAction.accept(new TestState(nodes, restrictableNodes));
            nodes.verifyAllComputed();
        });
    }

    @Test
    public void testNormalExecution() {
        testDoubleSplitGraph(SyncTaskExecutor.getSimpleExecutor(), (testState) -> {
            testState.releaseAndExpectComputed("child1.child1");
            testState.releaseAndExpectComputed("child1.child2");
            testState.releaseAndExpectComputed("child1");

            testState.releaseAndExpectComputed("child2.child1");
            testState.releaseAndExpectComputed("child2.child2");
            testState.releaseAndExpectComputed("child2");

            testState.releaseAndExpectComputed("root");
        });
    }

    private static void executeAll(ManualTaskExecutor executor) {
        while (executor.executeCurrentlySubmitted() > 0) {
            // loop until there are no more
        }
    }

    @Test
    public void testNormalAsyncExecution() {
        ManualTaskExecutor executor = new ManualTaskExecutor(false);
        testDoubleSplitGraph(executor, (testState) -> {
            executeAll(executor);

            testState.releaseAndExpectComputedAsync(executor, "child1.child1");
            testState.releaseAndExpectComputedAsync(executor, "child1.child2");
            testState.releaseAndExpectComputedAsync(executor, "child1");

            testState.releaseAndExpectComputedAsync(executor, "child2.child1");
            testState.releaseAndExpectComputedAsync(executor, "child2.child2");
            testState.releaseAndExpectComputedAsync(executor, "child2");

            testState.releaseAndExpectComputedAsync(executor, "root");
        });
    }

    @Test
    public void testLeafReleasedLast() {
        testDoubleSplitGraph(SyncTaskExecutor.getSimpleExecutor(), (testState) -> {
            testState.release("child1");
            testState.release("child2");
            testState.release("root");

            testState.nodes.verifyNoneComputed();

            testState.releaseAndExpectComputed("child1.child1");
            testState.releaseAndExpectComputed("child1.child2");
            testState.verifyComputed("child1");

            testState.releaseAndExpectComputed("child2.child1");
            testState.releaseAndExpectComputed("child2.child2");
            testState.verifyComputed("child2");
        });
    }

    @Test
    public void testSplitLeafRelease() {
        testDoubleSplitGraph(SyncTaskExecutor.getSimpleExecutor(), (testState) -> {
            testState.release("child1");
            testState.release("child2");
            testState.release("root");

            testState.nodes.verifyNoneComputed();

            testState.releaseAndExpectComputed("child1.child1");
            testState.releaseAndExpectComputed("child2.child1");

            testState.verifyNotComputed("child2");
            testState.releaseAndExpectComputed("child2.child2");
            testState.verifyComputed("child2");

            testState.verifyNotComputed("root");

            testState.verifyNotComputed("child1");
            testState.releaseAndExpectComputed("child1.child2");
            testState.verifyComputed("child1");
        });
    }

    private static DependencyDag<TaskNodeKey<?, ?>> doubleConnectedRootGraph() {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> graphBuilder = new DirectedGraph.Builder<>();

        graphBuilder.addNode(node("root1"), (root1) -> {
            root1.addChild(node("common"), (common) -> {
                common.addChild(node("common.child1"));
                common.addChild(node("common.child2"));
            });
        });

        graphBuilder.addNode(node("root2"), (root2) -> {
            root2.addChild(node("common"));
            root2.addChild(node("root2.child2"));
        });

        return new DependencyDag<>(graphBuilder.build());
    }

    private void testDoubleConnectedRootGraph(
            TaskExecutor executor,
            Consumer<? super TestState> testAction) {
        DependencyDag<TaskNodeKey<?, ?>> graph = doubleConnectedRootGraph();
        TaskNodes nodes = new TaskNodes(executor,
                "root1", "root2", "common", "common.child1", "common.child2", "root2.child2");

        testExecution(graph, nodes.getAllNodes(), (restrictableNodes) -> {
            nodes.verifyNoneComputed();
            testAction.accept(new TestState(nodes, restrictableNodes));
            nodes.verifyAllComputed();
        });
    }

    @Test
    public void testPartiallyConnectedExecution() {
        testDoubleConnectedRootGraph(SyncTaskExecutor.getSimpleExecutor(), (testState) -> {
            testState.release("common");
            testState.release("root1");
            testState.release("root2");

            testState.releaseAndExpectComputed("common.child2");
            testState.releaseAndExpectComputed("common.child1");

            testState.verifyComputed("common");
            testState.verifyComputed("root1");
            testState.verifyNotComputed("root2");

            testState.releaseAndExpectComputed("root2.child2");

            testState.verifyComputed("root2");
        });
    }

    private static final class TestState {
        private final TaskNodes nodes;
        private final RestrictableNodes restrictableNodes;

        public TestState(TaskNodes nodes, RestrictableNodes restrictableNodes) {
            this.nodes = nodes;
            this.restrictableNodes = restrictableNodes;
        }

        public void release(Object key) {
            restrictableNodes.release(key);
        }

        public void releaseButNotComputed(Object key) {
            restrictableNodes.release(key);
            nodes.verifyNotComputed(key);
        }

        public void releaseAndExpectComputed(Object key) {
            nodes.verifyNotComputed(key);
            restrictableNodes.release(key);
            nodes.verifyComputed(key);
        }

        public void releaseAndExpectComputedAsync(ManualTaskExecutor executor, Object key) {
            nodes.verifyNotComputed(key);

            restrictableNodes.release(key);
            executeAll(executor);

            nodes.verifyComputed(key);
        }

        public void verifyComputed(Object key) {
            nodes.verifyComputed(key);
        }

        public void verifyNotComputed(Object key) {
            nodes.verifyNotComputed(key);
        }
    }

    private static final class TaskNodes {
        private final Map<Object, TestTaskNode> tasks;

        public TaskNodes(TaskExecutor executor, Object... keys) {
            this.tasks = CollectionsEx.newHashMap(keys.length);

            TaskNodeProperties.Builder propertiesBuilder = new TaskNodeProperties.Builder();
            propertiesBuilder.setExecutor(executor);
            TaskNodeProperties properties = propertiesBuilder.build();

            for (Object key: keys) {
                TestTaskNode node = new TestTaskNode(node(key), properties);
                this.tasks.put(key, node);
            }
        }

        public List<TaskNode<?, ?>> getAllNodes() {
            return tasks.values().stream()
                    .map(TestTaskNode::getNode)
                    .collect(Collectors.toList());
        }

        private TestTaskNode getNode(Object key) {
            TestTaskNode node = tasks.get(key);
            if (node == null) {
                throw new AssertionError("Missing task for key: " + key);
            }
            return node;
        }

        public void verifyNoneComputed() {
            tasks.values().forEach((node) -> {
                node.verifyNotRun();
            });
        }

        public void verifyAllComputed() {
            tasks.values().forEach((node) -> {
                node.verifyRun();
            });
        }

        public void verifyNotComputed(Object key) {
            getNode(key).verifyNotRun();
        }

        public void verifyComputed(Object key) {
            getNode(key).verifyRun();
        }
    }

    private static final class TestTaskNode {
        private final TaskNode<?, ?> node;
        private final MockFunction<?> taskFunction;

        public <R, I> TestTaskNode(
                TaskNodeKey<R, I> key,
                TaskNodeProperties properties) {
            MockFunction<R> mockFunction = new MockFunction<>(key.getFactoryArg());
            this.taskFunction = mockFunction;
            this.node = new TaskNode<>(key, new NodeTaskRef<>(properties, mockFunction));
        }

        public TaskNode<?, ?> getNode() {
            return node;
        }

        public void verifyRun() {
            taskFunction.verifyRun();
        }

        public void verifyNotRun() {
            taskFunction.verifyNotRun();
        }
    }

    private static final class RestrictableNodes {
        private final Map<TaskNodeKey<?, ?>, RestrictableNode> nodes;
        private final Set<TaskNodeKey<?, ?>> computed;

        public RestrictableNodes(Iterable<? extends RestrictableNode> nodes) {
            this.computed = new HashSet<>();
            this.nodes = new HashMap<>();
            nodes.forEach((node) -> {
                this.nodes.put(node.getNodeKey(), node);
            });
        }

        public boolean setComputed(TaskNodeKey<?, ?> nodeKey) {
            return computed.add(nodeKey);
        }

        public void release(Object key) {
            RestrictableNode restrictableNode = nodes.get(node(key));
            if (restrictableNode == null) {
                throw new AssertionError("Missing restrictable node: " + key);
            }

            restrictableNode.getReleaseAction().run();
        }
    }

    private static final class MockFunction<V> implements CancelableFunction<V> {
        private final Object key;
        private final AtomicInteger callCount;

        public MockFunction(Object key) {
            this.key = key;
            this.callCount = new AtomicInteger(0);
        }

        @Override
        public V execute(CancellationToken cancelToken) throws Exception {
            callCount.incrementAndGet();
            return null;
        }

        public void verifyRun() {
            int currentCallCount = callCount.get();
            if (currentCallCount == 0) {
                throw new AssertionError("Expected a call but none received: " + key);
            }
            if (currentCallCount != 1) {
                throw new AssertionError("Expected exactly a single call but received "
                        + currentCallCount + ": " + key);
            }
        }

        public void verifyNotRun() {
            int currentCallCount = callCount.get();
            if (currentCallCount != 0) {
                throw new AssertionError("Not expected a call but received "
                        + currentCallCount + ": " + key);
            }
        }
    }
}
