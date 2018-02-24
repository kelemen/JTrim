package org.jtrim2.taskgraph.basic;

import org.junit.Test;

import static org.junit.Assert.*;

public class DependencyDagTest {
    private static DependencyDag<String> create(DirectedGraph<String> graph) {
        return new DependencyDag<>(graph);
    }

    @Test
    public void testDag() {
        DirectedGraph.Builder<String> builder = new DirectedGraph.Builder<>();
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

        DirectedGraph<String> graph = builder.build();

        DependencyDag<String> dag = create(graph);
        assertSame("getDependencyGraph", graph, dag.getDependencyGraph());
        assertEquals("getForwardGraph",
                graph.reverseGraph().getRawGraph(),
                dag.getForwardGraph().getRawGraph());
    }

    @Test
    public void testCyclic() {
        DirectedGraph.Builder<String> builder = new DirectedGraph.Builder<>();
        builder.addNode("a").addChild("b");
        builder.addNode("b").addChild("c");
        builder.addNode("c").addChild("a");

        DirectedGraph<String> graph = builder.build();

        try {
            create(graph);
        } catch (IllegalStateException ex) {
            return;
        }
        throw new AssertionError("Expected failure for cyclic graph.");
    }

    @Test
    public void testReverseGraph() {
        DirectedGraph.Builder<String> builder = new DirectedGraph.Builder<>();
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

        DependencyDag<String> dag = create(builder.build());

        DependencyDag<String> reversedGraph = dag.reverse();

        assertSame("dependencyGraph", dag.getForwardGraph(), reversedGraph.getDependencyGraph());
        assertSame("forwardGraph", dag.getDependencyGraph(), reversedGraph.getForwardGraph());
    }
}
