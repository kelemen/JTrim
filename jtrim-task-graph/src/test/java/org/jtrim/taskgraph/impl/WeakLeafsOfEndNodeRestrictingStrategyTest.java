package org.jtrim.taskgraph.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.jtrim.collections.ArraysEx;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.taskgraph.DependencyDag;
import org.jtrim.taskgraph.DirectedGraph;
import org.jtrim.taskgraph.TaskNodeKey;
import org.junit.Test;

import static org.junit.Assert.*;

public class WeakLeafsOfEndNodeRestrictingStrategyTest extends AbstractTaskExecutionRestrictionStrategyFactoryTest {
    public WeakLeafsOfEndNodeRestrictingStrategyTest() {
        super(() -> new TaskExecutionRestrictionStrategyFactory[]{
            create(1),
            create(2),
            create(3),
            create(1000),
        });
    }

    private static int cmpNodeFactoryArg(TaskNodeKey<?, ?> key1, TaskNodeKey<?, ?> key2) {
        Object arg1 = key1.getFactoryArg();
        Object arg2 = key2.getFactoryArg();
        return CollectionsEx.naturalOrder().compare(arg1, arg2);
    }

    private static void sortByFactoryArg(List<TaskNodeKey<?, ?>> nodes) {
        nodes.sort(WeakLeafsOfEndNodeRestrictingStrategyTest::cmpNodeFactoryArg);
    }

