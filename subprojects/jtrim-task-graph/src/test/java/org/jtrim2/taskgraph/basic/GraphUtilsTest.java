package org.jtrim2.taskgraph.basic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

public class GraphUtilsTest {
    @Test
    public void testDoubleSplitGraphForward() {
        testDoubleSplitGraphWithOrder(0, 1, 2, 3, 4, 5, 6);
    }

    @Test
    public void testDoubleSplitGraphBackward() {
        testDoubleSplitGraphWithOrder(6, 5, 4, 3, 2, 1, 0);
    }

    @Test
    public void testDoubleSplitGraphRandom() {
        testDoubleSplitGraphWithOrder(3, 4, 1, 6, 5, 0, 2);
    }

    private void testDoubleSplitGraphWithOrder(int... order) {
        DirectedGraph.Builder<String> builder = new DirectedGraph.Builder<>();
        builder.addNode("root", (root) -> {
            for (int i = 0; i < 2; i++) {
                String childName = "child" + (i + 1);
                root.addChild(childName, (child1) -> {
                    child1.addChild(childName + ".child1");
                    child1.addChild(childName + ".child2");
                });
            }
        });


        List<String> src = Arrays.asList(
                "root",
                "child1",
                "child2",
                "child1.child1",
                "child1.child2",
                "child2.child1",
                "child2.child2");
        if (src.size() != order.length) {
            throw new IllegalArgumentException("Illegal order list.");
        }

        List<String> toSort = new ArrayList<>(src.size());
        for (int index: order) {
            toSort.add(src.get(index));
        }

        List<String> sorted = GraphUtils.sortRecursively(
                builder.build(),
                Collections.singletonList("root"),
                new LinkedHashSet<>(toSort));

        assertEquals(new HashSet<>(toSort), new HashSet<>(sorted));

        src.subList(1, src.size())
                .forEach(node -> expectedOrder(sorted, node, "root"));

        expectedOrder(sorted, "child1.child1", "child1");
        expectedOrder(sorted, "child1.child2", "child1");
        expectedOrder(sorted, "child2.child1", "child2");
        expectedOrder(sorted, "child2.child2", "child2");

        String firstChildName;
        String secondChildName;
        if (isInOrder(sorted, "child1.child1", "child2.child1")) {
            firstChildName = "child1";
            secondChildName = "child2";
        }
        else {
            firstChildName = "child2";
            secondChildName = "child1";
        }

        expectedOrder(sorted, firstChildName + ".child1", secondChildName + ".child1");
        expectedOrder(sorted, firstChildName + ".child2", secondChildName + ".child1");
        expectedOrder(sorted, firstChildName + ".child1", secondChildName + ".child2");
        expectedOrder(sorted, firstChildName + ".child2", secondChildName + ".child2");
        expectedOrder(sorted, firstChildName, secondChildName);
    }

    @Test
    public void testSemiConnectedGraphForward() {
        testSemiConnectedGraphWithOrder(0, 1, 2, 3, 4, 5);
    }

    @Test
    public void testSemiConnectedGraphBackward() {
        testSemiConnectedGraphWithOrder(5, 4, 3, 2, 1, 0);
    }

    @Test
    public void testSemiConnectedGraphRandom() {
        testSemiConnectedGraphWithOrder(3, 4, 1, 5, 0, 2);
    }

    private void testSemiConnectedGraphWithOrder(int... order) {
        DirectedGraph.Builder<String> builder = new DirectedGraph.Builder<>();
        builder.addNode("root1", (root1) -> {
            root1.addChild("common", (common) -> {
                common.addChild("child1");
                common.addChild("child2");
            });
        });
        builder.addNode("root2", (root2) -> {
            root2.addChild("common");
            root2.addChild("child3");
        });

        List<String> src = Arrays.asList(
                "root1",
                "common",
                "child1",
                "child2",
                "root2",
                "child3");
        if (src.size() != order.length) {
            throw new IllegalArgumentException("Illegal order list.");
        }

        List<String> toSort = new ArrayList<>(src.size());
        for (int index: order) {
            toSort.add(src.get(index));
        }

        List<String> sorted = GraphUtils.sortRecursively(
                builder.build(),
                Arrays.asList("root1", "root2"),
                new LinkedHashSet<>(toSort));

        assertEquals(new HashSet<>(toSort), new HashSet<>(sorted));

        expectedOrder(sorted, "common", "root1");
        expectedOrder(sorted, "common", "root2");
        expectedOrder(sorted, "child3", "root2");

        expectedOrder(sorted, "child1", "common");
        expectedOrder(sorted, "child2", "common");
    }

    @Test
    public void testLoopForward() {
        testLoopWithOrder(0, 1, 2);
    }

    @Test
    public void testLoopBackward() {
        testLoopWithOrder(2, 1, 0);
    }

    @Test
    public void testLoopRandom() {
        testLoopWithOrder(1, 0, 2);
    }

    private void testLoopWithOrder(int... order) {
        DirectedGraph.Builder<String> builder = new DirectedGraph.Builder<>();
        builder.addNode("a", (root1) -> {
            root1.addChild("b", (common) -> {
                common.addChild("c");
            });
        });
        builder.addNode("c").addChild("a");
        builder.addNode("root2", (root2) -> {
            root2.addChild("common");
            root2.addChild("child3");
        });

        List<String> src = Arrays.asList("a", "b", "c");
        if (src.size() != order.length) {
            throw new IllegalArgumentException("Illegal order list.");
        }

        List<String> toSort = new ArrayList<>(src.size());
        for (int index: order) {
            toSort.add(src.get(index));
        }

        List<String> sorted = GraphUtils.sortRecursively(
                builder.build(),
                Arrays.asList("a"),
                new LinkedHashSet<>(toSort));

        assertEquals(Arrays.asList("c", "b", "a"), sorted);
    }

    private static boolean isInOrder(List<?> src, Object first, Object second) {
        int firstIndex = src.indexOf(first);
        int secondIndex = src.indexOf(second);
        return firstIndex < secondIndex;
    }

    private static void expectedOrder(List<?> src, Object first, Object second) {
        int firstIndex = src.indexOf(first);
        int secondIndex = src.indexOf(second);

        if (firstIndex >= secondIndex) {
            throw new AssertionError("Wrong order for [" + first + ", " + second + "] in " + src);
        }
    }
}
