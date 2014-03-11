package org.jtrim.collections;

import java.util.NoSuchElementException;
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
public class ElementRefIteratorTest {

    public ElementRefIteratorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static RefList<Integer> createTestList(int elementCount) {
        RefList<Integer> list = new RefLinkedList<>();
        for (int i = 0; i < elementCount; i++) {
            list.add(i);
        }
        return list;
    }

    private static ElementRefIterator<Integer> createTestIterator(int size) {
        return createTestIterator(size, 0);
    }

    private static ElementRefIterator<Integer> createTestIterator(int size, int startIndex) {
        if (size == 0) {
            return new ElementRefIterator<>(null);
        }
        return new ElementRefIterator<>(createTestList(size).getReference(startIndex));
    }

    /**
     * Test of hasNext method, of class ElementRefIterator.
     */
    @Test
    public void testHasNext() {
        assertFalse(createTestIterator(0).hasNext());
        assertTrue(createTestIterator(1).hasNext());
        assertTrue(createTestIterator(2).hasNext());
    }

    /**
     * Test of next method, of class ElementRefIterator.
     */
    @Test
    public void testNext() {
        final int size = 5;
        ElementRefIterator<Integer> itr = createTestIterator(size);
        for (int i = 0; i < size; i++) {
            ElementRef<Integer> current = itr.next();
            assertEquals(current.getElement().intValue(), i);
        }
    }

    @Test
    public void testComplexForward() {
        final int size = 5;
        ElementRefIterator<Integer> itr = createTestIterator(size);
        int seen = 0;
        while (itr.hasNext()) {
            ElementRef<Integer> current = itr.next();
            assertEquals(current.getElement().intValue(), seen);
            seen++;
        }

        assertEquals(seen, size);
    }

    /**
     * Test of remove method, of class ElementRefIterator.
     */
    @Test
    public void testRemove() {
        final int size = 5;
        ElementRefIterator<Integer> itr = createTestIterator(size);
        for (int i = 0; i < size; i++) {
            itr.next();
            itr.remove();
        }
        assertFalse(itr.hasPrevious());
        assertFalse(itr.hasNext());
    }

    /**
     * Test of hasPrevious method, of class ElementRefIterator.
     */
    @Test
    public void testHasPrevious() {
        assertFalse(createTestIterator(0).hasPrevious());
        assertFalse(createTestIterator(1).hasPrevious());
        assertFalse(createTestIterator(2).hasPrevious());
        assertTrue(createTestIterator(2, 1).hasPrevious());
        assertTrue(createTestIterator(5, 3).hasPrevious());
    }

    /**
     * Test of previous method, of class ElementRefIterator.
     */
    @Test
    public void testPrevious() {
        final int size = 5;
        ElementRefIterator<Integer> itr = createTestIterator(size, size - 1);
        for (int i = size - 2; i >= 0; i--) {
            ElementRef<Integer> current = itr.previous();
            assertEquals(current.getElement().intValue(), i);
        }
    }

    /**
     * Test of nextIndex method, of class ElementRefIterator.
     */
    @Test
    public void testNextIndex() {
        final int size = 5;
        ElementRefIterator<Integer> itr = createTestIterator(size);
        for (int i = 0; i < size; i++) {
            Integer expectedIndex = itr.nextIndex();
            itr.next();
            assertEquals(expectedIndex.intValue(), i);
        }
    }

    /**
     * Test of previousIndex method, of class ElementRefIterator.
     */
    @Test
    public void testPreviousIndex() {
        final int size = 5;
        ElementRefIterator<Integer> itr = createTestIterator(size, size - 1);
        for (int i = size - 2; i >= 0; i--) {
            Integer expectedIndex = itr.previousIndex();
            itr.previous();
            assertEquals(expectedIndex.intValue(), i);
        }
    }

    /**
     * Test of setElement method, of class ElementRefIterator.
     */
    @Test
    public void testSetElement() {
        final int size = 5;
        final int offset = 100;

        ElementRefIterator<Integer> itr;

        itr = createTestIterator(size);
        for (int i = 0; i < size; i++) {
            itr.next();
            itr.setElement(i + offset);
        }
        for (int i = 0; i < size; i++) {
            itr.previous();
        }
        assertFalse(itr.hasPrevious());
        for (int i = 0; i < size; i++) {
            ElementRef<Integer> current = itr.next();
            assertEquals(current.getElement().intValue(), i + offset);
        }

        itr = createTestIterator(size, size - 1);
        itr.next();
        for (int i = size - 1; i >= 0; i--) {
            itr.previous();
            itr.setElement(i + offset);
        }
        assertFalse(itr.hasPrevious());
        for (int i = 0; i < size; i++) {
            ElementRef<Integer> current = itr.next();
            assertEquals(current.getElement().intValue(), i + offset);
        }
    }

