package org.jtrim.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.ListIterator;
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
public class RefLinkedListTest {
    public RefLinkedListTest() {
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

    private static RefLinkedList<Integer> createTestList(int size) {
        RefLinkedList<Integer> result = new RefLinkedList<>();
        for (int i = 0; i < size; i++) {
            result.add(i);
        }
        return result;
    }

    private static RefLinkedList<Integer> createTestListWithContent(int... content) {
        RefLinkedList<Integer> result = new RefLinkedList<>();
        for (int i = 0; i < content.length; i++) {
            result.add(content[i]);
        }
        return result;
    }

    private static void checkListContent(RefList<Integer> list, int... content) {
        assertEquals(content.length, list.size());

        // Check from both side to detect failures in the links in both ways.
        ElementRef<Integer> ref = list.getFirstReference();
        for (int i = 0; i < content.length; i++) {
            Integer current = ref.getElement();
            assertEquals(content[i], current.intValue());

            ref = ref.getNext(1);
        }
        assertNull(ref);

        ref = list.getLastReference();
        for (int i = content.length - 1; i >= 0; i--) {
            Integer current = ref.getElement();
            assertEquals(content[i], current.intValue());

            ref = ref.getPrevious(1);
        }
        assertNull(ref);
    }

    /**
     * Test of size method, of class RefLinkedList.
     */
    @Test
    public void testSize() {
        assertEquals(0, createTestList(0).size());
        assertEquals(5, createTestList(5).size());
    }

    /**
     * Test of isEmpty method, of class RefLinkedList.
     */
    @Test
    public void testIsEmpty() {
        assertTrue(createTestList(0).isEmpty());
        assertFalse(createTestList(1).isEmpty());
        assertFalse(createTestList(5).isEmpty());
    }

    /**
     * Test of findFirstReference method, of class RefLinkedList.
     */
    @Test
    public void testFindFirstReference() {
        int listSize = 5;
        RefLinkedList<Integer> list = createTestList(listSize);
        for (int i = 0; i < listSize; i++) {
            list.add(i);
        }

        for (int i = 0; i < listSize; i++) {
            ElementRef<Integer> ref = list.findFirstReference(i);
            assertEquals(i, ref.getElement().intValue());
            assertEquals(i, ref.getIndex());
        }

        assertNull(list.findFirstReference(listSize + 1));
    }

    /**
     * Test of findLastReferece method, of class RefLinkedList.
     */
    @Test
    public void testFindLastReferece() {
        int listSize = 5;
        RefLinkedList<Integer> list = createTestList(listSize);
        for (int i = 0; i < listSize; i++) {
            list.add(i);
        }

        for (int i = 0; i < listSize; i++) {
            ElementRef<Integer> ref = list.findLastReferece(i);
            assertEquals(i, ref.getElement().intValue());
            assertEquals(listSize + i, ref.getIndex());
        }

        assertNull(list.findLastReferece(listSize + 1));
    }

    /**
     * Test of findReference method, of class RefLinkedList.
     */
    @Test
    public void testFindReference() {
        int listSize = 5;
        RefLinkedList<Integer> list = createTestList(listSize);
        for (int i = 0; i < listSize; i++) {
            list.add(i);
        }

        for (int i = 0; i < listSize; i++) {
            ElementRef<Integer> ref = list.findReference(i);
            assertEquals(i, ref.getElement().intValue());
            assertEquals(i, ref.getIndex());
        }

        assertNull(list.findReference(listSize + 1));
    }

    /**
     * Test of getFirstReference method, of class RefLinkedList.
     */
    @Test
    public void testGetFirstReference() {
        assertNull(createTestList(0).getFirstReference());
        assertEquals(0, createTestList(1).getFirstReference().getElement().intValue());
        assertEquals(0, createTestList(5).getFirstReference().getElement().intValue());
    }

    /**
     * Test of getLastReference method, of class RefLinkedList.
     */
    @Test
    public void testGetLastReference() {
        assertNull(createTestList(0).getLastReference());
        assertEquals(0, createTestList(1).getLastReference().getElement().intValue());
        assertEquals(4, createTestList(5).getLastReference().getElement().intValue());
    }

    /**
     * Test of getReference method, of class RefLinkedList.
     */
    @Test
    public void testGetReference() {
        assertEquals(0, createTestList(1).getReference(0).getElement().intValue());

        assertEquals(2, createTestList(5).getReference(2).getElement().intValue());
        assertEquals(4, createTestList(5).getReference(4).getElement().intValue());
    }

    /**
     * Test that getReference method throws an exception on an empty list.
     * Case: 1
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegalGetReferenceOnEmpty1() {
        assertEquals(0, createTestList(0).getReference(0).getElement().intValue());
    }

    /**
     * Test that getReference method throws an exception on an empty list.
     * Case: 2
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegalGetReferenceOnEmpty2() {
        assertEquals(0, createTestList(0).getReference(-1).getElement().intValue());
    }

    /**
     * Test that getReference method throws an exception on an empty list.
     * Case: 3
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testIllegalGetReferenceOnEmpty3() {
        assertEquals(0, createTestList(0).getReference(10).getElement().intValue());
    }

    /**
     * Test of contains method, of class RefLinkedList.
     */
    @Test
    public void testContains() {
        int listSize = 5;
        RefLinkedList<Integer> list = createTestList(listSize);
        for (int i = 0; i < 5; i++) {
            assertTrue("Must contain: " + i, list.contains(i));
        }
        assertFalse(list.contains(-1));
    }

    /**
     * Test of iterator method, of class RefLinkedList.
     */
    @Test
    public void testIterator() {
        int listSize = 5;
        RefLinkedList<Integer> list = createTestList(listSize);
        Iterator<Integer> itr = list.iterator();

        for (int i = 0; i < listSize; i++) {
            assertTrue(itr.hasNext());
            assertEquals(i, itr.next().intValue());
        }

        assertFalse(itr.hasNext());
    }

    /**
     * Test of add method, of class RefLinkedList.
     */
    @Test
    public void testAddAndGetAtIndex() {
        int listSize = 5;
        RefLinkedList<Integer> list = createTestList(listSize);
        for (int i = 0; i < listSize; i++) {
            assertEquals(i, list.get(i).intValue());
        }
    }

    /**
     * Test of remove method, of class RefLinkedList.
     */
    @Test
    public void testRemoveObject() {
        int listSize = 5;
        RefLinkedList<Integer> list = createTestList(listSize);

        assertTrue(list.remove(Integer.valueOf(3)));
        checkListContent(list, 0, 1, 2, 4);

        assertFalse(list.remove(Integer.valueOf(3)));
        checkListContent(list, 0, 1, 2, 4);

        assertTrue(list.remove(Integer.valueOf(4)));
        checkListContent(list, 0, 1, 2);

        assertTrue(list.remove(Integer.valueOf(0)));
        checkListContent(list, 1, 2);

        assertTrue(list.remove(Integer.valueOf(1)));
        checkListContent(list, 2);

        assertTrue(list.remove(Integer.valueOf(2)));
        assertTrue(list.isEmpty());
    }

    /**
     * Test of clear method, of class RefLinkedList.
     */
    @Test
    public void testClear() {
        RefLinkedList<Integer> list = createTestList(5);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    /**
     * Test of set method, of class RefLinkedList.
     */
    @Test
    public void testSetAtIndex() {
        RefLinkedList<Integer> list = createTestList(5);

        list.set(3, 13);
        checkListContent(list, 0, 1, 2, 13, 4);

        list.set(4, 14);
        checkListContent(list, 0, 1, 2, 13, 14);

        list.set(0, 10);
        checkListContent(list, 10, 1, 2, 13, 14);
    }

    /**
     * Test of addFirstGetReference method, of class RefLinkedList.
     */
    @Test
    public void testAddFirstGetReference() {
        RefLinkedList<Integer> list = createTestList(5);

        ElementRef<Integer> reference = list.addFirstGetReference(-1);
        assertEquals(-1, reference.getElement().intValue());
        checkListContent(list, -1, 0, 1, 2, 3, 4);
    }

    /**
     * Test of addFirstGetReference method, of class RefLinkedList when the
     * list is empty.
     */
    @Test
    public void testAddFirstGetReferenceWhenEmpty() {
        RefLinkedList<Integer> list = createTestList(0);

        ElementRef<Integer> reference = list.addFirstGetReference(0);
        assertEquals(0, reference.getElement().intValue());
        checkListContent(list, 0);
    }

    /**
     * Test of addLastGetReference method, of class RefLinkedList.
     */
    @Test
    public void testAddLastGetReference() {
        RefLinkedList<Integer> list = createTestList(5);

        ElementRef<Integer> reference = list.addLastGetReference(5);
        assertEquals(5, reference.getElement().intValue());
        checkListContent(list, 0, 1, 2, 3, 4, 5);
    }

    /**
     * Test of addLastGetReference method, of class RefLinkedList when the
     * list is empty.
     */
    @Test
    public void testAddLastGetReferenceWhenEmpty() {
        RefLinkedList<Integer> list = createTestList(0);

        ElementRef<Integer> reference = list.addLastGetReference(0);
        assertEquals(0, reference.getElement().intValue());
        checkListContent(list, 0);
    }

    /**
     * Test of addGetReference method, of class RefLinkedList.
     */
    @Test
    public void testAddGetReference() {
        RefLinkedList<Integer> list = createTestList(5);
        assertEquals(5, list.addGetReference(5).getElement().intValue());
        checkListContent(list, 0, 1, 2, 3, 4, 5);
    }

    /**
     * Test of addGetReference method, of class RefLinkedList.
     */
    @Test
    public void testAddGetReferenceWithEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        assertEquals(0, list.addGetReference(0).getElement().intValue());
        checkListContent(list, 0);
    }

    /**
     * Test of addGetReference method, of class RefLinkedList.
     */
    @Test
    public void testAddGetReferenceAtIndex() {
        RefLinkedList<Integer> list = createTestList(5);

        assertEquals(13, list.addGetReference(3, 13).getElement().intValue());
        checkListContent(list, 0, 1, 2, 13, 3, 4);

        assertEquals(16, list.addGetReference(6, 16).getElement().intValue());
        checkListContent(list, 0, 1, 2, 13, 3, 4, 16);

        assertEquals(10, list.addGetReference(0, 10).getElement().intValue());
        checkListContent(list, 10, 0, 1, 2, 13, 3, 4, 16);
    }

    /**
     * Test of add method, of class RefLinkedList.
     */
    @Test
    public void testAddAtIndex() {
        RefLinkedList<Integer> list = createTestList(5);

        list.add(3, 13);
        checkListContent(list, 0, 1, 2, 13, 3, 4);

        list.add(6, 16);
        checkListContent(list, 0, 1, 2, 13, 3, 4, 16);

        list.add(0, 10);
        checkListContent(list, 10, 0, 1, 2, 13, 3, 4, 16);
    }

    /**
     * Test of remove method, of class RefLinkedList.
     */
    @Test
    public void testRemoveAtIndex() {
        RefLinkedList<Integer> list = createTestList(5);

        list.remove(3);
        checkListContent(list, 0, 1, 2, 4);

        list.remove(3);
        checkListContent(list, 0, 1, 2);

        list.remove(0);
        checkListContent(list, 1, 2);

        list.remove(0);
        list.remove(0);
        assertTrue(list.isEmpty());
    }

    /**
     * Test of listIterator method, of class RefLinkedList.
     */
    @Test(expected = NoSuchElementException.class)
    public void testListIteratorTooManyNext() {
        RefLinkedList<Integer> list = createTestList(2);
        ListIterator<Integer> itr = list.listIterator();
        try {
            itr.next();
            itr.next();
        } catch (NoSuchElementException ex) {
            throw new RuntimeException(ex);
        }

        itr.next();
    }

    /**
     * Test of listIterator method, of class RefLinkedList.
     */
    @Test(expected = NoSuchElementException.class)
    public void testListIteratorTooManyPrevious() {
        RefLinkedList<Integer> list = createTestList(2);
        ListIterator<Integer> itr = list.listIterator();
        itr.previous();
    }

    /**
     * Test of listIterator method, of class RefLinkedList.
     */
    @Test(expected = IllegalStateException.class)
    public void testListIteratorEarlyRemove() {
        RefLinkedList<Integer> list = createTestList(2);
        ListIterator<Integer> itr = list.listIterator();
        itr.remove();
    }

    /**
     * Test of listIterator method, of class RefLinkedList.
     */
    @Test(expected = IllegalStateException.class)
    public void testListIteratorTwoRemove() {
        RefLinkedList<Integer> list = createTestList(2);
        ListIterator<Integer> itr = list.listIterator();
        try {
            itr.next();
            itr.remove();
        } catch (IllegalStateException ex) {
            throw new RuntimeException(ex);
        }

        itr.remove();
    }

    /**
     * Test of listIterator method, of class RefLinkedList.
     */
    @Test(expected = IllegalStateException.class)
    public void testListIteratorRemoveAfterAdd() {
        RefLinkedList<Integer> list = createTestList(2);
        ListIterator<Integer> itr = list.listIterator();
        try {
            itr.next();
            itr.add(100);
        } catch (IllegalStateException ex) {
            throw new RuntimeException(ex);
        }

        itr.remove();
    }

    /**
     * Test of listIterator method, of class RefLinkedList.
     */
    @Test(expected = IllegalStateException.class)
    public void testListIteratorSetWithoutNext() {
        RefLinkedList<Integer> list = createTestList(2);
        ListIterator<Integer> itr = list.listIterator();
        itr.set(0);
    }

    /**
     * Test of listIterator method, of class RefLinkedList.
     */
    @Test
    public void testListIterator() {
        RefLinkedList<Integer> list = createTestList(5);

        ListIterator<Integer> itr = list.listIterator();
        assertEquals(-1, itr.previousIndex());

        assertFalse(itr.hasPrevious());
        for (int i = 0; i < 5; i++) {
            assertTrue(itr.hasNext());
            assertEquals(i, itr.nextIndex());
            assertEquals(i, itr.next().intValue());
            assertEquals(i, itr.previousIndex());
            assertTrue(itr.hasPrevious());
        }
        assertFalse(itr.hasNext());

        assertEquals(4, itr.previous().intValue());
        assertEquals(3, itr.previous().intValue());
        itr.set(13);
        checkListContent(list, 0, 1, 2, 13, 4);

        assertEquals(13, itr.next().intValue());
        assertEquals(4, itr.next().intValue());
        itr.set(14);
        checkListContent(list, 0, 1, 2, 13, 14);

        assertEquals(14, itr.previous().intValue());
        assertEquals(13, itr.previous().intValue());
        itr.add(99);
        assertEquals(13, itr.next().intValue());
        assertEquals(13, itr.previous().intValue());

        checkListContent(list, 0, 1, 2, 99, 13, 14);

        itr.remove();
        checkListContent(list, 0, 1, 2, 99, 14);

        assertEquals(99, itr.previous().intValue());
        assertEquals(2, itr.previous().intValue());
        assertEquals(2, itr.next().intValue());
        itr.remove();

        checkListContent(list, 0, 1, 99, 14);

        assertEquals(1, itr.previous().intValue());
        assertEquals(0, itr.previous().intValue());
        assertFalse(itr.hasPrevious());

        assertEquals(0, itr.next().intValue());
        itr.remove();
        assertEquals(1, itr.next().intValue());
        itr.remove();
        assertEquals(99, itr.next().intValue());
        itr.remove();
        assertEquals(14, itr.next().intValue());
        itr.remove();
        assertTrue(list.isEmpty());
    }

    /**
     * Test of listIterator method, of class RefLinkedList.
     */
    @Test
    public void testListIteratorFromIndex() {
        int listSize = 5;
        int startIndex = 2;
        RefLinkedList<Integer> list = createTestList(listSize);

        ListIterator<Integer> itr = list.listIterator(startIndex);

        assertTrue(itr.hasPrevious());
        for (int i = startIndex; i < listSize; i++) {
            assertTrue(itr.hasNext());
            assertEquals(i, itr.next().intValue());
            assertTrue(itr.hasPrevious());
        }

        assertFalse(itr.hasNext());

        for (int i = listSize - 1; i >= 0; i--) {
            assertTrue(itr.hasPrevious());
            assertEquals(i, itr.previous().intValue());
            assertTrue(itr.hasNext());
        }

        assertFalse(itr.hasPrevious());
    }

    /**
     * Test of listIterator method, of class RefLinkedList.
     */
    @Test
    public void testListIteratorFromIndex0() {
        int listSize = 5;
        int startIndex = 0;
        RefLinkedList<Integer> list = createTestList(listSize);

        ListIterator<Integer> itr = list.listIterator(startIndex);

        assertFalse(itr.hasPrevious());
        for (int i = startIndex; i < listSize; i++) {
            assertTrue(itr.hasNext());
            assertEquals(i, itr.next().intValue());
            assertTrue(itr.hasPrevious());
        }

        assertFalse(itr.hasNext());

        for (int i = listSize - 1; i >= 0; i--) {
            assertTrue(itr.hasPrevious());
            assertEquals(i, itr.previous().intValue());
            assertTrue(itr.hasNext());
        }

        assertFalse(itr.hasPrevious());
    }

    /**
     * Test of listIterator method, of class RefLinkedList.
     */
    @Test
    public void testListIteratorFromEnd() {
        int listSize = 5;
        RefLinkedList<Integer> list = createTestList(listSize);

        ListIterator<Integer> itr = list.listIterator(listSize);
        assertFalse(itr.hasNext());

        for (int i = listSize - 1; i >= 0; i--) {
            assertTrue(itr.hasPrevious());
            assertEquals(i, itr.previous().intValue());
            assertTrue(itr.hasNext());
        }

        assertFalse(itr.hasPrevious());
    }

    /**
     * Test of addFirst method, of class RefLinkedList.
     */
    @Test
    public void testAddFirst() {
        RefLinkedList<Integer> list = createTestList(5);
        list.addFirst(-1);
        checkListContent(list, -1, 0, 1, 2, 3, 4);
    }

    /**
     * Test of addFirst method, of class RefLinkedList.
     */
    @Test
    public void testAddFirstToEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        list.addFirst(0);
        checkListContent(list, 0);
    }

