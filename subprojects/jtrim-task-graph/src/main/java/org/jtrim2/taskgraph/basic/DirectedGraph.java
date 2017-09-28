package org.jtrim2.taskgraph.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines a directed graph without any solitary nodes. That is, this class
 * only defines the edges in a directed graph and cannot define nodes without
 * part of an edge.
 * <P>
 * An instance of {@code DirectedGraph} via its {@link DirectedGraph.Builder}.
 *
 * <h3>Thread safety</h3>
 * Instances of {@code DirectedGraph} are immutable (assuming the nodes themselves
 * are immutable) and so can be used safely by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of {@code DirectedGraph} are <I>synchronization transparent</I>.
 *
 * @param <N> the type of the node. The nodes are distinguished based on their
 *   {@code equals}.
 */
public final class DirectedGraph<N> {
    private final Map<N, Set<N>> childrenGraph;

    private DirectedGraph(Builder<N> builder) {
        this(copy(builder.childrenGraph));
    }

    private DirectedGraph(Map<N, Set<N>> childrenGraph) {
        this.childrenGraph = childrenGraph;
    }

    /**
     * Checks if this graph is acyclic or not, throwing an exception if
     * this graph is not acyclic.
     *
     * @throws IllegalStateException thrown if this graph is not acyclic.
     */
    public void checkNotCyclic() {
        Map<N, Set<N>> graph = new LinkedHashMap<>(childrenGraph);
        while (!graph.isEmpty()) {
            N key = graph.keySet().iterator().next();
            checkNotCyclic(Collections.singleton(key), new LinkedHashSet<>(), graph);
        }
    }

    private static <N> void checkNotCyclic(
            Set<N> startNodes,
            Set<N> visited,
            Map<N, Set<N>> graph) {

        startNodes.forEach((key) -> {
            if (visited.contains(key)) {
                List<N> cycle = new ArrayList<>(visited.size() + 1);
                cycle.addAll(visited);
                cycle.add(key);

                cycle = afterFirstMatch(key, cycle);

                throw new IllegalStateException("The graph is cyclic: " + cycle);
            }

            Set<N> dependencies = graph.get(key);
            if (dependencies != null) {
                visited.add(key);
                checkNotCyclic(dependencies, visited, graph);
                visited.remove(key);
            }
            graph.remove(key);
        });
    }

