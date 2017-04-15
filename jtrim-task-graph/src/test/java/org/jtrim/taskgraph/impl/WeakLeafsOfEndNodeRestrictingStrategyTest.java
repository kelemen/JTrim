package org.jtrim.taskgraph.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.taskgraph.DependencyDag;
import org.jtrim.taskgraph.DirectedGraph;
import org.jtrim.taskgraph.TaskNodeKey;
import org.junit.Test;

public class WeakLeafsOfEndNodeRestrictingStrategyTest extends AbstractTaskExecutionRestrictionStrategyFactoryTest {
    public WeakLeafsOfEndNodeRestrictingStrategyTest() {
        super(() -> new TaskExecutionRestrictionStrategyFactory[]{
            create(1),
            create(2),
            create(3),
            create(1000),
        });
    }

    private static WeakLeafsOfEndNodeRestrictingStrategy create(int maxRetainedLeafNodes) {
        WeakLeafsOfEndNodeRestrictingStrategy builder = new WeakLeafsOfEndNodeRestrictingStrategy(maxRetainedLeafNodes);
        builder.setQueueSorter((queue) -> {
            List<TaskNodeKey<?, ?>> sortedQueue = new ArrayList<>(queue);
            sortedQueue.sort((key1, key2) -> {
                Object arg1 = key1.getFactoryArg();
                Object arg2 = key2.getFactoryArg();
                return CollectionsEx.naturalOrder().compare(arg1, arg2);
            });
            return sortedQueue;
        });
        return builder;
    }

    private static RestrictableNode restrictedNode(Object key, Runnable releaseTask) {
        return new RestrictableNode(node(key), releaseTask);
    }

    private static void restrictableNodes(
            DirectedGraph<TaskNodeKey<?, ?>> graph,
            TaskNodeKey<?, ?> parent,
            Map<Object, RestrictableNode> result) {

        Object key = parent.getFactoryArg();
        result.putIfAbsent(key, restrictedNode(key, new ReleaseTask(key)));

        graph.getChildren(parent).forEach((child) -> {
            restrictableNodes(graph, child, result);
        });
    }

    private static Map<Object, RestrictableNode> restrictableNodes(DependencyDag<TaskNodeKey<?, ?>> graph) {
        Map<Object, RestrictableNode> result = new HashMap<>();
        graph.getDependencyGraph().getRawGraph().keySet().forEach((node) -> {
            restrictableNodes(graph.getDependencyGraph(), node, result);
        });
        return result;
    }

    private static ReleaseTask getReleaseTask(Map<Object, RestrictableNode> nodes, Object key) {
        RestrictableNode node = nodes.get(key);
        return (ReleaseTask)node.getReleaseAction();
    }

    private static void verifyReleased(Map<Object, RestrictableNode> nodes, Object key) {
        getReleaseTask(nodes, key).verifyCalled();
    }

    private static void verifyNotReleased(Map<Object, RestrictableNode> nodes, Object key) {
        getReleaseTask(nodes, key).verifyNotCalled();
    }

    private void testSingleRoot(int maxRetainedLeafNodes) {
        WeakLeafsOfEndNodeRestrictingStrategy strategyBuilder = create(maxRetainedLeafNodes);

        DirectedGraph.Builder<TaskNodeKey<?, ?>> graphBuilder = new DirectedGraph.Builder<>();

        TaskNodeKey<?, ?> root = node("root");
        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> children = graphBuilder.addNode(root);
        children.addChild(node("child1"));
        children.addChild(node("child2"));

        DependencyDag<TaskNodeKey<?, ?>> graph = new DependencyDag<>(graphBuilder.build());
        Map<Object, RestrictableNode> restrictableNodes = restrictableNodes(graph);

        strategyBuilder.buildStrategy(graph, restrictableNodes.values());
        verifyReleased(restrictableNodes, "child1");
        verifyReleased(restrictableNodes, "child2");
        verifyReleased(restrictableNodes, "root");
    }

    @Test
    public void testSingleRoot() {
        testSingleRoot(1);
        testSingleRoot(2);
        testSingleRoot(3);
        testSingleRoot(10);
    }

    private static DependencyDag<TaskNodeKey<?, ?>> doubleRootGraph() {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> graphBuilder = new DirectedGraph.Builder<>();

        TaskNodeKey<?, ?> root1 = node("root1");
        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> root1Children = graphBuilder.addNode(root1);
        root1Children.addChild(node("root1.child1"));
        root1Children.addChild(node("root1.child2"));

        TaskNodeKey<?, ?> root2 = node("root2");
        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> root2Children = graphBuilder.addNode(root2);
        root2Children.addChild(node("root2.child1"));
        root2Children.addChild(node("root2.child2"));

        return new DependencyDag<>(graphBuilder.build());
    }

