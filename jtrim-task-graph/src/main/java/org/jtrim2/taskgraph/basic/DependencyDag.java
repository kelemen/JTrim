package org.jtrim2.taskgraph.basic;

import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines a directed acyclic graph (DAG). {@code DependencyDag} allows
 * requesting the edges in both direction for easy traversal.
 *
 * <h3>Thread safety</h3>
 * Instances of {@code DependencyDag} are immutable (assuming the nodes themselves
 * are immutable) and so can be used safely by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
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
        ExceptionHelper.checkNotNullArgument(dependencyGraph, "dependencyGraph");

        dependencyGraph.checkNotCyclic();

        this.dependencyGraph = dependencyGraph;
        this.forwardGraph = dependencyGraph.reverseGraph();
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
}