    private static <T> List<T> afterFirstMatch(T match, List<T> list) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (match.equals(list.get(i))) {
                return list.subList(i, size);
            }
        }
        return Collections.emptyList();
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
    public Map<N, Set<N>> getAllLeafToRootNodes(Iterable<? extends N> rootNodes) {
        return getAllLeafToRootNodes(rootNodes, LinkedHashSet::new);
    }

    /**
     * Returns a map, mapping the leaf nodes (nodes having no children) to nodes they are reachable where the
     * image of the mapping only contains nodes specified in the argument. If multiple leaf nodes are
     * reachable from a particular node, that particular node will be listed under all the reachable
     * leaf nodes.
     * <P>
     * The nodes from which a particular leaf is reachable will be added to the image sets in the same
     * order as they appear in the node list given in the argument.
     * <P>
     * There will not be entry in the map for leaf nodes not reachable from any of the given
     * nodes.
     *
     * @param rootNodes the nodes to be considered from where leaf nodes are reachable from.
     *   This argument cannot be {@code null} and may not contain {@code null} elements.
     * @param newSetFactory a factory creating a empty, mutable sets for the result map.
     *   This argument cannot be {@code null}.
     * @return  a map, mapping the leaf nodes (nodes having no children) to nodes they are
     *   reachable where the image of the mapping only contains nodes specified in the argument.
     *   This method never returns {@code null}.
     */
    public Map<N, Set<N>> getAllLeafToRootNodes(
            Iterable<? extends N> rootNodes,
            Supplier<? extends Set<N>> newSetFactory) {

        Function<N, Set<N>> keyBasedSetFactory = (key) -> newSetFactory.get();

        Map<N, Set<N>> result = new LinkedHashMap<>();
        rootNodes.forEach((root) -> {
            addLeafToRootNodes(root, result, keyBasedSetFactory);
        });
        return result;
    }

    private void addLeafToRootNodes(
            N root,
            Map<N, Set<N>> result,
            Function<N, Set<N>> newSetFactory) {
        addLeafToRootNodes(root, root, result, newSetFactory);
    }

    private void addLeafToRootNodes(
            N root,
            N currentNode,
            Map<N, Set<N>> result,
            Function<N, Set<N>> newSetFactory) {

        Set<N> children = getChildren(currentNode);
        if (children.isEmpty()) {
            Set<N> roots = result.computeIfAbsent(currentNode, newSetFactory);
            roots.add(root);
        } else {
            children.forEach((key) -> {
                addLeafToRootNodes(root, key, result, newSetFactory);
            });
        }
    }

    /**
     * Returns a map, mapping parent nodes to their direct children. The returned map
     * does not contain mapping for nodes having no children.
     * <P>
     * The returned map cannot be modified.
     *
     * @return a map, mapping parent nodes to their direct children. This method
     *   never returns {@code null}.
     */
    public Map<N, Set<N>> getRawGraph() {
        return childrenGraph;
    }

    /**
     * Returns the set of direct children of the given node.
     *
     * @param node the node whose children are to be returned. This argument cannot
     *   be {@code null}.
     * @return the set of direct children of the given node. This method
     *   never returns {@code null}, if the node has no children, an empty
     *   set is returned.
     */
    public Set<N> getChildren(N node) {
        Objects.requireNonNull(node, "node");

        Set<N> result = childrenGraph.get(node);
        return result != null ? result : Collections.emptySet();
    }

    /**
     * Returns {@code true} if the given node has at least one child node,
     * {@code false} otherwise. That is, this method returns {@code true},
     * if and only, if the given node is not a leaf node.
     * <P>
     * This method is effectively equivalent to:
     * <pre>
     * !getChildren(node).isEmpty()
     * </pre>
     *
     * @param node the node to be checked. This argument cannot be {@code null}.
     * @return {@code true} if the given node has at least one child node,
     *   {@code false} otherwise
     */
    public boolean hasChildren(N node) {
        Objects.requireNonNull(node, "node");

        // We do not store empty children lists.
        return childrenGraph.containsKey(node);
    }

    /**
     * Returns a {@code DirectedGraph} where the edges a reversed compared to this
     * graph.
     *
     * @return a {@code DirectedGraph} where the edges a reversed compared to this
     *   graph. This method never returns {@code null}.
     */
    public DirectedGraph<N> reverseGraph() {
        Map<N, Set<N>> reverseGraph = CollectionsEx.newLinkedHashMap(childrenGraph.size());
        childrenGraph.forEach((node, children) -> {
            children.forEach((child) -> {
                Set<N> parents = reverseGraph.computeIfAbsent(child, (x) -> new LinkedHashSet<>());
                parents.add(node);
            });
        });

        Iterator<Map.Entry<N, Set<N>>> entryItr = reverseGraph.entrySet().iterator();
        while (entryItr.hasNext()) {
            Map.Entry<N, Set<N>> entry = entryItr.next();
            entry.setValue(Collections.unmodifiableSet(entry.getValue()));
        }

        return new DirectedGraph<>(Collections.unmodifiableMap(reverseGraph));
    }

    /**
     * The {@code Builder} used to create {@link DirectedGraph} instances.
     *
     * <h3>Thread safety</h3>
     * The methods of this class may not be used from multiple threads concurrently.
     *
     * <h4>Synchronization transparency</h4>
     * The methods of this class are <I>synchronization transparent</I>.
     *
     * @param <N> the type of the node. The nodes are distinguished based on their
     *   {@code equals}.
     */
    public static final class Builder<N> {
        private final Map<N, Set<N>> childrenGraph;

        /**
         * Creates a new {@code Builder} with no nodes added yet.
         */
        public Builder() {
            this.childrenGraph = new LinkedHashMap<>();
        }

        /**
         * Adds a node to the graph and returns a {@code ChildrenBuilder} which can be used
         * to define the children of the given node.
         * <P>
         * If you do not add any child to this node, the node will not appear in the built graph,
         * unless this node is the child of another node.
         * <P>
         * Calling this method multiple times with the same node is the same as calling it only once
         * and using any of the returned {@code ChildrenBuilder} instances is effectively the same.
         *
         * @param node the node to which child nodes are to be added to. This argument cannot be
         *   {@code null}.
         * @return the {@code ChildBuilder} which can be used to add new child nodes
         *   to the given node. This method never returns {@code null}.
         */
        public ChildrenBuilder<N> addNode(N node) {
            Set<N> childrenList = getChildrenList(node);
            return new ChildrenBuilderImpl<>(this, childrenList);
        }

        /**
         * Adds a node to the graph and immediately allows configuring its children.
         * <P>
         * If you do not add any child to this node, the node will not appear in the built graph,
         * unless this node is the child of another node.
         * <P>
         * Calling this method multiple times with the same node is the same as calling it only once
         * and adding all children in the same
         *
         * @param node the node to which child nodes are to be added to. This argument cannot be
         *   {@code null}.
         * @param childSpec the action which can add child nodes to the given node. This
         *   argument cannot be {@code null}.
         */
        public void addNode(N node, Consumer<? super ChildrenBuilder<N>> childSpec) {
            ChildrenBuilder<N> childBuilder = addNode(node);
            childSpec.accept(childBuilder);
        }

        /**
         * Adds a node to the graph and immediately add some children to that node.
         * <P>
         * If you do not add any child to this node, the node will not appear in the built graph,
         * unless this node is the child of another node.
         *
         * @param node the node to which child nodes are to be added to. This argument cannot be
         *   {@code null}.
         * @param children the children to be added to the given node. This argument cannot be {@code null},
         *   and cannot contain {@code null} elements. However, it can be an empty collection, in which
         *   case, this method does effectively nothing.
         */
        public void addNodeWithChildren(N node, Collection<? extends N> children) {
            Set<N> childrenList = getChildrenList(node);
            childrenList.addAll(children);
        }

        private Set<N> getChildrenList(N node) {
            Objects.requireNonNull(node, "node");
            return childrenGraph.computeIfAbsent(node, (key) -> new LinkedHashSet<>());
        }

        /**
         * Creates a snapshot of the currently built graph. Nodes and edges added after this
         * method call have effect on the returned {@code DirectedGraph}.
         *
         * @return a snapshot of the currently built graph. This method never returns {@code null}.
         */
        public DirectedGraph<N> build() {
            return new DirectedGraph<>(this);
        }

        private static class ChildrenBuilderImpl<N> implements ChildrenBuilder<N> {
            private final Builder<N> builder;
            private final Set<N> childrenList;

            public ChildrenBuilderImpl(
                    Builder<N> builder,
                    Set<N> childrenList) {
                this.builder = builder;
                this.childrenList = childrenList;
            }

            @Override
            public void addChild(N child, Consumer<? super ChildrenBuilder<N>> grandChildSpec) {
                addChild(child);
                Set<N> grandChildren = builder.getChildrenList(child);
                grandChildSpec.accept(new ChildrenBuilderImpl<>(builder, grandChildren));
            }

            @Override
            public void addChild(N child) {
                Objects.requireNonNull(child, "child");
                childrenList.add(child);
            }

            @Override
            public void addChildren(Collection<? extends N> children) {
                ExceptionHelper.checkNotNullElements(children, "children");
                childrenList.addAll(children);
            }
        }
    }

    private static <K, V> Map<K, Set<V>> copy(Map<K, Set<V>> src) {
        Map<K, Set<V>> result = CollectionsEx.newLinkedHashMap(src.size());
        src.forEach((key, value) -> {
            if (!value.isEmpty()) {
                result.put(key, Collections.unmodifiableSet(new LinkedHashSet<>(value)));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    /**
     * Defines a builder to add children to a particular node in the graph.
     *
     * <h3>Thread safety</h3>
     * The methods of this class may not be used from multiple threads concurrently.
     *
     * <h4>Synchronization transparency</h4>
     * The methods of this class are <I>synchronization transparent</I>.
     *
     * @param <N> the type of the node. The nodes are distinguished based on their
     *   {@code equals}.
     *
     * @see DirectedGraph.Builder#addNode(Object) DirectedGraph.Builder.addNode
     */
    public interface ChildrenBuilder<N> {
        /**
         * Adds a new child to the associated node and continues with adding children
         * to the newly added child node.
         *
         * @param child the new child to be added to the associated node. This argument
         *   cannot be {@code null}.
         * @param grandChildSpec the action which can add child nodes to the newly added child node.
         *   This argument cannot be {@code null}.
         */
        public void addChild(N child, Consumer<? super ChildrenBuilder<N>> grandChildSpec);

        /**
         * Adds new children to the associated node.
         * <P>
         * The default implementation repeatedly calls the one argument
         * {@link #addChild(Object) addChild} method for all the elements in the
         * children collection.
         *
         * @param children the children to be added to the associated node. This argument
         *   cannot be {@code null} and cannot contain {@code null} elements. However, it
         *   can be empty, in which case, this method does effectively nothing.
         */
        public default void addChildren(Collection<? extends N> children) {
            children.forEach(this::addChild);
        }

        /**
         * Adds a new child to the associated node.
         * <P>
         * The default implementation calls the two argument
         * {@link #addChild(Object, Consumer) addChild} method} with an action doing nothing.
         *
         * @param child the new child to be added to the associated node. This argument
         *   cannot be {@code null}.
         */
        public default void addChild(N child) {
            addChild(child, (grandChildBuilder) -> { });
        }
    }
}
