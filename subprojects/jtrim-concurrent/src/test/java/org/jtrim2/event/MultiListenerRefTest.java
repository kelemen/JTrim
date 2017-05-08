package org.jtrim2.event;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MultiListenerRefTest {
    @Test
    public void testZero() {
        ListenerRef ref = MultiListenerRef.combine();
        assertFalse(ref.isRegistered());
        ref.unregister();
        assertFalse(ref.isRegistered());
    }

    private static void testMultiple(int count) {
        testMultiple(count, MultiListenerRef::combine);
    }

    public static void testMultiple(int count, Combiner combiner) {
        for (boolean[] registered: combinations(count)) {
            ListenerRef[] refs = new ListenerRef[count];
            for (int i = 0; i < refs.length; i++) {
                refs[i] = mock(ListenerRef.class);
                stub(refs[i].isRegistered()).toReturn(registered[i]);
            }

            boolean allUnregistered = true;
            for (int i = 0; i < registered.length; i++) {
                if (registered[i]) {
                    allUnregistered = false;
                    break;
                }
            }

            ListenerRef ref = combiner.combine(refs);
            assertEquals(!allUnregistered, ref.isRegistered());
            for (ListenerRef mockRef: refs) {
                verify(mockRef, never()).unregister();
            }
            ref.unregister();
            for (ListenerRef mockRef: refs) {
                verify(mockRef).unregister();
            }
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

        stub(ref1.isRegistered()).toReturn(true);
        stub(ref2.isRegistered()).toReturn(true);

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
            }
            else if (error == error2) {
                assertSame(error1, suppressed[0]);
            }
            else {
                fail("Unexpected exception: " + error);
            }
        }
    }

    private static boolean[] bits(int value, int bitLength) {
        boolean[] result = new boolean[bitLength];
        int currentValue = value;
        for (int i = 0; i < result.length; i++) {
            result[i] = (currentValue & 1) != 0;
            currentValue = currentValue >>> 1;
        }
        return result;
    }

    private static Iterable<boolean[]> combinations(final int count) {
        if (count > 30 || count < 1) {
            throw new IllegalArgumentException();
        }

        final int iteratorLength = 1 << count; // 2 ^ count
        return () -> new Iterator<boolean[]>() {
            private int nextValue = 0;

            @Override
            public boolean hasNext() {
                return nextValue < iteratorLength;
            }

            @Override
            public boolean[] next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                boolean[] result = bits(nextValue, count);
                nextValue++;
                return result;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static interface Combiner {
        public ListenerRef combine(ListenerRef[] refs);
    }
}
