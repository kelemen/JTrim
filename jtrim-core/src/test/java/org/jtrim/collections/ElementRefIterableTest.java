package org.jtrim.collections;

import java.util.Iterator;
import org.jtrim.collections.RefList.ElementRef;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class ElementRefIterableTest {

    public ElementRefIterableTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

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