    private static WeakLeafsOfEndNodeRestrictingStrategy create(int maxRetainedLeafNodes) {
        WeakLeafsOfEndNodeRestrictingStrategy builder = new WeakLeafsOfEndNodeRestrictingStrategy(maxRetainedLeafNodes);
        builder.setQueueSorter((queue) -> {
            List<TaskNodeKey<?, ?>> sortedQueue = new ArrayList<>(queue);
            sortByFactoryArg(sortedQueue);
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
            Map<Object, RestrictableNode> result,
            Consumer<Object> releaseCollector) {

        Object key = parent.getFactoryArg();
        result.putIfAbsent(key, restrictedNode(key, new ReleaseTask(key, releaseCollector)));

        graph.getChildren(parent).forEach((child) -> {
            restrictableNodes(graph, child, result, releaseCollector);
        });
    }

    private static Map<Object, RestrictableNode> restrictableNodes(
            DependencyDag<TaskNodeKey<?, ?>> graph,
            Consumer<Object> releaseCollector) {
        Map<Object, RestrictableNode> result = new HashMap<>();
        graph.getDependencyGraph().getRawGraph().keySet().forEach((node) -> {
            restrictableNodes(graph.getDependencyGraph(), node, result, releaseCollector);
        });
        return result;
    }

    private static Map<Object, RestrictableNode> restrictableNodes(DependencyDag<TaskNodeKey<?, ?>> graph) {
        return restrictableNodes(graph, (key) -> { });
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

    private void testPreferSemiStarted(int maxRetainedLeafNodes, String root2, String root3) {
        WeakLeafsOfEndNodeRestrictingStrategy strategyBuilder = create(maxRetainedLeafNodes);

        DependencyDag<TaskNodeKey<?, ?>> graph = semiConnectedEndGraph(root2, root3);
        Map<Object, RestrictableNode> restrictableNodes = restrictableNodes(graph);

        TaskExecutionRestrictionStrategy strategy = strategyBuilder.buildStrategy(graph, restrictableNodes.values());

        verifyReleased(restrictableNodes, "root1");
        verifyReleased(restrictableNodes, root2);
        verifyReleased(restrictableNodes, root3);
        verifyReleased(restrictableNodes, "common");

        verifyReleased(restrictableNodes, "common.child1");
        verifyReleased(restrictableNodes, "common.child2");

        strategy.setNodeComputed(node("common.child1"));
        strategy.setNodeComputed(node("common.child2"));

        verifyNotReleased(restrictableNodes, root2 + ".child2");
        verifyNotReleased(restrictableNodes, root3 + ".child1");
        verifyNotReleased(restrictableNodes, root3 + ".child2");

        strategy.setNodeComputed(node("root1"));

        verifyReleased(restrictableNodes, root2 + ".child2");
        verifyNotReleased(restrictableNodes, root3 + ".child1");
        verifyNotReleased(restrictableNodes, root3 + ".child2");

        strategy.setNodeComputed(node(root2 + ".child2"));
        strategy.setNodeComputed(node(root2));

        verifyReleased(restrictableNodes, root3 + ".child1");
        verifyReleased(restrictableNodes, root3 + ".child2");
    }

    private void testPreferSemiStarted(String root2, String root3) {
        testPreferSemiStarted(1, root2, root3);
        testPreferSemiStarted(2, root2, root3);
    }

    @Test
    public void testPreferSemiStarted1() {
        testPreferSemiStarted("root2", "root3");
    }

    @Test
    public void testPreferSemiStarted2() {
        testPreferSemiStarted("root3", "root2");
    }

    private static DependencyDag<TaskNodeKey<?, ?>> threeLevelSeparatedLeafs() {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> graphBuilder = new DirectedGraph.Builder<>();

        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> root1Children = graphBuilder.addNode(node("root"));
        root1Children.addChild(node("child1"));
        root1Children.addChild(node("child2"));

        int leafCount = 30;

        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> child1Children = graphBuilder.addNode(node("child1"));
        for (int i = 0; i < leafCount; i++) {
            child1Children.addChild(node("leaf.child1." + i));
        }

        DirectedGraph.ChildrenBuilder<TaskNodeKey<?, ?>> child2Children = graphBuilder.addNode(node("child2"));
        for (int i = 0; i < leafCount; i++) {
            child2Children.addChild(node("leaf.child2." + i));
        }

        return new DependencyDag<>(graphBuilder.build());
    }

    private void assertAllSame(Collection<?> src) {
        Object first = src.iterator().next();
        src.forEach((entry) -> {
            assertEquals(first, entry);
        });
    }

    private void testSeparatedLeafReleaseOrder(int maxRetainedLeafNodes) {
        WeakLeafsOfEndNodeRestrictingStrategy strategyBuilder = create(maxRetainedLeafNodes);

        List<Object> releaseOrder = new ArrayList<>();
        DependencyDag<TaskNodeKey<?, ?>> graph = threeLevelSeparatedLeafs();
        Map<Object, RestrictableNode> restrictableNodes = restrictableNodes(graph, releaseOrder::add);

        strategyBuilder.buildStrategy(graph, restrictableNodes.values());

        String[] releasedLeafs = releaseOrder.stream()
                .map(Object::toString)
                .filter(name -> name.startsWith("leaf."))
                .map(name -> name.substring("leaf.".length()))
                .map(name -> name.substring(0, name.lastIndexOf('.')))
                .toArray(length -> new String[length]);

        assertEquals("releasedLeafs.length", 60, releasedLeafs.length);

        int halfLeafSize = releasedLeafs.length / 2;
        List<String> part1 = ArraysEx.viewAsList(releasedLeafs, 0, halfLeafSize);
        List<String> part2 = ArraysEx.viewAsList(releasedLeafs, halfLeafSize, halfLeafSize);

        assertNotEquals(part1.get(0), part2.get(0));
        assertAllSame(part1);
        assertAllSame(part2);
    }

    @Test
    public void testSeparatedLeafReleaseOrder() {
        testSeparatedLeafReleaseOrder(1);
        testSeparatedLeafReleaseOrder(4);
        testSeparatedLeafReleaseOrder(10);
    }

    private static final class ReleaseTask implements Runnable {
        private final Object key;
        private final AtomicInteger callCount;
        private volatile Throwable lastCall;
        private final Consumer<Object> releaseCollector;

        public ReleaseTask(Object key, Consumer<Object> releaseCollector) {
            this.key = key;
            this.callCount = new AtomicInteger(0);
            this.releaseCollector = releaseCollector;
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

            releaseCollector.accept(key);
        }
    }
}
