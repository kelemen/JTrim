package org.jtrim.taskgraph.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import org.jtrim.concurrent.Tasks;
import org.jtrim.taskgraph.TaskNodeKey;
import org.jtrim.utils.ExceptionHelper;

final class WeakLeafsOfEndNodeRestrictingStrategy implements TaskExecutionRestrictionStrategyFactory {
    private final int maxRetainedLeafNodes;

    // This is simply here to allow creating deterministic tests.
    private Function<Collection<TaskNodeKey<?, ?>>, Collection<TaskNodeKey<?, ?>>> queueSorter;

    public WeakLeafsOfEndNodeRestrictingStrategy(int maxRetainedLeafNodes) {
        ExceptionHelper.checkArgumentInRange(maxRetainedLeafNodes, 1, Integer.MAX_VALUE, "maxRetainedLeafNodes");
        this.maxRetainedLeafNodes = maxRetainedLeafNodes;
        this.queueSorter = queue -> queue;
    }

    @Override
    public TaskExecutionRestrictionStrategy buildStrategy(
            DependencyDag<TaskNodeKey<?, ?>> taskGraph,
            Iterable<? extends RestrictableNode> restrictableNodes) {

        StrategyImpl strategy = new StrategyImpl(
                maxRetainedLeafNodes,
                taskGraph,
                restrictableNodes,
                queueSorter);

        strategy.scheduleUnsafe();
        return strategy;
    }

    void setQueueSorter(Function<Collection<TaskNodeKey<?, ?>>, Collection<TaskNodeKey<?, ?>>> queueSorter) {
        ExceptionHelper.checkNotNullArgument(queueSorter, "queueSorter");
        this.queueSorter = queueSorter;
    }

    private static void selectLeafAndEndNodes(
            DependencyDag<TaskNodeKey<?, ?>> graph,
            Iterable<? extends RestrictableNode> restrictableNodes,
            Map<TaskNodeKey<?, ?>, Runnable> leafNodes,
            Collection<TaskNodeKey<?, ?>> endNodes) {

        restrictableNodes.forEach((restrictableNode) -> {
            TaskNodeKey<?, ?> nodeKey = restrictableNode.getNodeKey();
            if (graph.getDependencyGraph().hasChildren(nodeKey)) {
                restrictableNode.release();
            }
            else {
                leafNodes.put(nodeKey, Tasks.runOnceTask(restrictableNode.getReleaseAction(), false));
            }

            if (!graph.getForwardGraph().hasChildren(nodeKey)) {
                endNodes.add(nodeKey);
            }
        });
    }

    private static <N> void addMissingEndNodes(DependencyDag<N> taskGraph, Collection<? super N> endNodes) {
        DirectedGraph<N> forwardGraph = taskGraph.getForwardGraph();
        taskGraph.getDependencyGraph().getRawGraph().keySet().forEach((nodeKey) -> {
            if (!forwardGraph.hasChildren(nodeKey)) {
                endNodes.add(nodeKey);
            }
        });
    }

    private static final class StrategyImpl implements TaskExecutionRestrictionStrategy {
        private final int maxRetainedLeafNodes;

        private final Lock mainLock;
        private final Map<TaskNodeKey<?, ?>, Runnable> leafNodes;
        private final Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> endNodesToLeafs;
        private final Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> leafsToEndNodes;

        private final Set<TaskNodeKey<?, ?>> endNodeQueue;
        private final Set<TaskNodeKey<?, ?>> computedEndNodes;
        private final Set<TaskNodeKey<?, ?>> releasedNotComputedEndNodes;
        private final Set<TaskNodeKey<?, ?>> retainingNotComputedEndNodes;
        private final Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> scheduledLeafNodes;

        public StrategyImpl(
                int maxRetainedLeafNodes,
                DependencyDag<TaskNodeKey<?, ?>> taskGraph,
                Iterable<? extends RestrictableNode> restrictableNodes,
                Function<Collection<TaskNodeKey<?, ?>>, Collection<TaskNodeKey<?, ?>>> queueSorter) {

            this.leafNodes = new HashMap<>();

            Collection<TaskNodeKey<?, ?>> endNodes = new LinkedHashSet<>();
            selectLeafAndEndNodes(taskGraph, restrictableNodes, this.leafNodes, endNodes);
            addMissingEndNodes(taskGraph, endNodes);

            List<TaskNodeKey<?, ?>> sortedLeafs
                    = GraphUtils.sortRecursively(taskGraph.getDependencyGraph(), endNodes, leafNodes.keySet());

            this.endNodesToLeafs = taskGraph.getForwardGraph().getAllLeafToRootNodes(sortedLeafs);
            this.leafsToEndNodes = taskGraph.getDependencyGraph().getAllLeafToRootNodes(endNodes);

            this.mainLock = new ReentrantLock();
            this.maxRetainedLeafNodes = maxRetainedLeafNodes;

            this.endNodeQueue = new LinkedHashSet<>(queueSorter.apply(endNodesToLeafs.keySet()));
            this.computedEndNodes = new HashSet<>();
            this.retainingNotComputedEndNodes = new LinkedHashSet<>();
            this.releasedNotComputedEndNodes = new HashSet<>();
            this.scheduledLeafNodes = new HashMap<>();
        }

