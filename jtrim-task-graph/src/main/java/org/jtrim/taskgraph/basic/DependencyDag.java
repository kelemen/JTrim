package org.jtrim.taskgraph.basic;

import org.jtrim.utils.ExceptionHelper;

public final class DependencyDag<N> {
    private final DirectedGraph<N> dependencyGraph;
    private final DirectedGraph<N> forwardGraph;

    public DependencyDag(DirectedGraph<N> dependencyGraph) {
        ExceptionHelper.checkNotNullArgument(dependencyGraph, "dependencyGraph");

        dependencyGraph.checkNotCyclic();

        this.dependencyGraph = dependencyGraph;
        this.forwardGraph = dependencyGraph.reverseGraph();
    }

    public DirectedGraph<N> getDependencyGraph() {
        return dependencyGraph;
    }

    public DirectedGraph<N> getForwardGraph() {
        return forwardGraph;
    }
}
