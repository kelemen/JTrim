package org.jtrim2.taskgraph;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jtrim2.taskgraph.basic.DependencyDag;

import static java.util.Collections.*;
import static org.jtrim2.utils.ExceptionHelper.*;

/**
 * Defines a whole task execution graph. That is, the set of nodes and the
 * edges.
 *
 * <h3>Thread safety</h3>
 * Instances of {@code BuiltGraph} are immutable and so can be used safely by
 * multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of {@code BuiltGraph} are <I>synchronization transparent</I>.
 */
public final class BuiltGraph {
    private final Set<TaskNodeKey<?, ?>> nodes;
    private final DependencyDag<TaskNodeKey<?, ?>> graph;

    /**
     * Creates a new task execution graph with the given edges and nodes.
     *
     * @param nodes specifies the set of all the nodes of the task execution
     *   graph. This argument cannot be {@code null} and cannot contain
     *   {@code null} elements.
     * @param graph defines the edges of the task execution graph. That is,
     *   the dependencies between the nodes. This argument cannot be {@code null}.
     */
    public BuiltGraph(
            Set<TaskNodeKey<?, ?>> nodes,
            DependencyDag<TaskNodeKey<?, ?>> graph) {
        this.nodes = unmodifiableSet(checkNotNullElements(new HashSet<>(nodes), "nodes"));
        this.graph = Objects.requireNonNull(graph, "graph");
    }

    /**
     * Returns the nodes of the task execution graph.
     *
     * @return the nodes of the task execution graph. The returned set is never
     *   {@code null} and is not modifiable.
     */
    public Set<TaskNodeKey<?, ?>> getNodes() {
        return nodes;
    }

    /**
     * Returns the edges of the task execution graph. That is, the dependencies
     * between the nodes.
     *
     * @return the edges of the task execution graph. This method never returns
     *   {@code null}.
     */
    public DependencyDag<TaskNodeKey<?, ?>> getGraph() {
        return graph;
    }
}