    private void testDoubleRoot(int maxRetainedLeafNodes) {
        WeakLeafsOfEndNodeRestrictingStrategy strategyBuilder = create(maxRetainedLeafNodes);

        DependencyDag<TaskNodeKey<?, ?>> graph = doubleRootGraph();
        Map<Object, RestrictableNode> restrictableNodes = restrictableNodes(graph);

        TaskExecutionRestrictionStrategy strategy = strategyBuilder.buildStrategy(graph, restrictableNodes.values());

        verifyReleased(restrictableNodes, "root1");
        verifyReleased(restrictableNodes, "root2");

        verifyReleased(restrictableNodes, "root1.child1");
        verifyReleased(restrictableNodes, "root1.child2");

        strategy.setNodeComputed(node("root1.child1"));
        strategy.setNodeComputed(node("root1.child2"));

        verifyNotReleased(restrictableNodes, "root2.child1");
        verifyNotReleased(restrictableNodes, "root2.child2");

        strategy.setNodeComputed(node("root1"));

        verifyReleased(restrictableNodes, "root1.child1");
        verifyReleased(restrictableNodes, "root1.child2");

        verifyReleased(restrictableNodes, "root2.child1");
        verifyReleased(restrictableNodes, "root2.child2");

        verifyReleased(restrictableNodes, "root1");
        verifyReleased(restrictableNodes, "root2");
    }

    @Test
    public void testDoubleRoot() {
        testDoubleRoot(1);
        testDoubleRoot(2);
    }

    private void testSingleShotDoubleRoot(int maxRetainedLeafNodes) {
        WeakLeafsOfEndNodeRestrictingStrategy strategyBuilder = create(maxRetainedLeafNodes);

        DependencyDag<TaskNodeKey<?, ?>> graph = doubleRootGraph();
        Map<Object, RestrictableNode> restrictableNodes = restrictableNodes(graph);

        strategyBuilder.buildStrategy(graph, restrictableNodes.values());

        verifyReleased(restrictableNodes, "root1.child1");
        verifyReleased(restrictableNodes, "root1.child2");

        verifyReleased(restrictableNodes, "root2.child1");
        verifyReleased(restrictableNodes, "root2.child2");

        verifyReleased(restrictableNodes, "root1");
        verifyReleased(restrictableNodes, "root2");
    }

    @Test
    public void testSingleShotDoubleRoot() {
        testSingleShotDoubleRoot(10);
        testSingleShotDoubleRoot(3);
    }

    private static DependencyDag<TaskNodeKey<?, ?>> doubleConnectedRootGraph(String name1, String name2) {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> graphBuilder = new DirectedGraph.Builder<>();

        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> root1Children = graphBuilder.addNode(node(name1));
        root1Children.addChild(node("common"));

        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> commonChildren = graphBuilder.addNode(node("common"));
        commonChildren.addChild(node("common.child1"));
        commonChildren.addChild(node("common.child2"));

        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> root2Children = graphBuilder.addNode(node(name2));
        root2Children.addChild(node("common"));
        root2Children.addChild(node(name2 + ".child2"));

        return new DependencyDag<>(graphBuilder.build());
    }

    private void testConnectedDoubleRoot1(int maxRetainedLeafNodes) {
        WeakLeafsOfEndNodeRestrictingStrategy strategyBuilder = create(maxRetainedLeafNodes);

        DependencyDag<TaskNodeKey<?, ?>> graph = doubleConnectedRootGraph("root1", "root2");
        Map<Object, RestrictableNode> restrictableNodes = restrictableNodes(graph);

        TaskExecutionRestrictionStrategy strategy = strategyBuilder.buildStrategy(graph, restrictableNodes.values());

        verifyReleased(restrictableNodes, "root1");
        verifyReleased(restrictableNodes, "root2");
        verifyReleased(restrictableNodes, "common");

        verifyReleased(restrictableNodes, "common.child1");
        verifyReleased(restrictableNodes, "common.child2");

        strategy.setNodeComputed(node("common.child1"));
        strategy.setNodeComputed(node("common.child2"));

        verifyNotReleased(restrictableNodes, "root2.child2");

        strategy.setNodeComputed(node("root1"));

        verifyReleased(restrictableNodes, "root2.child2");
    }

    @Test
    public void testConnectedDoubleRoot1() {
        testConnectedDoubleRoot1(1);
        testConnectedDoubleRoot1(2);
    }

