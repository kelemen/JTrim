package org.jtrim2.taskgraph.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.CountDownEvent;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.taskgraph.TaskNodeKey;
import org.junit.Test;

import static org.jtrim2.taskgraph.basic.TestNodes.*;

public abstract class AbstractTaskExecutionRestrictionStrategyFactoryTest {
    private final Supplier<TaskExecutionRestrictionStrategyFactory[]> strategyFactories;

    public AbstractTaskExecutionRestrictionStrategyFactoryTest(
            Supplier<TaskExecutionRestrictionStrategyFactory[]> strategyFactories) {
        this.strategyFactories = strategyFactories;
    }

    private void doTest(TestMethod test) {
        for (TaskExecutionRestrictionStrategyFactory factory: strategyFactories.get()) {
            test.doTest(factory);
        }
    }

    public DependencyDag<TaskNodeKey<?, ?>> getRandomGraph(Random random, int width, int depth) {
        DirectedGraph.Builder<TaskNodeKey<?, ?>> graphBuilder = new DirectedGraph.Builder<>();

        for (int i = 0; i < depth - 1; i++) {
            for (int j = 0; j < width; j++) {
                TaskNodeKey<?, ?> parent = matrixNode(i, j);
                int childrenCount = random.nextInt(width / 3);
                for (int childIndex = 0; childIndex < childrenCount; childIndex++) {
                    int childColumn = random.nextInt(width);
                    graphBuilder.addNode(parent).addChild(matrixNode(i + 1, childColumn));
                }
            }
        }

        return new DependencyDag<>(graphBuilder.build());
    }

    private Map<TaskNodeKey<?, ?>, Runnable> setupNodes(
            DependencyDag<TaskNodeKey<?, ?>> graph,
            int width,
            int depth,
            Supplier<TaskExecutionRestrictionStrategy> strategyRef,
            TaskExecutor nodeComputerExecutor,
            List<RestrictableNode> restrictableNodes) {

        Map<TaskNodeKey<?, ?>, Runnable> uncomputed = new ConcurrentHashMap<>(width * depth);
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < width; j++) {
                TaskNodeKey<?, ?> node = matrixNode(i, j);
                Runnable releaseTask = setupNode(graph, node, nodeComputerExecutor, strategyRef, uncomputed);
                restrictableNodes.add(new RestrictableNode(node, Tasks.runOnceTask(releaseTask)));
            }
        }
        return uncomputed;
    }

    private Runnable setupNode(
            DependencyDag<TaskNodeKey<?, ?>> graph,
            TaskNodeKey<?, ?> node,
            TaskExecutor nodeComputerExecutor,
            Supplier<TaskExecutionRestrictionStrategy> strategyRef,
            Map<TaskNodeKey<?, ?>, Runnable> uncomputed) {

        Set<TaskNodeKey<?, ?>> dependencies = graph.getDependencyGraph().getChildren(node);
        CountDownEvent computeEvent = new CountDownEvent(dependencies.size() + 2, () -> {
            nodeComputerExecutor.execute(() -> {
                strategyRef.get().setNodeComputed(node);

                uncomputed.remove(node);
                graph.getForwardGraph().getChildren(node).forEach((parent) -> {
                    Runnable parentReleaseTask = uncomputed.get(parent);
                    parentReleaseTask.run();
                });
            });
        });

        uncomputed.put(node, computeEvent::dec);

        return computeEvent::dec;
    }

    private void testGraphExecution(
            DependencyDag<TaskNodeKey<?, ?>> graph,
            int width,
            int depth,
            TaskExecutor executor,
            ExecutionVerification verification) {

        doTest((strategyFactory) -> {
            List<RestrictableNode> restrictableNodes = new ArrayList<>(width * depth);
            AtomicReference<TaskExecutionRestrictionStrategy> strategyRef = new AtomicReference<>();
            Map<TaskNodeKey<?, ?>, Runnable> uncomputed
                    = setupNodes(graph, width, depth, strategyRef::get, executor, restrictableNodes);

            TaskExecutionRestrictionStrategy strategy = strategyFactory.buildStrategy(graph, restrictableNodes);
            strategyRef.set(strategy);

            uncomputed.values().forEach(Runnable::run);

            verification.verify(uncomputed);
        });
    }

    private void verifyNoUncomputed(Map<TaskNodeKey<?, ?>, Runnable> uncomputed) {
        if (!uncomputed.isEmpty()) {
            throw new AssertionError("The following nodes remained uncomputed ("
                    + uncomputed.size() + "): " + uncomputed.keySet());
        }
    }

    private void testRandomGraphExecutionSync(Random random, int width, int depth) {
        DependencyDag<TaskNodeKey<?, ?>> graph = getRandomGraph(random, width, depth);
        testGraphExecution(graph, width, depth, SyncTaskExecutor.getSimpleExecutor(), this::verifyNoUncomputed);
    }

    @Test
    public void testRandomGraphExecutionSync() {
        testRandomGraphExecutionSync(new Random(346432535), 10, 30);
    }

    private void testRandomGraphExecutionAsync(Random random, int width, int depth) {
        ManualTaskExecutor executor = new ManualTaskExecutor(false);
        DependencyDag<TaskNodeKey<?, ?>> graph = getRandomGraph(random, width, depth);
        testGraphExecution(graph, width, depth, executor, (uncomputed) -> {
            while (executor.executeCurrentlySubmitted() > 0) {
                // loop while we have anything to execute
            }

            verifyNoUncomputed(uncomputed);
        });
    }

    @Test
    public void testRandomGraphExecutionAsync() {
        testRandomGraphExecutionAsync(new Random(752364375), 10, 30);
    }

    private interface ExecutionVerification {
        public void verify(Map<TaskNodeKey<?, ?>, Runnable> uncomputed);
    }

    private interface TestMethod {
        public void doTest(TaskExecutionRestrictionStrategyFactory strategyFactory);
    }
}
