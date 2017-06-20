package org.jtrim2.event;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MultiListenerRefTest {
    @Test
    public void testZero() {
        ListenerRef ref = MultiListenerRef.combine();
        ref.unregister();
    }

    private static void testMultiple(int count) {
        testMultiple(count, MultiListenerRef::combine);
    }

    public static void testMultiple(int count, Combiner combiner) {
        ListenerRef[] refs = new ListenerRef[count];
        for (int i = 0; i < refs.length; i++) {
            refs[i] = mock(ListenerRef.class);
        }

        ListenerRef ref = combiner.combine(refs);
        for (ListenerRef mockRef: refs) {
            verify(mockRef, never()).unregister();
        }
        ref.unregister();
        for (ListenerRef mockRef: refs) {
            verify(mockRef).unregister();
        }
    }

    @Test
    public void testSingle() {
        testMultiple(1);
    }

    @Test
    public void testTwo() {
        testMultiple(2);
    }

    @Test
    public void testThree() {
        testMultiple(3);
    }

    @Test
    public void testFailingUnregisters() {
        ListenerRef ref1 = mock(ListenerRef.class);
        ListenerRef ref2 = mock(ListenerRef.class);

        Throwable error1 = new RuntimeException();
        Throwable error2 = new RuntimeException();

        doThrow(error1).when(ref1).unregister();
        doThrow(error2).when(ref2).unregister();

        try {
            MultiListenerRef.combine(ref1, ref2).unregister();
            fail("Expected exception from unregister.");
        } catch (Throwable error) {
            Throwable[] suppressed = error.getSuppressed();
            assertEquals("Must have suppressed exception.", 1, suppressed.length);

            if (error == error1) {
                assertSame(error2, suppressed[0]);
            } else if (error == error2) {
                assertSame(error1, suppressed[0]);
            } else {
                fail("Unexpected exception: " + error);
            }
        }
    }

    public static interface Combiner {
        public ListenerRef combine(ListenerRef[] refs);
    }
}
