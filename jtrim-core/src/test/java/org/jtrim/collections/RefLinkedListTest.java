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

    private static void execute(String methodName) throws Throwable {
        ListTestMethods.executeTest(methodName, LinkedListFactory.INSTANCE);
    }

    private static RefLinkedList<Integer> createTestList(int size) {
        RefLinkedList<Integer> result = new RefLinkedList<>();
        for (int i = 0; i < size; i++) {
            result.add(i);
        }
        return result;
    }

    private static RefLinkedList<Integer> createTestListWithContent(Integer... content) {
        return new RefLinkedList<>(Arrays.asList(content));
    }

    private static void checkListContent(RefList<Integer> list, Integer... content) {
        assertEquals(content.length, list.size());

        // Check from both side to detect failures in the links in both ways.
        ElementRef<Integer> ref = list.getFirstReference();
        for (int i = 0; i < content.length; i++) {
            Integer current = ref.getElement();
            assertEquals(content[i], current);

            ref = ref.getNext(1);
        }
        assertNull(ref);

        ref = list.getLastReference();
        for (int i = content.length - 1; i >= 0; i--) {
            Integer current = ref.getElement();
            assertEquals(content[i], current);

            ref = ref.getPrevious(1);
        }
        assertNull(ref);
    }

    @Test
    public void testSize() throws Throwable {
        execute("testSize");
    }

    @Test
    public void testIsEmpty() throws Throwable {
        execute("testIsEmpty");
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

    @Test
    public void testContains() throws Throwable {
        execute("testContains");
    }

    @Test
    public void testIterator() throws Throwable {
        execute("testIterator");
    }

    @Test
    public void testAddAndGetAtIndex() throws Throwable {
        execute("testAddAndGetAtIndex");
    }

    @Test
    public void testRemoveObject() throws Throwable {
        execute("testRemoveObject");
    }

    @Test
    public void testClear() throws Throwable {
        execute("testClear");
    }

    @Test
    public void testSetAtIndex() throws Throwable {
        execute("testSetAtIndex");
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

    @Test
    public void testAddAtIndex() throws Throwable {
        execute("testAddAtIndex");
    }

    @Test
    public void testRemoveAtIndex() throws Throwable {
        execute("testRemoveAtIndex");
    }

    @Test(expected = NoSuchElementException.class)
    public void testListIteratorTooManyNext() throws Throwable {
        execute("testListIteratorTooManyNext");
    }

    @Test(expected = NoSuchElementException.class)
    public void testListIteratorTooManyPrevious() throws Throwable {
        execute("testListIteratorTooManyPrevious");
    }

    @Test(expected = IllegalStateException.class)
    public void testListIteratorEarlyRemove() throws Throwable {
        execute("testListIteratorEarlyRemove");
    }

    @Test(expected = IllegalStateException.class)
    public void testListIteratorTwoRemove() throws Throwable {
        execute("testListIteratorTwoRemove");
    }

    @Test(expected = IllegalStateException.class)
    public void testListIteratorRemoveAfterAdd() throws Throwable {
        execute("testListIteratorRemoveAfterAdd");
    }

    @Test(expected = IllegalStateException.class)
    public void testListIteratorSetWithoutNext() throws Throwable {
        execute("testListIteratorSetWithoutNext");
    }

    @Test
    public void testListIteratorRead() throws Throwable {
        execute("testListIteratorRead");
    }

    @Test
    public void testListIteratorEdit() throws Throwable {
        execute("testListIteratorEdit");
    }

    @Test
    public void testListIteratorFromIndex() throws Throwable {
        execute("testListIteratorFromIndex");
    }

    @Test
    public void testListIteratorFromIndex0() throws Throwable {
        execute("testListIteratorFromIndex0");
    }

    @Test
    public void testListIteratorFromEnd() throws Throwable {
        execute("testListIteratorFromEnd");
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

    private enum LinkedListFactory implements ListTestMethods.ListFactory<RefList<Integer>> {
        INSTANCE;

        @Override
        public RefList<Integer> createListOfSize(int size) {
            Integer[] array = new Integer[size];
            for (int i = 0; i < array.length; i++) {
                array[i] = i;
            }
            return createList(array);
        }

        @Override
        public RefList<Integer> createList(Integer... content) {
            return createTestListWithContent(content);
        }

        @Override
        public void checkListContent(RefList<Integer> list, Integer... content) {
            RefLinkedListTest.checkListContent(list, content);
        }
    }
}