        private static <N> N pollCollection(Collection<? extends N> src) {
            if (src.isEmpty()) {
                return null;
            }

            Iterator<? extends N> itr = src.iterator();
            N result = itr.next();
            itr.remove();
            return result;
        }

        private TaskNodeKey<?, ?> pollNextEndNode() {
            TaskNodeKey<?, ?> candidateEndNode = pollCollection(retainingNotComputedEndNodes);
            if (candidateEndNode == null) {
                return pollCollection(endNodeQueue);
            }
            else {
                endNodeQueue.remove(candidateEndNode);
                return candidateEndNode;
            }
        }

        private void scheduleOne(List<Runnable> releaseTasks) {
            TaskNodeKey<?, ?> candidateEndNode = pollNextEndNode();
            if (candidateEndNode == null) {
                return;
            }

            if (!computedEndNodes.contains(candidateEndNode)) {
                releasedNotComputedEndNodes.add(candidateEndNode);
            }

            Set<TaskNodeKey<?, ?>> candidateLeafs
                    = endNodesToLeafs.getOrDefault(candidateEndNode, Collections.emptySet());
            addScheduledLeafs(candidateLeafs);
            candidateLeafs.forEach((leaf) -> {
                Runnable releaseTask = leafNodes.remove(leaf);
                if (releaseTask != null) {
                    releaseTasks.add(releaseTask);
                }
            });
        }

        private void addScheduledLeafs(Set<TaskNodeKey<?, ?>> newLeafs) {
            newLeafs.forEach(this::addScheduledLeaf);
        }

        private void addScheduledLeaf(TaskNodeKey<?, ?> leafKey) {
            Set<TaskNodeKey<?, ?>> scheduledRetainingEndNodes
                    = scheduledLeafNodes.computeIfAbsent(leafKey, (key) -> new HashSet<>());

            Set<TaskNodeKey<?, ?>> retainingEndNodes
                    = leafsToEndNodes.getOrDefault(leafKey, Collections.emptySet());
            retainingEndNodes.forEach((retainingEndNode) -> {
                if (!computedEndNodes.contains(retainingEndNode)) {
                    if (!releasedNotComputedEndNodes.contains(retainingEndNode)) {
                        retainingNotComputedEndNodes.add(retainingEndNode);
                    }
                    scheduledRetainingEndNodes.add(retainingEndNode);
                }
            });

            if (scheduledRetainingEndNodes.isEmpty()) {
                scheduledLeafNodes.remove(leafKey);
            }
        }

        private boolean removeLeafNode(TaskNodeKey<?, ?> endNode, TaskNodeKey<?, ?> leaf) {
            Set<TaskNodeKey<?, ?>> retainingEndNodes = scheduledLeafNodes.get(leaf);
            if (retainingEndNodes == null) {
                // This should never happen.
                return false;
            }

            retainingEndNodes.remove(endNode);
            if (retainingEndNodes.isEmpty()) {
                scheduledLeafNodes.remove(leaf);
                return true;
            }
            else {
                return false;
            }
        }

        private void removeLeafNodes(
                TaskNodeKey<?, ?> endNode,
                Set<TaskNodeKey<?, ?>> leafs) {
            leafs.forEach((leaf) -> {
                removeLeafNode(endNode, leaf);
            });
        }

        private void scheduleUnsafe(List<Runnable> releaseTasks) {
            if (releasedNotComputedEndNodes.isEmpty()) {
                // If we do not have any end node released to be executed,
                // we must release at least one to avoid failing to release anything.
                scheduleOne(releaseTasks);
            }

            // FIXME: This may result in nodes never being released
            while (!endNodeQueue.isEmpty() && scheduledLeafNodes.size() < maxRetainedLeafNodes) {
                scheduleOne(releaseTasks);
            }
        }

        public void scheduleUnsafe() {
            List<Runnable> releaseTasks = new ArrayList<>();
            scheduleUnsafe(releaseTasks);
            releaseTasks.forEach(Runnable::run);
        }

        @Override
        public void setNodeComputed(TaskNodeKey<?, ?> nodeKey) {
            Set<TaskNodeKey<?, ?>> leafs = endNodesToLeafs.get(nodeKey);
            if (leafs == null) {
                return;
            }

            List<Runnable> releaseTasks = new ArrayList<>();

            mainLock.lock();
            try {
                computedEndNodes.add(nodeKey);
                releasedNotComputedEndNodes.remove(nodeKey);
                retainingNotComputedEndNodes.remove(nodeKey);

                removeLeafNodes(nodeKey, leafs);
                scheduleUnsafe(releaseTasks);
            } finally {
                mainLock.unlock();
            }

            releaseTasks.forEach(Runnable::run);
        }
    }
}
