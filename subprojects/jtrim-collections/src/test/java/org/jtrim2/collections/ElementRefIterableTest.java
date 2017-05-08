package org.jtrim2.collections;

import java.util.Iterator;
import org.jtrim2.collections.RefList.ElementRef;
import org.junit.Test;

import static org.junit.Assert.*;

public class ElementRefIterableTest {
    /**
     * Test of iterator method, of class ElementRefIterable.
     */
    @Test
    public void testIterator() {
        int[] expected = new int[]{10, 11, 13};
        RefList<Integer> list = new RefLinkedList<>();
        for (int i = 0; i < expected.length; i++) {
            list.add(expected[i]);
        }

        ElementRefIterable<Integer> iterable = new ElementRefIterable<>(list);
        Iterator<ElementRef<Integer>> itr = iterable.iterator();
        assertTrue(itr instanceof ElementRefIterator);

        for (int i = 0; i < expected.length; i++) {
            assertTrue(itr.hasNext());
            assertEquals(expected[i], itr.next().getElement().intValue());
        }
        assertFalse(itr.hasNext());
    }
}
