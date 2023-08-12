package org.jtrim2.taskgraph.basic;

import org.jtrim2.taskgraph.TaskNodeKey;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RestrictableNodeTest {
    @Test
    public void testProperties() {
        TaskNodeKey<Object, Object> key = TestNodes.node("My-Test-Key");
        Runnable action = mock(Runnable.class);
        RestrictableNode restrictableNode = new RestrictableNode(key, action);

        assertSame("getReleaseAction", action, restrictableNode.getReleaseAction());
        assertSame("getNodeKey", key, restrictableNode.getNodeKey());
    }

    @Test
    public void testToString() {
        TaskNodeKey<Object, Object> key = TestNodes.node("My-Test-Key");
        Runnable action = mock(Runnable.class);
        RestrictableNode restrictableNode = new RestrictableNode(key, action);

        assertTrue("toString", restrictableNode.toString().contains(key.toString()));
    }

    @Test
    public void testRelease() {
        TaskNodeKey<Object, Object> key = TestNodes.node("My-Test-Key");
        Runnable action = mock(Runnable.class);
        RestrictableNode restrictableNode = new RestrictableNode(key, action);

        verifyNoInteractions(action);
        restrictableNode.release();
        verify(action).run();
    }
}
