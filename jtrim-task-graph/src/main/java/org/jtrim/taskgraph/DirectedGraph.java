package org.jtrim.taskgraph;

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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class DirectedGraph<N> {
    private final Map<N, Set<N>> childrenGraph;

    private DirectedGraph(Builder<N> builder) {
        this(copy(builder.childrenGraph));
    }

    private DirectedGraph(Map<N, Set<N>> childrenGraph) {
        this.childrenGraph = childrenGraph;
    }

    public void checkNotCyclic() {
        Map<N, Set<N>> graph = new HashMap<>(childrenGraph);
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

    public Collection<N> getEndNodes(Collection<? extends N> nodes) {
        Collection<N> result = new ArrayList<>();
        forEndNodes(nodes, result::add);
        return result;
    }

    public void forEndNodes(Collection<? extends N> nodes, Consumer<? super N> action) {
        nodes.forEach((node) -> {
            if (!hasChildren(node)) {
                action.accept(node);
            }
        });
    }

    public Map<N, Set<N>> getAllLeafToRootNodes(Iterable<? extends N> rootNodes) {
        return getAllLeafToRootNodes(rootNodes, LinkedHashSet::new);
    }

    public Map<N, Set<N>> getAllLeafToRootNodes(
            Iterable<? extends N> rootNodes,
            Supplier<? extends Set<N>> newSetFactory) {

        Function<N, Set<N>> keyBasedSetFactory = (key) -> newSetFactory.get();

        Map<N, Set<N>> result = new HashMap<>();
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
        }
        else {
            children.forEach((key) -> {
                addLeafToRootNodes(root, key, result, newSetFactory);
            });
        }
    }

    public Map<N, Set<N>> getRawGraph() {
        return childrenGraph;
    }

    public Set<N> getChildren(N node) {
        Set<N> result = childrenGraph.get(node);
        return result != null ? result : Collections.emptySet();
    }

    public boolean hasChildren(N node) {
        // We do not store empty children lists.
        return childrenGraph.containsKey(node);
    }

    public DirectedGraph<N> reverseGraph() {
        Map<N, Set<N>> reverseGraph = CollectionsEx.newHashMap(childrenGraph.size());
        childrenGraph.forEach((node, children) -> {
            children.forEach((child) -> {
                Set<N> parents = reverseGraph.computeIfAbsent(child, (x) -> new HashSet<>());
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

    public static final class Builder<N> {
        private final Map<N, Set<N>> childrenGraph;

        public Builder() {
            this.childrenGraph = new HashMap<>();
        }

        public ChildrenBuilder<N> addNode(N node) {
            Set<N> childrenList = getChildrenList(node);
            return new ChildrenBuilderImpl<>(childrenList);
        }

        public void addNodeWithChildren(N node, Collection<N> children) {
            Set<N> childrenList = getChildrenList(node);
            childrenList.addAll(children);
        }

        private Set<N> getChildrenList(N node) {
            ExceptionHelper.checkNotNullArgument(node, "node");
            return childrenGraph.computeIfAbsent(node, (key) -> new HashSet<>());
        }

        public DirectedGraph<N> build() {
            return new DirectedGraph<>(this);
        }

        private static class ChildrenBuilderImpl<N> implements ChildrenBuilder<N> {
            private final Set<N> childrenList;

            public ChildrenBuilderImpl(
                    Set<N> childrenList) {
                this.childrenList = childrenList;
            }

            @Override
            public void addChild(N child) {
                ExceptionHelper.checkNotNullArgument(child, "child");
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
        Map<K, Set<V>> result = CollectionsEx.newHashMap(src.size());
        src.forEach((key, value) -> {
            if (!value.isEmpty()) {
                result.put(key, Collections.unmodifiableSet(new HashSet<>(value)));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    public interface ChildrenBuilder<N> {
        public void addChildren(Collection<? extends N> children);

        public default void addChild(N child) {
            addChildren(Collections.singletonList(child));
        }
    }
}