    /**
     * Test of addElement method, of class ElementRefIterator.
     */
    @Test
    public void testAddElement() {
        final int size = 5;
        ElementRefIterator<Integer> itr = createTestIterator(0);
        for (int i = 0; i < size; i++) {
            itr.addElement(i);
            assertFalse(itr.hasNext());
            assertTrue(itr.hasPrevious());
        }

        for (int i = size - 1; i >= 0; i--) {
            RefList.ElementRef<Integer> ref = itr.previous();
            assertEquals(ref.getElement().intValue(), i);
        }
        assertFalse(itr.hasPrevious());
    }

    /**
     * Test of addElement method, of class ElementRefIterator.
     */
    @Test
    public void testAddElementToNonEmpty() {
        ElementRefIterator<Integer> itr = createTestIterator(2);

        itr.next();
        itr.addElement(10);
        itr.addElement(11);

        assertEquals(1, itr.next().getElement().intValue());
        assertFalse(itr.hasNext());

        assertEquals(1, itr.previous().getElement().intValue());
        assertEquals(11, itr.previous().getElement().intValue());
        assertEquals(10, itr.previous().getElement().intValue());
        assertEquals(0, itr.previous().getElement().intValue());

        assertFalse(itr.hasPrevious());
    }

        /**
     * Test of addElement method, of class ElementRefIterator.
     */
    @Test
    public void testAddElementToNonEmptyAfterPrevious() {
        ElementRefIterator<Integer> itr = createTestIterator(2);

        itr.next();
        itr.next();
        itr.previous();
        itr.addElement(10);
        itr.addElement(11);

        assertEquals(1, itr.next().getElement().intValue());
        assertFalse(itr.hasNext());

        assertEquals(1, itr.previous().getElement().intValue());
        assertEquals(11, itr.previous().getElement().intValue());
        assertEquals(10, itr.previous().getElement().intValue());
        assertEquals(0, itr.previous().getElement().intValue());

        assertFalse(itr.hasPrevious());
    }

    /**
     * Test of set method, of class ElementRefIterator.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testSet() {
        createTestIterator(1).set(new DetachedListRef<>(0));
    }

    /**
     * Test of add method, of class ElementRefIterator.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testAdd() {
        createTestIterator(1).add(new DetachedListRef<>(0));
    }

    /**
     * Test NoSuchElementException at the end of the iterator.
     */
    @Test(expected = NoSuchElementException.class)
    public void testTooManyNext() {
        ElementRefIterator<Integer> itr = createTestIterator(1);
        try {
            assertFalse(itr.hasPrevious());
            assertTrue(itr.hasNext());
            itr.next();
            assertTrue(itr.hasPrevious());
            assertFalse(itr.hasNext());
        } catch (NoSuchElementException ex) {
            throw new RuntimeException("Early NoSuchElementException", ex);
        }

        itr.next();
    }

    /**
     * Test NoSuchElementException at the end of the iterator.
     */
    @Test(expected = NoSuchElementException.class)
    public void testTooManyPrevious() {
        ElementRefIterator<Integer> itr = createTestIterator(1);

        try {
            itr.next();
            itr.previous();
        } catch (NoSuchElementException ex) {
            throw new RuntimeException("Early NoSuchElementException", ex);
        }

        itr.previous();
    }

    @Test
    public void testRemoveFromPrevious() {
        int itrLength = 5;
        ElementRefIterator<Integer> itr = createTestIterator(itrLength);
        for (int i = 0; i < itrLength; i++) {
            itr.next();
        }
        for (int i = 0; i < itrLength; i++) {
            itr.previous();
            itr.remove();
        }

        assertFalse(itr.hasNext());
        assertFalse(itr.hasPrevious());
    }

    @Test
    public void testIndexOfFirst() {
        ElementRefIterator<Integer> itr = createTestIterator(3);
        assertEquals(0, itr.nextIndex());
        assertEquals(-1, itr.previousIndex());
    }

    @Test
    public void testIndexOfEmpty() {
        ElementRefIterator<Integer> itr = createTestIterator(0);
        assertEquals(0, itr.nextIndex());
        assertEquals(-1, itr.previousIndex());
    }

    @Test
    public void testPreviousIndexAfterNext() {
        ElementRefIterator<Integer> itr = createTestIterator(3);
        itr.next();
        itr.next();
        assertEquals(1, itr.previousIndex());
    }

    /**
     * The iterator must throw IllegalStateException when setElement is called
     * but neither next nor previous has been called.
     */
    @Test(expected = IllegalStateException.class)
    public void testIllegalSetElementCall1() {
        ElementRefIterator<Integer> itr = createTestIterator(3);
        itr.setElement(0);
    }

    /**
     * The iterator must throw IllegalStateException when setElement is called
     * but remove has been called.
     */
    @Test(expected = IllegalStateException.class)
    public void testIllegalSetElementCall2() {
        ElementRefIterator<Integer> itr = createTestIterator(3);
        itr.next();
        itr.remove();
        itr.setElement(0);
    }

    /**
     * The iterator must throw IllegalStateException when remove is called
     * but next has not been called.
     */
    @Test(expected = IllegalStateException.class)
    public void testIllegalRemoveCall() {
        ElementRefIterator<Integer> itr = createTestIterator(3);
        itr.remove();
    }
}
