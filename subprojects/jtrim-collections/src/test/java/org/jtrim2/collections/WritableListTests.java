package org.jtrim2.collections;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import org.jtrim2.testutils.JTrimTests;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public abstract class WritableListTests extends JTrimTests<TestListFactory<?>> {
    public WritableListTests(Collection<? extends TestListFactory<?>> factories) {
        super(factories);
    }

    @Test
    public void testRemoveObject() throws Exception {
        testAll(WritableListTests::testRemoveObject);
    }

    public static <ListType extends List<Integer>> void testRemoveObject(TestListFactory<ListType> factory) {
        int listSize = 5;
        ListType list = factory.createListOfSize(listSize);

        assertTrue(list.remove(Integer.valueOf(3)));
        factory.checkListContent(list, 0, 1, 2, 4);

        assertFalse(list.remove(Integer.valueOf(3)));
        factory.checkListContent(list, 0, 1, 2, 4);

        assertTrue(list.remove(Integer.valueOf(4)));
        factory.checkListContent(list, 0, 1, 2);

        assertTrue(list.remove(Integer.valueOf(0)));
        factory.checkListContent(list, 1, 2);

        assertTrue(list.remove(Integer.valueOf(1)));
        factory.checkListContent(list, 2);

        assertTrue(list.remove(Integer.valueOf(2)));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testClear() throws Exception {
        testAll(WritableListTests::testClear);
    }

    public static <ListType extends List<Integer>> void testClear(TestListFactory<ListType> factory) {
        List<Integer> list = factory.createListOfSize(5);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSetAtIndex() throws Exception {
        testAll(WritableListTests::testSetAtIndex);
    }

    public static <ListType extends List<Integer>> void testSetAtIndex(TestListFactory<ListType> factory) {
        ListType list = factory.createListOfSize(5);

        list.set(3, 13);
        factory.checkListContent(list, 0, 1, 2, 13, 4);

        list.set(4, 14);
        factory.checkListContent(list, 0, 1, 2, 13, 14);

        list.set(0, 10);
        factory.checkListContent(list, 10, 1, 2, 13, 14);
    }

    @Test
    public void testAddAtIndex() throws Exception {
        testAll(WritableListTests::testAddAtIndex);
    }

    public static <ListType extends List<Integer>> void testAddAtIndex(TestListFactory<ListType> factory) {
        ListType list = factory.createListOfSize(5);

        list.add(3, 13);
        factory.checkListContent(list, 0, 1, 2, 13, 3, 4);

        list.add(6, 16);
        factory.checkListContent(list, 0, 1, 2, 13, 3, 4, 16);

        list.add(0, 10);
        factory.checkListContent(list, 10, 0, 1, 2, 13, 3, 4, 16);
    }

    @Test
    public void testRemoveAtIndex() throws Exception {
        testAll(WritableListTests::testRemoveAtIndex);
    }

    public static <ListType extends List<Integer>> void testRemoveAtIndex(TestListFactory<ListType> factory) {
        ListType list = factory.createListOfSize(5);

        list.remove(3);
        factory.checkListContent(list, 0, 1, 2, 4);

        list.remove(3);
        factory.checkListContent(list, 0, 1, 2);

        list.remove(0);
        factory.checkListContent(list, 1, 2);

        list.remove(0);
        list.remove(0);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testListIteratorEarlyRemove() throws Exception {
        testAll(WritableListTests::testListIteratorEarlyRemove);
    }

    public static <ListType extends List<Integer>> void testListIteratorEarlyRemove(
            TestListFactory<ListType> factory) {
        List<Integer> list = factory.createListOfSize(2);
        ListIterator<Integer> itr = list.listIterator();
        TestUtils.expectError(IllegalStateException.class, () -> itr.remove());
    }

    @Test
    public void testListIteratorTwoRemove() throws Exception {
        testAll(WritableListTests::testListIteratorTwoRemove);
    }

    public static <ListType extends List<Integer>> void testListIteratorTwoRemove(
            TestListFactory<ListType> factory) {
        List<Integer> list = factory.createListOfSize(2);
        ListIterator<Integer> itr = list.listIterator();
        try {
            itr.next();
            itr.remove();
        } catch (IllegalStateException ex) {
            throw new RuntimeException(ex);
        }

        TestUtils.expectError(IllegalStateException.class, () -> itr.remove());
    }

    @Test
    public void testListIteratorRemoveAfterAdd() throws Exception {
        testAll(WritableListTests::testListIteratorRemoveAfterAdd);
    }

    public static <ListType extends List<Integer>> void testListIteratorRemoveAfterAdd(
            TestListFactory<ListType> factory) {
        List<Integer> list = factory.createListOfSize(2);
        ListIterator<Integer> itr = list.listIterator();
        try {
            itr.next();
            itr.add(100);
        } catch (IllegalStateException ex) {
            throw new RuntimeException(ex);
        }

        TestUtils.expectError(IllegalStateException.class, () -> itr.remove());
    }

    @Test
    public void testListIteratorSetWithoutNext() throws Exception {
        testAll(WritableListTests::testListIteratorSetWithoutNext);
    }

    public static <ListType extends List<Integer>> void testListIteratorSetWithoutNext(
            TestListFactory<ListType> factory) {
        List<Integer> list = factory.createListOfSize(2);
        ListIterator<Integer> itr = list.listIterator();

        TestUtils.expectError(IllegalStateException.class, () -> itr.set(0));
    }

    private static void checkPreviousIndex(ListIterator<?> itr, int expected) {
        assertEquals(expected, itr.previousIndex());
        assertEquals(expected + 1, itr.nextIndex());
    }

    private static void checkNextIndex(ListIterator<?> itr, int expected) {
        assertEquals(expected - 1, itr.previousIndex());
        assertEquals(expected, itr.nextIndex());
    }

    @Test
    public void testListIteratorEdit() throws Exception {
        testAll(WritableListTests::testListIteratorEdit);
    }

    public static <ListType extends List<Integer>> void testListIteratorEdit(TestListFactory<ListType> factory) {
        ListType list = factory.createListOfSize(5);

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

        checkPreviousIndex(itr, 4);
        assertEquals(4, itr.previous().intValue());
        checkPreviousIndex(itr, 3);
        assertEquals(3, itr.previous().intValue());
        itr.set(13);
        factory.checkListContent(list, 0, 1, 2, 13, 4);

        checkNextIndex(itr, 3);
        assertEquals(13, itr.next().intValue());
        checkNextIndex(itr, 4);
        assertEquals(4, itr.next().intValue());
        itr.set(14);
        factory.checkListContent(list, 0, 1, 2, 13, 14);

        checkPreviousIndex(itr, 4);
        assertEquals(14, itr.previous().intValue());
        checkPreviousIndex(itr, 3);
        assertEquals(13, itr.previous().intValue());
        itr.add(99);
        checkNextIndex(itr, 4);
        assertEquals(13, itr.next().intValue());
        checkPreviousIndex(itr, 4);
        assertEquals(13, itr.previous().intValue());

        factory.checkListContent(list, 0, 1, 2, 99, 13, 14);

        itr.remove();
        factory.checkListContent(list, 0, 1, 2, 99, 14);

        checkPreviousIndex(itr, 3);
        assertEquals(99, itr.previous().intValue());
        checkPreviousIndex(itr, 2);
        assertEquals(2, itr.previous().intValue());
        checkNextIndex(itr, 2);
        assertEquals(2, itr.next().intValue());
        itr.remove();

        factory.checkListContent(list, 0, 1, 99, 14);

        checkPreviousIndex(itr, 1);
        assertEquals(1, itr.previous().intValue());
        checkPreviousIndex(itr, 0);
        assertEquals(0, itr.previous().intValue());
        assertFalse(itr.hasPrevious());

        checkNextIndex(itr, 0);
        assertEquals(0, itr.next().intValue());
        itr.remove();
        checkNextIndex(itr, 0);
        assertEquals(1, itr.next().intValue());
        itr.remove();
        checkNextIndex(itr, 0);
        assertEquals(99, itr.next().intValue());
        itr.remove();
        checkNextIndex(itr, 0);
        assertEquals(14, itr.next().intValue());
        itr.remove();
        assertTrue(list.isEmpty());
    }
}