    /**
     * Test of addLast method, of class RefLinkedList.
     */
    @Test
    public void testAddLast() {
        RefLinkedList<Integer> list = createTestList(5);
        list.addLast(5);
        checkListContent(list, 0, 1, 2, 3, 4, 5);
    }

    /**
     * Test of addLast method, of class RefLinkedList.
     */
    @Test
    public void testAddLastToEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        list.addLast(0);
        checkListContent(list, 0);
    }

    /**
     * Test of offerFirst method, of class RefLinkedList.
     */
    @Test
    public void testOfferFirst() {
        RefLinkedList<Integer> list = createTestList(5);
        assertTrue(list.offerFirst(-1));
        checkListContent(list, -1, 0, 1, 2, 3, 4);
    }

    /**
     * Test of offerFirst method, of class RefLinkedList.
     */
    @Test
    public void testOfferFirstToEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        assertTrue(list.offerFirst(0));
        checkListContent(list, 0);
    }

    /**
     * Test of offerLast method, of class RefLinkedList.
     */
    @Test
    public void testOfferLast() {
        RefLinkedList<Integer> list = createTestList(5);
        assertTrue(list.offerLast(5));
        checkListContent(list, 0, 1, 2, 3, 4, 5);
    }

    /**
     * Test of offerLast method, of class RefLinkedList.
     */
    @Test
    public void testOfferLastToEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        assertTrue(list.offerLast(0));
        checkListContent(list, 0);
    }

    /**
     * Test of removeFirst method, of class RefLinkedList.
     */
    @Test
    public void testRemoveFirst() {
        RefLinkedList<Integer> list = createTestList(5);

        assertEquals(0, list.removeFirst().intValue());
        checkListContent(list, 1, 2, 3, 4);

        assertEquals(1, list.removeFirst().intValue());
        checkListContent(list, 2, 3, 4);

        assertEquals(2, list.removeFirst().intValue());
        checkListContent(list, 3, 4);

        assertEquals(3, list.removeFirst().intValue());
        checkListContent(list, 4);

        assertEquals(4, list.removeFirst().intValue());
        assertTrue(list.isEmpty());
    }

    /**
     * Test of removeFirst method, of class RefLinkedList.
     */
    @Test(expected = NoSuchElementException.class)
    public void testRemoveFirstOnEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        list.removeFirst();
    }

    /**
     * Test of removeLast method, of class RefLinkedList.
     */
    @Test
    public void testRemoveLast() {
        RefLinkedList<Integer> list = createTestList(5);

        assertEquals(4, list.removeLast().intValue());
        checkListContent(list, 0, 1, 2, 3);

        assertEquals(3, list.removeLast().intValue());
        checkListContent(list, 0, 1, 2);

        assertEquals(2, list.removeLast().intValue());
        checkListContent(list, 0, 1);

        assertEquals(1, list.removeLast().intValue());
        checkListContent(list, 0);

        assertEquals(0, list.removeLast().intValue());
        assertTrue(list.isEmpty());
    }

    /**
     * Test of removeLast method, of class RefLinkedList.
     */
    @Test(expected = NoSuchElementException.class)
    public void testRemoveLastOnEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        list.removeLast();
    }

    /**
     * Test of pollFirst method, of class RefLinkedList.
     */
    @Test
    public void testPollFirst() {
        RefLinkedList<Integer> list = createTestList(5);

        assertEquals(0, list.pollFirst().intValue());
        checkListContent(list, 1, 2, 3, 4);

        assertEquals(1, list.pollFirst().intValue());
        checkListContent(list, 2, 3, 4);

        assertEquals(2, list.pollFirst().intValue());
        checkListContent(list, 3, 4);

        assertEquals(3, list.pollFirst().intValue());
        checkListContent(list, 4);

        assertEquals(4, list.pollFirst().intValue());
        assertTrue(list.isEmpty());

        assertNull(list.pollFirst());
    }

    /**
     * Test of pollLast method, of class RefLinkedList.
     */
    @Test
    public void testPollLast() {
        RefLinkedList<Integer> list = createTestList(5);

        assertEquals(4, list.pollLast().intValue());
        checkListContent(list, 0, 1, 2, 3);

        assertEquals(3, list.pollLast().intValue());
        checkListContent(list, 0, 1, 2);

        assertEquals(2, list.pollLast().intValue());
        checkListContent(list, 0, 1);

        assertEquals(1, list.pollLast().intValue());
        checkListContent(list, 0);

        assertEquals(0, list.pollLast().intValue());
        assertTrue(list.isEmpty());

        assertNull(list.pollLast());
    }

    /**
     * Test of getFirst method, of class RefLinkedList.
     */
    @Test
    public void testGetFirst() {
        RefLinkedList<Integer> list = createTestList(5);
        assertEquals(0, list.getFirst().intValue());
        checkListContent(list, 0, 1, 2, 3, 4);
    }

    /**
     * Test of getFirst method, of class RefLinkedList.
     */
    @Test(expected = NoSuchElementException.class)
    public void testGetFirstOnEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        list.getFirst();
    }

    /**
     * Test of getLast method, of class RefLinkedList.
     */
    @Test
    public void testGetLast() {
        RefLinkedList<Integer> list = createTestList(5);
        assertEquals(4, list.getLast().intValue());
        checkListContent(list, 0, 1, 2, 3, 4);
    }

    /**
     * Test of getLast method, of class RefLinkedList.
     */
    @Test(expected = NoSuchElementException.class)
    public void testGetLastOnEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        list.getLast();
    }


    /**
     * Test of peekFirst method, of class RefLinkedList.
     */
    @Test
    public void testPeekFirst() {
        RefLinkedList<Integer> list = createTestList(5);
        assertEquals(0, list.peekFirst().intValue());
        checkListContent(list, 0, 1, 2, 3, 4);
    }

    /**
     * Test of peekFirst method, of class RefLinkedList.
     */
    @Test
    public void testPeekFirstOnEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        assertNull(list.peekFirst());
    }

    /**
     * Test of peekLast method, of class RefLinkedList.
     */
    @Test
    public void testPeekLast() {
        RefLinkedList<Integer> list = createTestList(5);
        assertEquals(4, list.peekLast().intValue());
        checkListContent(list, 0, 1, 2, 3, 4);
    }

    /**
     * Test of peekLast method, of class RefLinkedList.
     */
    @Test
    public void testPeekLastOnEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        assertNull(list.peekLast());
    }

    /**
     * Test of removeFirstOccurrence method, of class RefLinkedList.
     */
    @Test
    public void testRemoveFirstOccurrence() {
        RefLinkedList<Integer> list = createTestListWithContent(0, 1, 2, 0, 1, 2);

        assertTrue(list.removeFirstOccurrence(1));
        checkListContent(list, 0, 2, 0, 1, 2);

        assertTrue(list.removeFirstOccurrence(0));
        checkListContent(list, 2, 0, 1, 2);

        assertTrue(list.removeFirstOccurrence(2));
        checkListContent(list, 0, 1, 2);

        assertTrue(list.removeFirstOccurrence(2));
        checkListContent(list, 0, 1);

        assertFalse(list.removeFirstOccurrence(2));
        checkListContent(list, 0, 1);

        assertTrue(list.removeFirstOccurrence(0));
        checkListContent(list, 1);

        assertTrue(list.removeFirstOccurrence(1));
        assertTrue(list.isEmpty());

        assertFalse(list.removeFirstOccurrence(0));
    }

    /**
     * Test of removeLastOccurrence method, of class RefLinkedList.
     */
    @Test
    public void testRemoveLastOccurrence() {
        RefLinkedList<Integer> list = createTestListWithContent(0, 1, 2, 0, 1, 2);

        assertTrue(list.removeLastOccurrence(1));
        checkListContent(list, 0, 1, 2, 0, 2);

        assertTrue(list.removeLastOccurrence(2));
        checkListContent(list, 0, 1, 2, 0);

        assertTrue(list.removeLastOccurrence(0));
        checkListContent(list, 0, 1, 2);

        assertTrue(list.removeLastOccurrence(0));
        checkListContent(list, 1, 2);

        assertFalse(list.removeLastOccurrence(0));
        checkListContent(list, 1, 2);

        assertTrue(list.removeLastOccurrence(1));
        checkListContent(list, 2);

        assertTrue(list.removeLastOccurrence(2));
        assertTrue(list.isEmpty());

        assertFalse(list.removeLastOccurrence(0));
    }

    /**
     * Test of offer method, of class RefLinkedList.
     */
    @Test
    public void testOffer() {
        RefLinkedList<Integer> list = createTestList(5);
        assertTrue(list.offer(5));
        checkListContent(list, 0, 1, 2, 3, 4, 5);
    }

    /**
     * Test of offer method, of class RefLinkedList.
     */
    @Test
    public void testOfferToEmpty() {
        RefLinkedList<Integer> list = createTestList(0);
        assertTrue(list.offer(0));
        checkListContent(list, 0);
    }

    /**
     * Test of remove method, of class RefLinkedList.
     */
    @Test
    public void testRemove() {
        RefLinkedList<Integer> list = createTestList(5);

        assertEquals(0, list.remove().intValue());
        checkListContent(list, 1, 2, 3, 4);

        assertEquals(1, list.remove().intValue());
        checkListContent(list, 2, 3, 4);

        assertEquals(2, list.remove().intValue());
        checkListContent(list, 3, 4);

        assertEquals(3, list.remove().intValue());
        checkListContent(list, 4);

        assertEquals(4, list.remove().intValue());
        assertTrue(list.isEmpty());
    }

    /**
     * Test of poll method, of class RefLinkedList.
     */
    @Test
    public void testPoll() {
        RefLinkedList<Integer> list = createTestList(5);

        assertEquals(0, list.poll().intValue());
        checkListContent(list, 1, 2, 3, 4);

        assertEquals(1, list.poll().intValue());
        checkListContent(list, 2, 3, 4);

        assertEquals(2, list.poll().intValue());
        checkListContent(list, 3, 4);

        assertEquals(3, list.poll().intValue());
        checkListContent(list, 4);

        assertEquals(4, list.poll().intValue());
        assertTrue(list.isEmpty());

        assertNull(list.poll());
    }

    /**
     * Test of element method, of class RefLinkedList.
     */
    @Test
    public void testElement() {
        RefLinkedList<Integer> list = createTestList(5);
        assertEquals(0, list.element().intValue());
        checkListContent(list, 0, 1, 2, 3, 4);
    }

    /**
     * Test of peek method, of class RefLinkedList.
     */
    @Test
    public void testPeek() {
        RefLinkedList<Integer> list = createTestList(5);
        assertEquals(0, list.peek().intValue());
        checkListContent(list, 0, 1, 2, 3, 4);
    }

    /**
     * Test of push method, of class RefLinkedList.
     */
    @Test
    public void testPush() {
        RefLinkedList<Integer> list = createTestList(5);
        list.push(-1);
        checkListContent(list, -1, 0, 1, 2, 3, 4);
    }

    /**
     * Test of pop method, of class RefLinkedList.
     */
    @Test
    public void testPop() {
        RefLinkedList<Integer> list = createTestList(5);

        assertEquals(0, list.pop().intValue());
        checkListContent(list, 1, 2, 3, 4);

        assertEquals(1, list.pop().intValue());
        checkListContent(list, 2, 3, 4);

        assertEquals(2, list.pop().intValue());
        checkListContent(list, 3, 4);

        assertEquals(3, list.pop().intValue());
        checkListContent(list, 4);

        assertEquals(4, list.pop().intValue());
        assertTrue(list.isEmpty());
    }

    /**
     * Test of descendingIterator method, of class RefLinkedList.
     */
    @Test
    public void testDescendingIterator() {
        int listSize = 5;
        RefLinkedList<Integer> list = createTestList(listSize);

        Iterator<Integer> itr = list.descendingIterator();
        for (int i = listSize - 1; i >= 0; i--) {
            assertTrue(itr.hasNext());
            assertEquals(i, itr.next().intValue());
        }
        assertFalse(itr.hasNext());
    }

    /**
     * Test of descendingIterator method, of class RefLinkedList.
     */
    @Test
    public void testDescendingIteratorRemove() {
        RefLinkedList<Integer> list = createTestList(5);

        Iterator<Integer> itr = list.descendingIterator();
        assertEquals(4, itr.next().intValue());
        itr.remove();

        checkListContent(list, 0, 1, 2, 3);

        assertEquals(3, itr.next().intValue());
        assertEquals(2, itr.next().intValue());
        itr.remove();

        checkListContent(list, 0, 1, 3);

        assertEquals(1, itr.next().intValue());
        assertEquals(0, itr.next().intValue());
        itr.remove();

        checkListContent(list, 1, 3);

        itr = list.descendingIterator();
        assertEquals(3, itr.next().intValue());
        itr.remove();
        assertEquals(1, itr.next().intValue());
        itr.remove();

        assertTrue(list.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void testIndexOfRemovedReference() {
        ElementRef<Integer> ref = createTestList(0).addGetReference(0);
        ref.remove();
        ref.getIndex();
    }

    @Test
    public void testCopyConstructor() {
        RefLinkedList<Integer> list1 = new RefLinkedList<>(Arrays.asList(0, 1, 2, 3, 4));
        checkListContent(list1, 0, 1, 2, 3, 4);

        RefLinkedList<Integer> list2 = new RefLinkedList<>(Collections.<Integer>emptyList());
        assertTrue(list2.isEmpty());
    }

    private static void verifyRef(ElementRef<Integer> ref, int index, int value) {
        assertEquals(index, ref.getIndex());
        assertEquals(value, ref.getElement().intValue());
    }

    @Test
    public void testElementRefNavigate() {
        RefLinkedList<Integer> list = createTestListWithContent(10, 11, 12, 13, 14);
        ElementRef<Integer> ref = list.getFirstReference();

        verifyRef(ref, 0, 10);
        ref = ref.getNext(2);
        verifyRef(ref, 2, 12);

        assertSame(ref, ref.getNext(0));
        assertSame(ref, ref.getPrevious(0));

        ref = ref.getPrevious(1);
        verifyRef(ref, 1, 11);

        ref = ref.getNext(1);
        verifyRef(ref, 2, 12);
        ref = ref.getNext(2);
        verifyRef(ref, 4, 14);

        assertNull(ref.getNext(1));
        assertNull(ref.getNext(100));
        assertNull(ref.getPrevious(5));

        ref = ref.getPrevious(4);

        assertNull(ref.getPrevious(1));
        assertNull(ref.getPrevious(100));
        assertNull(ref.getNext(5));

        verifyRef(ref, 0, 10);
    }

    @Test
    public void testElementRefShuffle() {
        RefLinkedList<Integer> list = createTestListWithContent(10, 11, 12, 13, 14);
        ElementRef<Integer> ref = list.getFirstReference();

        ref.moveLast();
        verifyRef(ref, 4, 10);
        checkListContent(list, 11, 12, 13, 14, 10);

        ref.moveFirst();
        verifyRef(ref, 0, 10);
        checkListContent(list, 10, 11, 12, 13, 14);

        assertEquals(1, ref.moveForward(1));
        verifyRef(ref, 1, 10);
        checkListContent(list, 11, 10, 12, 13, 14);

        assertEquals(2, ref.moveForward(2));
        verifyRef(ref, 3, 10);
        checkListContent(list, 11, 12, 13, 10, 14);

        assertEquals(1, ref.moveForward(100));
        verifyRef(ref, 4, 10);
        checkListContent(list, 11, 12, 13, 14, 10);

        ref.moveFirst();
        verifyRef(ref, 0, 10);
        checkListContent(list, 10, 11, 12, 13, 14);

        assertEquals(0, ref.moveForward(0));
        verifyRef(ref, 0, 10);
        checkListContent(list, 10, 11, 12, 13, 14);

        // Now do the same thing with moveBackward

        ref = list.getLastReference();
        verifyRef(ref, 4, 14);

        ref.moveFirst();
        verifyRef(ref, 0, 14);
        checkListContent(list, 14, 10, 11, 12, 13);

        ref.moveLast();
        verifyRef(ref, 4, 14);
        checkListContent(list, 10, 11, 12, 13, 14);

        assertEquals(1, ref.moveBackward(1));
        verifyRef(ref, 3, 14);
        checkListContent(list, 10, 11, 12, 14, 13);

        assertEquals(2, ref.moveBackward(2));
        verifyRef(ref, 1, 14);
        checkListContent(list, 10, 14, 11, 12, 13);

        assertEquals(1, ref.moveBackward(100));
        verifyRef(ref, 0, 14);
        checkListContent(list, 14, 10, 11, 12, 13);

        ref.moveLast();
        verifyRef(ref, 4, 14);
        checkListContent(list, 10, 11, 12, 13, 14);

        assertEquals(0, ref.moveBackward(0));
        verifyRef(ref, 4, 14);
        checkListContent(list, 10, 11, 12, 13, 14);
    }

    @Test
    public void testElementRefAdd() {
        RefLinkedList<Integer> list = createTestListWithContent(12);
        ElementRef<Integer> ref = list.getFirstReference();

        verifyRef(ref.addBefore(10), 0, 10);
        verifyRef(ref, 1, 12);
        checkListContent(list, 10, 12);

        verifyRef(ref.addBefore(11), 1, 11);
        verifyRef(ref, 2, 12);
        checkListContent(list, 10, 11, 12);

        verifyRef(ref.addAfter(14), 3, 14);
        verifyRef(ref, 2, 12);
        checkListContent(list, 10, 11, 12, 14);

        verifyRef(ref.addAfter(13), 3, 13);
        verifyRef(ref, 2, 12);
        checkListContent(list, 10, 11, 12, 13, 14);
    }

    @Test
    public void testElementRefEdit() {
        RefLinkedList<Integer> list = createTestListWithContent(10, 11, 12, 13, 14);
        ElementRef<Integer> ref = list.getFirstReference();

        assertFalse(ref.isRemoved());
        ref.setElement(20);
        verifyRef(ref, 0, 20);
        checkListContent(list, 20, 11, 12, 13, 14);
        assertFalse(ref.isRemoved());

        ref = ref.getNext(2);
        assertFalse(ref.isRemoved());
        verifyRef(ref, 2, 12);
        ref.setElement(22);
        assertFalse(ref.isRemoved());
        verifyRef(ref, 2, 22);
        checkListContent(list, 20, 11, 22, 13, 14);

        ref = ref.getNext(2);
        assertFalse(ref.isRemoved());
        verifyRef(ref, 4, 14);
        ref.setElement(24);
        verifyRef(ref, 4, 24);
        checkListContent(list, 20, 11, 22, 13, 24);
        assertFalse(ref.isRemoved());

        ref.remove();
        assertTrue(ref.isRemoved());
        checkListContent(list, 20, 11, 22, 13);

        ref = list.getReference(2);
        assertFalse(ref.isRemoved());
        verifyRef(ref, 2, 22);
        ref.remove();
        assertTrue(ref.isRemoved());
        checkListContent(list, 20, 11, 13);

        ref = list.getFirstReference();
        assertFalse(ref.isRemoved());
        verifyRef(ref, 0, 20);
        ref.remove();
        assertTrue(ref.isRemoved());
        checkListContent(list, 11, 13);

        list.getFirstReference().remove();
        list.getFirstReference().remove();
        assertTrue(list.isEmpty());

        assertNull(ref.getPrevious(0));
        assertNull(ref.getPrevious(1));
        assertNull(ref.getPrevious(100));

        assertNull(ref.getNext(0));
        assertNull(ref.getNext(1));
        assertNull(ref.getNext(100));
    }

    @Test
    public void testElementRefIterator() {
        int listSize = 5;
        RefLinkedList<Integer> list = createTestList(listSize);

        ListIterator<Integer> itr = list.getFirstReference().getIterator();
        for (int i = 0; i < listSize; i++) {
            assertTrue(itr.hasNext());
            assertEquals(i, itr.next().intValue());
        }
        assertFalse(itr.hasNext());

        itr = list.getReference(2).getIterator();
        for (int i = 2; i < listSize; i++) {
            assertTrue(itr.hasNext());
            assertEquals(i, itr.next().intValue());
        }
        assertFalse(itr.hasNext());
    }

    @Test(expected = IllegalStateException.class)
    public void testElementRefIllegalMoveFirst() {
        ElementRef<Integer> ref = createTestList(1).getFirstReference();
        ref.remove();
        ref.moveFirst();
    }

    @Test(expected = IllegalStateException.class)
    public void testElementRefIllegalMoveLast() {
        ElementRef<Integer> ref = createTestList(1).getFirstReference();
        ref.remove();
        ref.moveLast();
    }

    @Test(expected = IllegalStateException.class)
    public void testElementRefIllegalMoveForward() {
        ElementRef<Integer> ref = createTestList(1).getFirstReference();
        ref.remove();
        ref.moveForward(0);
    }

    @Test(expected = IllegalStateException.class)
    public void testElementRefIllegalMoveBackward() {
        ElementRef<Integer> ref = createTestList(1).getFirstReference();
        ref.remove();
        ref.moveBackward(0);
    }

    @Test(expected = IllegalStateException.class)
    public void testElementRefIllegalAddBefore() {
        ElementRef<Integer> ref = createTestList(1).getFirstReference();
        ref.remove();
        ref.addBefore(100);
    }

    @Test(expected = IllegalStateException.class)
    public void testElementRefIllegalAddAfter() {
        ElementRef<Integer> ref = createTestList(1).getFirstReference();
        ref.remove();
        ref.addAfter(100);
    }

    @Test(expected = IllegalStateException.class)
    public void testElementRefIllegalIterator() {
        ElementRef<Integer> ref = createTestList(1).getFirstReference();
        ref.remove();
        ref.getIterator();
    }
}
