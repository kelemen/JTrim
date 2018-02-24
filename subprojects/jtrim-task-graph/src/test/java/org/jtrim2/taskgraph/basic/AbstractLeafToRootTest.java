package org.jtrim2.taskgraph.basic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.taskgraph.basic.DirectedGraphTest.TestBuilder;
import org.jtrim2.taskgraph.basic.DirectedGraphTest.TestSetup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public abstract class AbstractLeafToRootTest {
    private final boolean keepOrder;
    private final Class<?> expectedSetType;
    private final LeafToRootsFunction<?> leafToRoots;

    public <C extends Collection<String>> AbstractLeafToRootTest(
            Class<?> expectedSetType,
            LeafToRootsFunction<C> leafToRoots) {
        this(false, expectedSetType, leafToRoots);
    }

    public <C extends Collection<String>> AbstractLeafToRootTest(
            boolean keepOrder,
            Class<?> expectedSetType,
            LeafToRootsFunction<C> leafToRoots) {
        this.keepOrder = keepOrder;
        this.expectedSetType = Objects.requireNonNull(expectedSetType, "expectedSetType");
        this.leafToRoots = Objects.requireNonNull(leafToRoots, "leafToRoots");
    }

    public interface LeafToRootsFunction<C extends Collection<String>> {
        public Map<String, C> getAllLeafToRootNodes(DirectedGraph<String> graph, Iterable<? extends String> rootNodes);
    }

    private Map<String, Set<String>> testGetAllLeafToRootNodes(
            List<String> rootNodes,
            TestSetup setup,
            Consumer<TestBuilder> expectationBuilder) {

        DirectedGraph.Builder<String> builder = new DirectedGraph.Builder<>();
        DirectedGraph<String> graph = setup.buildGraph(builder);

        Map<String, Set<String>> expected = new HashMap<>();
        expectationBuilder.accept((parent, children) -> {
            assert children.length > 0;
            expected.put(parent, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(children))));
        });

        Map<String, ? extends Collection<String>> leafToRoot = leafToRoots.getAllLeafToRootNodes(graph, rootNodes);
        leafToRoot.values().forEach((rootSet) -> {
            if (!expectedSetType.isAssignableFrom(rootSet.getClass())) {
                throw new AssertionError("Unexpected set type: " + rootSet.getClass());
            }
        });

        Map<String, Set<String>> result = valuesToSet(leafToRoot);
        assertEquals(expected, result);

        if (keepOrder) {
            result.values().forEach((rootSet) -> {
                int lastIndex = -1;
                for (String root: rootSet) {
                    int index = rootNodes.indexOf(root);
                    if (index <= lastIndex) {
                        throw new AssertionError("Wrong order for " + rootSet + ". Expected: " + rootNodes);
                    }
                    lastIndex = index;
                }
            });
        }

        return result;
    }

    private static <K, V> Map<K, Set<V>> valuesToSet(Map<K, ? extends Collection<V>> src) {
        Map<K, Set<V>> result = new LinkedHashMap<>();
        src.forEach((key, valueList) -> {
            Set<V> valueSet = CollectionsEx.newLinkedHashSet(valueList.size());
            valueList.forEach(valueSet::add);

            if (valueList.size() != valueSet.size()) {
                throw new AssertionError("Non-unique elements for " + key + " in " + valueList);
            }

            result.put(key, valueSet);
        });
        return result;
    }

    @Test
    public void testManyRootToTwoLeaves() {
        List<String> roots = Arrays.asList("b", "a", "f", "d", "e", "c");
        testGetAllLeafToRootNodes(roots, (builder) -> {
            roots.forEach(root -> {
                builder.addNode(root, (level0) -> {
                    level0.addChild("x");
                    level0.addChild("y");
                });
            });
            return builder.build();
        }, (graph) -> {
            graph.setEdges("x", "b", "a", "f", "d", "e", "c");
            graph.setEdges("y", "b", "a", "f", "d", "e", "c");
        });
    }

    @Test
    public void testGetAllLeafToRootNodesSingleRoot() {
        testGetAllLeafToRootNodes(Arrays.asList("a"), (builder) -> {
            builder.addNode("a", (level0) -> {
                level0.addChild("a.a");
                level0.addChild("a.b");
                level0.addChild("a.c");
                level0.addChild("a.d");
                level0.addChild("a.e");
                level0.addChild("a.f");
            });
            return builder.build();
        }, (graph) -> {
            graph.setEdges("a.a", "a");
            graph.setEdges("a.b", "a");
            graph.setEdges("a.c", "a");
            graph.setEdges("a.d", "a");
            graph.setEdges("a.e", "a");
            graph.setEdges("a.f", "a");
        });
    }

    @Test
    public void testGetAllLeafToRootNodesForTree() {
        testGetAllLeafToRootNodes(Arrays.asList("a"), (builder) -> {
            builder.addNode("a", (level0) -> {
                level0.addChild("a.a", (level1) -> {
                    level1.addChild("a.a.a");
                    level1.addChild("a.a.b");
                });
                level0.addChild("a.b", (level1) -> {
                    level1.addChild("a.b.a");
                    level1.addChild("a.b.b");
                });
            });
            return builder.build();
        }, (graph) -> {
            graph.setEdges("a.a.a", "a");
            graph.setEdges("a.a.b", "a");
            graph.setEdges("a.b.a", "a");
            graph.setEdges("a.b.b", "a");
        });
    }

    @Test
    public void testGetAllLeafToRootNodesForMultiRoot() {
        testGetAllLeafToRootNodes(Arrays.asList("a", "b"), (builder) -> {
            builder.addNode("a", (level0) -> {
                level0.addChild("x", (level1) -> {
                    level1.addChild("x.a");
                    level1.addChild("x.b");
                });
            });
            builder.addNode("b", (level0) -> {
                level0.addChild("x");
                level0.addChild("b.a");
            });
            return builder.build();
        }, (graph) -> {
            graph.setEdges("x.a", "a", "b");
            graph.setEdges("x.b", "a", "b");
            graph.setEdges("b.a", "b");
        });
    }

    @Test
    public void testGetAllLeafToRootNodesForMultiRootSingleRequested1() {
        testGetAllLeafToRootNodes(Arrays.asList("b"), (builder) -> {
            builder.addNode("a", (level0) -> {
                level0.addChild("x", (level1) -> {
                    level1.addChild("x.a");
                    level1.addChild("x.b");
                });
            });
            builder.addNode("b", (level0) -> {
                level0.addChild("x");
                level0.addChild("b.a");
            });
            return builder.build();
        }, (graph) -> {
            graph.setEdges("x.a", "b");
            graph.setEdges("x.b", "b");
            graph.setEdges("b.a", "b");
        });
    }

    @Test
    public void testGetAllLeafToRootNodesForMultiRootSingleRequested2() {
        testGetAllLeafToRootNodes(Arrays.asList("a"), (builder) -> {
            builder.addNode("a", (level0) -> {
                level0.addChild("x", (level1) -> {
                    level1.addChild("x.a");
                    level1.addChild("x.b");
                });
            });
            builder.addNode("b", (level0) -> {
                level0.addChild("x");
                level0.addChild("b.a");
            });
            return builder.build();
        }, (graph) -> {
            graph.setEdges("x.a", "a");
            graph.setEdges("x.b", "a");
        });
    }
}
