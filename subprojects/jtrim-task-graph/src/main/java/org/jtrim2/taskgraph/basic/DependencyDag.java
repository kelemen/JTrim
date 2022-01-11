package org.jtrim2.taskgraph.basic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.jtrim2.collections.CollectionsEx;

/**
 * Defines a directed acyclic graph (DAG). {@code DependencyDag} allows
 * requesting the edges in both direction for easy traversal.
 *
 * <h2>Thread safety</h2>
 * Instances of {@code DependencyDag} are immutable (assuming the nodes themselves
 * are immutable) and so can be used safely by multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of {@code DependencyDag} are <I>synchronization transparent</I>.
 *
 * @param <N> the type of the node. The nodes are distinguished based on their
 *   {@code equals}.
 */
public final class DependencyDag<N> {
    private final DirectedGraph<N> dependencyGraph;
    private final DirectedGraph<N> forwardGraph;

    /**
     * Creates a {@code DependencyDag} from a directed graph where the edges point
     * from dependent node to their dependencies.
     *
     * @param dependencyGraph the directed graph where the edges point
     *   from dependent node to their dependencies. This argument cannot be
     *   {@code null}. The passed graph must be acyclic.
     */
    public DependencyDag(DirectedGraph<N> dependencyGraph) {
        this(dependencyGraph, dependencyGraph.reverseGraph());

        dependencyGraph.checkNotCyclic();
    }

    private DependencyDag(DirectedGraph<N> dependencyGraph, DirectedGraph<N> forwardGraph) {
        this.dependencyGraph = Objects.requireNonNull(dependencyGraph, "dependencyGraph");
        this.forwardGraph = Objects.requireNonNull(forwardGraph, "forwardGraph");
    }

    /**
     * Returns the directed graph where the edges point from dependent node to their dependencies.
     *
     * @return the directed graph where the edges point from dependent node to their dependencies.
     *   This method never returns {@code null}.
     */
    public DirectedGraph<N> getDependencyGraph() {
        return dependencyGraph;
    }

    /**
     * Returns the directed graph where the edges point from the dependency to dependent node.
     *
     * @return the directed graph where the edges point from the dependency to dependent node.
     *   This method never returns {@code null}.
     */
    public DirectedGraph<N> getForwardGraph() {
        return forwardGraph;
    }

    /**
     * Returns a map, mapping the leaf nodes (nodes having no children) to nodes they are reachable where the
     * image of the mapping only contains nodes specified in the argument. If multiple leaf nodes are
     * reachable from a particular node, that particular node will be listed under all the reachable
     * leaf nodes.
     * <P>
     * The nodes from which a particular leaf is reachable are listed in the same order as
     * they appear in the node list given in the argument.
     * <P>
     * There will not be entry in the map for leaf nodes not reachable from any of the given
     * nodes.
     *
     * @param rootNodes the nodes to be considered from where leaf nodes are reachable from.
     *   This argument cannot be {@code null} and may not contain {@code null} elements.
     * @return  a map, mapping the leaf nodes (nodes having no children) to nodes they are
     *   reachable where the image of the mapping only contains nodes specified in the argument.
     *   This method never returns {@code null}.
     */
    public Map<N, List<N>> getAllLeafToRootNodesOrdered(Iterable<? extends N> rootNodes) {
        Map<N, Integer> order = new HashMap<>();
        int index = Integer.MIN_VALUE;
        for (N root : rootNodes) {
            order.put(root, index);
            index++;
        }
        return getAllLeafToRootNodes(order.keySet(), rootsOfLeaf -> {
            List<N> ordered = new ArrayList<>(rootsOfLeaf);
            ordered.sort((a, b) -> Integer.compare(order.get(a), order.get(b)));
            return ordered;
        });
    }

    /**
     * Returns a map, mapping the leaf nodes (nodes having no children) to nodes they are reachable where the
     * image of the mapping only contains nodes specified in the argument. If multiple leaf nodes are
     * reachable from a particular node, that particular node will be listed under all the reachable
     * leaf nodes.
     * <P>
     * There will not be entry in the map for leaf nodes not reachable from any of the given
     * nodes.
     *
     * @param rootNodes the nodes to be considered from where leaf nodes are reachable from.
     *   This argument cannot be {@code null} and may not contain {@code null} elements.
     * @return  a map, mapping the leaf nodes (nodes having no children) to nodes they are
     *   reachable where the image of the mapping only contains nodes specified in the argument.
     *   This method never returns {@code null}.
     */
    public Map<N, Set<N>> getAllLeafToRootNodes(Iterable<? extends N> rootNodes) {
        return getAllLeafToRootNodes(rootNodes, Function.identity());
    }

    private <C extends Collection<N>> Map<N, C> getAllLeafToRootNodes(
            Iterable<? extends N> rootNodes,
            Function<Set<N>, C> rootSorter) {

        Set<N> reachable = getDependencyGraph().getReachableNodes(rootNodes);

        Map<N, ForwardingDef<N>> forwardingDefs = new HashMap<>();

        Deque<N> toProcess = new ArrayDeque<>();
        rootNodes.forEach(node -> {
            ForwardingDef<N> def = new ForwardingDef<>();
            def.init(this, node, reachable, true);
            forwardingDefs.put(node, def);
            toProcess.add(node);
        });

        Map<N, C> result = new HashMap<>();
        for (N node = toProcess.pollLast(); node != null; node = toProcess.pollLast()) {
            ForwardingDef<N> forwardingDef = forwardingDefs.remove(node);
            assert forwardingDef != null : "Queued node without ForwardingDef";

            Set<N> dependencies = dependencyGraph.getChildren(node);
            if (dependencies.isEmpty()) {
                result.put(node, rootSorter.apply(forwardingDef.roots));
                continue;
            }

            N currentNode = node; // To be usable in the lambda
            dependencies.forEach(dependency -> {
                ForwardingDef<N> dependencyDef = forwardingDefs
                        .computeIfAbsent(dependency, key -> new ForwardingDef<>());

                dependencyDef.init(this, dependency, reachable, false);

                if (dependencyDef.addRoots(currentNode, forwardingDef.roots)) {
                    toProcess.add(dependency);
                }
            });
        }

        return result;
    }

    /**
     * Returns a {@code DependencyDag} having edges in the opposite direction than this
     * {@code DependencyDag}. That is, its {@link #getForwardGraph() forward graph}
     * and {@link #getDependencyGraph() dependency graph} are reversed.
     *
     * @return a {@code DependencyDag} having edges in the opposite direction than this
     *   {@code DependencyDag}. This method never returns {@code null}.
     */
    public DependencyDag<N> reverse() {
        return new DependencyDag<>(forwardGraph, dependencyGraph);
    }

    private static final class ForwardingDef<N> {
        private Set<N> roots;
        private Set<N> remainingDependants;

        public void init(DependencyDag<N> dag, N node, Set<N> reachable, boolean leaf) {
            if (roots != null) {
                // Already initialized.
                return;
            }

            roots = new HashSet<>();
            if (leaf) {
                roots.add(node);
            }

            Set<N> dependants = dag.getForwardGraph().getChildren(node);
            remainingDependants = CollectionsEx.newHashSet(dependants.size());
            dependants.forEach(dependant -> {
                if (reachable.contains(dependant)) {
                    remainingDependants.add(dependant);
                }
            });
        }

        public boolean addRoots(N dependant, Set<N> newRoots) {
            roots.addAll(newRoots);

            remainingDependants.remove(dependant);
            return remainingDependants.isEmpty();
        }
    }
}
