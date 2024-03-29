package org.jtrim2.taskgraph.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DirectedGraphTest {
    public static class DefaultLeafToRootTest extends AbstractLeafToRootTest {
        public DefaultLeafToRootTest() {
            super(true, LinkedHashSet.class, DirectedGraph::getAllLeafToRootNodes);
        }
    }

    public static class TreeLeafToRootTest extends AbstractLeafToRootTest {
        public TreeLeafToRootTest() {
            super(TreeSet.class, (graph, roots) -> graph.getAllLeafToRootNodes(roots, TreeSet::new));
        }
    }

    private static void verifyCyclic(DirectedGraph<?> graph, String... expectedCycle) {
        try {
            graph.checkNotCyclic();
        } catch (IllegalStateException ex) {
            if (!ex.getMessage().contains(String.join(", ", expectedCycle))) {
                throw new AssertionError("Exception message does not contain the expected cycle: " + ex.getMessage());
            }
            return;
        }

        throw new AssertionError("Expected IllegalStateException");
    }

    private static void verifyNotCyclic(DirectedGraph<?> graph) {
        graph.checkNotCyclic();
    }

    private static void test(
            TestSetup graphBuilder,
            GraphVerifier verifier) {

        DirectedGraph.Builder<String> builder = new DirectedGraph.Builder<>();
        DirectedGraph<String> graph = graphBuilder.buildGraph(builder);

        verifier.verify(graph);
    }

    private static void testResult(
            TestSetup graphBuilder,
            Consumer<TestBuilder> expectationBuilder) {
        testResult(graphBuilder, expectationBuilder, (graph) -> { });
    }

    private static void testResult(
            TestSetup graphBuilder,
            Consumer<TestBuilder> expectationBuilder,
            GraphVerifier extraVerification) {
        DirectedGraph.Builder<String> builder = new DirectedGraph.Builder<>();
        graphBuilder.buildGraph(builder);

        test(graphBuilder, (graph) -> {
            Map<String, Set<String>> expected = new HashMap<>();
            expectationBuilder.accept((parent, children) -> {
                assert children.length > 0;
                expected.put(parent, Collections.unmodifiableSet(new HashSet<>(Arrays.asList(children))));
            });

            assertEquals("rawGraph", expected, graph.getRawGraph());
            expected.keySet().forEach((parent) -> {
                assertTrue("hasChildren: " + parent, graph.hasChildren(parent));
                assertEquals("children: " + parent, expected.get(parent), graph.getChildren(parent));
            });

            extraVerification.verify(graph);
        });
    }

    private void testDoubleSplitGraph(TestSetup graphBuilder) {
        testResult(graphBuilder, (builder) -> {
            builder.setDefaultEdges("a", 2);
            builder.setDefaultEdges("a.a", 2);
            builder.setDefaultEdges("a.b", 2);
        }, (graph) -> {
            for (String leaf: new String[]{"a.a.a", "a.a.b", "a.b.a", "a.b.b", "x"}) {
                assertFalse("hasChildren: " + leaf, graph.hasChildren(leaf));
            }

            verifyNotCyclic(graph);

            testResult(builder -> graph.reverseGraph(), (builder) -> {
                builder.setEdges("a.a.a", "a.a");
                builder.setEdges("a.a.b", "a.a");

                builder.setEdges("a.b.a", "a.b");
                builder.setEdges("a.b.b", "a.b");

                builder.setEdges("a.a", "a");
                builder.setEdges("a.b", "a");
            }, (reverseGraph) -> {
                for (String leaf: new String[]{"a", "x"}) {
                    assertFalse("hasChildren: " + leaf, reverseGraph.hasChildren(leaf));
                }

                verifyNotCyclic(reverseGraph);
            });
        });
    }

    @Test
    public void testTreeBuildWithAdds() {
        testDoubleSplitGraph((builder) -> {
            DirectedGraph.ChildrenBuilder<String> childrenA = builder.addNode("a");
            childrenA.addChild("a.a");
            childrenA.addChild("a.b");

            DirectedGraph.ChildrenBuilder<String> childrenAA = builder.addNode("a.a");
            childrenAA.addChild("a.a.a");
            childrenAA.addChild("a.a.b");

            DirectedGraph.ChildrenBuilder<String> childrenAB = builder.addNode("a.b");
            childrenAB.addChild("a.b.a");
            childrenAB.addChild("a.b.b");
            return builder.build();
        });
    }

    @Test
    public void testTreeBuildWithBogusAdds() {
        testDoubleSplitGraph((builder) -> {
            DirectedGraph.ChildrenBuilder<String> childrenA = builder.addNode("a");
            childrenA.addChild("a.a");
            childrenA.addChild("a.b");

            DirectedGraph.ChildrenBuilder<String> childrenAA = builder.addNode("a.a");
            childrenAA.addChild("a.a.a");
            childrenAA.addChild("a.a.b");

            DirectedGraph.ChildrenBuilder<String> childrenAB = builder.addNode("a.b");
            childrenAB.addChild("a.b.a");
            childrenAB.addChild("a.b.b");

            builder.addNode("x");

            return builder.build();
        });
    }

    @Test
    public void testTreeBuildWithSeparatedAdds() {
        testDoubleSplitGraph((builder) -> {
            DirectedGraph.ChildrenBuilder<String> childrenA = builder.addNode("a");
            childrenA.addChild("a.a");
            childrenA = builder.addNode("a");
            childrenA.addChild("a.b");

            DirectedGraph.ChildrenBuilder<String> childrenAA = builder.addNode("a.a");
            childrenAA.addChild("a.a.a");
            childrenAA = builder.addNode("a.a");
            childrenAA.addChild("a.a.b");

            DirectedGraph.ChildrenBuilder<String> childrenAB = builder.addNode("a.b");
            childrenAB.addChild("a.b.a");
            childrenAB = builder.addNode("a.b");
            childrenAB.addChild("a.b.b");
            return builder.build();
        });
    }

    @Test
    public void testTreeBuildWithAddWithChildren() {
        testDoubleSplitGraph((builder) -> {
            builder.addNodeWithChildren("a", Arrays.asList("a.a", "a.b"));
            builder.addNodeWithChildren("a.a", Arrays.asList("a.a.a", "a.a.b"));
            builder.addNodeWithChildren("a.b", Arrays.asList("a.b.a", "a.b.b"));
            return builder.build();
        });
    }

    @Test
    public void testTreeBuildWithNestedApi() {
        testDoubleSplitGraph((builder) -> {
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
        });
    }

    @Test
    public void testTreeBuildWithNestedApiCollection() {
        testDoubleSplitGraph((builder) -> {
            builder.addNode("a", (level0) -> {
                level0.addChild("a.a", (level1) -> {
                    level1.addChildren(Arrays.asList("a.a.a", "a.a.b"));
                });
                level0.addChild("a.b", (level1) -> {
                    level1.addChildren(Arrays.asList("a.b.a", "a.b.b"));
                });
            });
            return builder.build();
        });
    }

    @Test
    public void testEmpty() {
        testResult(DirectedGraph.Builder::build, (builder) -> {
        }, (graph) -> {
            testResult(builder -> graph.reverseGraph(), (builder) -> {
            });
        });
    }

    @Test
    public void testChildrenBuilderDefaultAddChild() {
        List<String> added = new ArrayList<>();
        Runnable uncalledMethod = mock(Runnable.class);
        DirectedGraph.ChildrenBuilder<String> childrenBuilder = new DirectedGraph.ChildrenBuilder<String>() {
            @Override
            public void addChild(String child, Consumer<? super DirectedGraph.ChildrenBuilder<String>> grandChildSpec) {
                added.add(child);
                grandChildSpec.accept((DirectedGraph.ChildrenBuilder<String>) (child1, grandChildSpec1) -> {
                    uncalledMethod.run();
                });
            }

            @Override
            public void addChildren(Collection<? extends String> children) {
                uncalledMethod.run();
            }
        };

        childrenBuilder.addChild("abc");
        assertEquals(Arrays.asList("abc"), added);
        verifyNoInteractions(uncalledMethod);
    }

    @Test
    public void testChildrenBuilderDefaultAddChildren() {
        List<String> added = new ArrayList<>();
        Runnable uncalledMethod = mock(Runnable.class);
        DirectedGraph.ChildrenBuilder<String> childrenBuilder = new DirectedGraph.ChildrenBuilder<String>() {
            @Override
            public void addChild(String child, Consumer<? super DirectedGraph.ChildrenBuilder<String>> grandChildSpec) {
                uncalledMethod.run();
            }

            @Override
            public void addChild(String child) {
                added.add(child);
            }
        };

        List<String> expected = Collections.unmodifiableList(Arrays.asList("abc1", "abc2", "abc3", "abc4"));
        childrenBuilder.addChildren(expected);
        assertEquals(expected, added);
        verifyNoInteractions(uncalledMethod);
    }

    @Test
    public void testLoop() {
        testResult((builder) -> {
            builder.addNode("a").addChild("b");
            builder.addNode("b").addChild("c");
            builder.addNode("c").addChild("a");
            return builder.build();
        }, (builder) -> {
            builder.setEdges("a", "b");
            builder.setEdges("b", "c");
            builder.setEdges("c", "a");
        }, (graph) -> {
            verifyCyclic(graph);

            testResult(builder -> graph.reverseGraph(), (builder) -> {
                builder.setEdges("a", "c");
                builder.setEdges("b", "a");
                builder.setEdges("c", "b");
            }, (reverseGraph) -> {
                verifyCyclic(reverseGraph);
            });
        });
    }

    @Test
    public void testDag() {
        testResult((builder) -> {
            builder.addNode("a").addChild("a.a");
            builder.addNode("a").addChild("a.b");
            builder.addNode("a.a").addChild("c");
            builder.addNode("a.b").addChild("c");
            return builder.build();
        }, (builder) -> {
            builder.setEdges("a", "a.a", "a.b");
            builder.setEdges("a.a", "c");
            builder.setEdges("a.b", "c");
        }, (graph) -> {
            verifyNotCyclic(graph);

            testResult(builder -> graph.reverseGraph(), (builder) -> {
                builder.setEdges("c", "a.a", "a.b");
                builder.setEdges("a.a", "a");
                builder.setEdges("a.b", "a");
            }, (reverseGraph) -> {
                verifyNotCyclic(reverseGraph);
            });
        });
    }

    @Test
    public void testDisjunctEdges() {
        testResult((builder) -> {
            builder.addNode("a1").addChild("b1");
            builder.addNode("a2").addChild("b2");
            return builder.build();
        }, (builder) -> {
            builder.setEdges("a1", "b1");
            builder.setEdges("a2", "b2");
        }, (graph) -> {
            verifyNotCyclic(graph);

            testResult(builder -> graph.reverseGraph(), (builder) -> {
                builder.setEdges("b1", "a1");
                builder.setEdges("b2", "a2");
            }, (reverseGraph) -> {
                verifyNotCyclic(reverseGraph);
            });
        });
    }

    private void testGetReachableNodes(
            Collection<String> nodes,
            TestSetup setup,
            String... expected) {

        DirectedGraph.Builder<String> builder = new DirectedGraph.Builder<>();
        setup.buildGraph(builder);
        DirectedGraph<String> graph = builder.build();

        Set<String> reachableNodes = graph.getReachableNodes(nodes);
        assertEquals(new HashSet<>(Arrays.asList(expected)), reachableNodes);
    }

    @Test
    public void testGetReachableNodesFromRoot1() {
        testGetReachableNodes(Arrays.asList("a"), (builder) -> {
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
        }, "a", "x", "x.a", "x.b");
    }

    @Test
    public void testGetReachableNodesFromRoot2() {
        testGetReachableNodes(Arrays.asList("a", "b"), (builder) -> {
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
        }, "a", "x", "x.a", "x.b", "b", "x", "b.a");
    }

    @Test
    public void testGetReachableNodesFromNonExistent() {
        testGetReachableNodes(Arrays.asList("NON-EXISTENT"), (builder) -> {
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
        }, "NON-EXISTENT");
    }

    @Test
    public void testGetReachableNodesFromNonRoot1() {
        testGetReachableNodes(Arrays.asList("x"), (builder) -> {
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
        }, "x", "x.a", "x.b");
    }

    @Test
    public void testGetReachableNodesFromNonRoot2() {
        testGetReachableNodes(Arrays.asList("x", "b.a"), (builder) -> {
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
        }, "x", "x.a", "x.b", "b.a");
    }

    @Test
    public void testGetReachableNodesFromNonExistentEmpty() {
        testGetReachableNodes(Arrays.asList("NON-EXISTENT"), DirectedGraph.Builder::build, "NON-EXISTENT");
    }

    public interface TestSetup {
        public DirectedGraph<String> buildGraph(DirectedGraph.Builder<String> builder);
    }

    public interface TestBuilder {
        public void setEdges(String parent, String... children);

        public default void setDefaultEdges(String parent, int childrenCount) {
            String[] children = new String[childrenCount];
            for (int i = 0; i < children.length; i++) {
                char childName = (char) ('a' + i);
                children[i] = parent + "." + childName;
            }
            setEdges(parent, children);
        }
    }

    private interface GraphVerifier {
        public void verify(DirectedGraph<String> graph);
    }
}