    private void testConnectedDoubleRoot2(int maxRetainedLeafNodes) {
        WeakLeafsOfEndNodeRestrictingStrategy strategyBuilder = create(maxRetainedLeafNodes);

        DependencyDag<TaskNodeKey<?, ?>> graph = doubleConnectedRootGraph("root2", "root1");
        Map<Object, RestrictableNode> restrictableNodes = restrictableNodes(graph);

        strategyBuilder.buildStrategy(graph, restrictableNodes.values());

        verifyReleased(restrictableNodes, "root1");
        verifyReleased(restrictableNodes, "root2");
        verifyReleased(restrictableNodes, "common");

        verifyReleased(restrictableNodes, "root1.child2");

        verifyReleased(restrictableNodes, "common.child1");
        verifyReleased(restrictableNodes, "common.child2");
    }

    @Test
    public void testConnectedDoubleRoot2() {
        testConnectedDoubleRoot2(1);
    }

    private static DependencyDag<TaskNodeKey<?, ?>> semiConnectedEndGraph(String name2, String name3) {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> graphBuilder = new DirectedGraph.Builder<>();

        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> root1Children = graphBuilder.addNode(node("root1"));
        root1Children.addChild(node("common"));

        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> commonChildren = graphBuilder.addNode(node("common"));
        commonChildren.addChild(node("common.child1"));
        commonChildren.addChild(node("common.child2"));

        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> root2Children = graphBuilder.addNode(node(name2));
        root2Children.addChild(node("common"));
        root2Children.addChild(node(name2 + ".child2"));

        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> root3Children = graphBuilder.addNode(node(name3));
        root3Children.addChild(node(name3 + ".child1"));
        root3Children.addChild(node(name3 + ".child2"));

        return new DependencyDag<>(graphBuilder.build());
    }

    private void testPreferSemiStarted(int maxRetainedLeafNodes) {
        WeakLeafsOfEndNodeRestrictingStrategy strategyBuilder = create(maxRetainedLeafNodes);

        DependencyDag<TaskNodeKey<?, ?>> graph = semiConnectedEndGraph("root2", "root3");
        Map<Object, RestrictableNode> restrictableNodes = restrictableNodes(graph);

        TaskExecutionRestrictionStrategy strategy = strategyBuilder.buildStrategy(graph, restrictableNodes.values());

        verifyReleased(restrictableNodes, "root1");
        verifyReleased(restrictableNodes, "root2");
        verifyReleased(restrictableNodes, "root3");
        verifyReleased(restrictableNodes, "common");

        verifyReleased(restrictableNodes, "common.child1");
        verifyReleased(restrictableNodes, "common.child2");

        strategy.setNodeComputed(node("common.child1"));
        strategy.setNodeComputed(node("common.child2"));

        verifyNotReleased(restrictableNodes, "root2.child2");
        verifyNotReleased(restrictableNodes, "root3.child1");
        verifyNotReleased(restrictableNodes, "root3.child2");

        strategy.setNodeComputed(node("root1"));

        verifyReleased(restrictableNodes, "root2.child2");
        verifyNotReleased(restrictableNodes, "root3.child1");
        verifyNotReleased(restrictableNodes, "root3.child2");

        strategy.setNodeComputed(node("root2.child2"));
        strategy.setNodeComputed(node("root2"));

        verifyReleased(restrictableNodes, "root3.child1");
        verifyReleased(restrictableNodes, "root3.child2");
    }

    @Test
    public void testPreferSemiStarted() {
        testPreferSemiStarted(1);
        testPreferSemiStarted(2);
    }

    private static final class ReleaseTask implements Runnable {
        private final Object key;
        private final AtomicInteger callCount;
        private volatile Throwable lastCall;

        public ReleaseTask(Object key) {
            this.key = key;
            this.callCount = new AtomicInteger(0);
        }

        public void verifyNotCalled() {
            int currentCallCount = callCount.get();
            if (currentCallCount != 0) {
                throw new AssertionError("Task " + key
                        + " must not have been called but was called " + currentCallCount + " times", lastCall);
            }
        }

        public void verifyCalled() {
            int currentCallCount = callCount.get();
            if (currentCallCount != 1) {
                throw new AssertionError("Task " + key
                        + " must have been called exactly once but was called " + currentCallCount + " times",
                        lastCall);
            }
        }

        public boolean isCalled() {
            return callCount.get() > 0;
        }

        @Override
        public void run() {
            lastCall = new Exception();
            callCount.incrementAndGet();
        }
    }
}
