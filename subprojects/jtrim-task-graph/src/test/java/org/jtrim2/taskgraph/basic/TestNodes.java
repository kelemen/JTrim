package org.jtrim2.taskgraph.basic;

import org.jtrim2.taskgraph.TaskFactoryKey;
import org.jtrim2.taskgraph.TaskNodeKey;

public final class TestNodes {
    public static TaskNodeKey<Object, Object> node(Object key) {
        return new TaskNodeKey<>(new TaskFactoryKey<>(Object.class, Object.class), key);
    }

    public static TaskNodeKey<?, ?> matrixNode(int row, int column) {
        return node("node(" + row + ", " + column + ")");
    }

    private TestNodes() {
        throw new AssertionError();
    }
}
