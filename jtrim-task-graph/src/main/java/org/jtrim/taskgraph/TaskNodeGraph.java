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
import org.jtrim.collections.CollectionsEx;
import org.jtrim.utils.ExceptionHelper;

public final class TaskNodeGraph {
    private final Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> childrenGraph;

    private TaskNodeGraph(Builder builder) {
        this(copy(builder.childrenGraph));
    }

    private TaskNodeGraph(Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> childrenGraph) {
        this.childrenGraph = childrenGraph;
    }

    public void checkNotCyclic() {
        Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> graph = new HashMap<>(childrenGraph);
        while (!graph.isEmpty()) {
            TaskNodeKey<?, ?> key = graph.keySet().iterator().next();
            checkNotCyclic(Collections.singleton(key), new LinkedHashSet<>(), graph);
        }
    }

    private static void checkNotCyclic(
            Set<TaskNodeKey<?, ?>> startNodes,
            Set<TaskNodeKey<?, ?>> visited,
            Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> graph) {

        startNodes.forEach((key) -> {
            if (visited.contains(key)) {
                List<TaskNodeKey<?, ?>> cycle = new ArrayList<>(visited.size() + 1);
                cycle.addAll(visited);
                cycle.add(key);

                cycle = afterFirstMatch(key, cycle);

                throw new IllegalStateException("The graph is cyclic: " + cycle);
            }

            Set<TaskNodeKey<?, ?>> dependencies = graph.get(key);
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

    public Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> getRawGraph() {
        return childrenGraph;
    }

    public Set<TaskNodeKey<?, ?>> getChildren(TaskNodeKey<?, ?> node) {
        Set<TaskNodeKey<?, ?>> result = childrenGraph.get(node);
        return result != null ? result : Collections.emptySet();
    }

    public boolean hasChildren(TaskNodeKey<?, ?> node) {
        // We do not store empty children lists.
        return childrenGraph.containsKey(node);
    }

    public TaskNodeGraph reverseGraph() {
        Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> reverseGraph = CollectionsEx.newHashMap(childrenGraph.size());
        childrenGraph.forEach((node, children) -> {
            children.forEach((child) -> {
                Set<TaskNodeKey<?, ?>> parents = reverseGraph.computeIfAbsent(child, (x) -> new HashSet<>());
                parents.add(node);
            });
        });

        Iterator<Map.Entry<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>>> entryItr = reverseGraph.entrySet().iterator();
        while (entryItr.hasNext()) {
            Map.Entry<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> entry = entryItr.next();
            entry.setValue(Collections.unmodifiableSet(entry.getValue()));
        }

        return new TaskNodeGraph(Collections.unmodifiableMap(reverseGraph));
    }

    public static final class Builder {
        private final Map<TaskNodeKey<?, ?>, Set<TaskNodeKey<?, ?>>> childrenGraph;

        public Builder() {
            this.childrenGraph = new HashMap<>();
        }

        public ChildrenBuilder addNode(TaskNodeKey<?, ?> node) {
            Set<TaskNodeKey<?, ?>> childrenList = getChildrenList(node);
            return new ChildrenBuilderImpl(childrenList);
        }

        public void addNodeWithChildren(TaskNodeKey<?, ?> node, Collection<TaskNodeKey<?, ?>> children) {
            Set<TaskNodeKey<?, ?>> childrenList = getChildrenList(node);
            childrenList.addAll(children);
        }

        private Set<TaskNodeKey<?, ?>> getChildrenList(TaskNodeKey<?, ?> node) {
            ExceptionHelper.checkNotNullArgument(node, "node");
            return childrenGraph.computeIfAbsent(node, (key) -> new HashSet<>());
        }

        public TaskNodeGraph build() {
            return new TaskNodeGraph(this);
        }

        private static class ChildrenBuilderImpl implements ChildrenBuilder {
            private final Set<TaskNodeKey<?, ?>> childrenList;

            public ChildrenBuilderImpl(
                    Set<TaskNodeKey<?, ?>> childrenList) {
                this.childrenList = childrenList;
            }

            @Override
            public void addChild(TaskNodeKey<?, ?> child) {
                ExceptionHelper.checkNotNullArgument(child, "child");
                childrenList.add(child);
            }

            @Override
            public void addChildren(Collection<? extends TaskNodeKey<?, ?>> children) {
                ExceptionHelper.checkNotNullElements(children, "children");
                childrenList.addAll(children);
            }
        }
    }

    private static <K, V> Map<K, Set<V>> copy(Map<K, Set<V>> src) {
        Map<K, Set<V>> result = CollectionsEx.newHashMap(src.size());
        src.forEach((key, value) -> {
            result.put(key, Collections.unmodifiableSet(new HashSet<>(value)));
        });
        return Collections.unmodifiableMap(result);
    }

    public interface ChildrenBuilder {
        public void addChildren(Collection<? extends TaskNodeKey<?, ?>> children);

        public default void addChild(TaskNodeKey<?, ?> child) {
            addChildren(Collections.singletonList(child));
        }
    }
}
