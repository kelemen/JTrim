package org.jtrim2.collections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.jtrim2.testutils.JTrimTests;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public abstract class ReadableListTests extends JTrimTests<TestListFactory<?>> {
    public ReadableListTests(Collection<? extends TestListFactory<?>> factories) {
        super(factories);
    }

    @Test
    public void testAddAndGetAtIndex() throws Exception {
        testAll(ReadableListTests::testAddAndGetAtIndex);
    }

    public static <ListType extends List<Integer>> void testAddAndGetAtIndex(TestListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = factory.createListOfSize(listSize);
        for (int i = 0; i < listSize; i++) {
            assertEquals(i, list.get(i).intValue());
        }
    }

    @Test
    public void testListIteratorFromIndex() throws Exception {
        testAll(ReadableListTests::testListIteratorFromIndex);
    }

    public static <ListType extends List<Integer>> void testListIteratorFromIndex(TestListFactory<ListType> factory) {
        int listSize = 5;
        int startIndex = 2;
        List<Integer> list = factory.createListOfSize(listSize);

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

    @Test
    public void testListIteratorFromIndex0() throws Exception {
        testAll(ReadableListTests::testListIteratorFromIndex0);
    }

    public static <ListType extends List<Integer>> void testListIteratorFromIndex0(TestListFactory<ListType> factory) {
        int listSize = 5;
        int startIndex = 0;
        List<Integer> list = factory.createListOfSize(listSize);

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

    @Test
    public void testListIteratorFromEnd() throws Exception {
        testAll(ReadableListTests::testListIteratorFromEnd);
    }

    public static <ListType extends List<Integer>> void testListIteratorFromEnd(TestListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = factory.createListOfSize(listSize);

        ListIterator<Integer> itr = list.listIterator(listSize);
        assertFalse(itr.hasNext());

        for (int i = listSize - 1; i >= 0; i--) {
            assertTrue(itr.hasPrevious());
            assertEquals(i, itr.previous().intValue());
            assertTrue(itr.hasNext());
        }

        assertFalse(itr.hasPrevious());
    }

    private static Integer[] createDoubleArray(int simpleSize) {
        Integer[] doubleArray = new Integer[2 * simpleSize];
        for (int i = 0; i < simpleSize; i++) {
            doubleArray[i] = i;
            doubleArray[simpleSize + i] = i;
        }
        return doubleArray;
    }

    @Test
    public void testIndexOf() throws Exception {
        testAll(ReadableListTests::testIndexOf);
    }

    public static <ListType extends List<Integer>> void testIndexOf(TestListFactory<ListType> factory) {
        int size = 5;
        ListType list = factory.createListOfSize(size);
        ListType doubleList = factory.createList(createDoubleArray(size));

        assertEquals(-1, factory.createList().indexOf(-1));
        assertEquals(-1, list.indexOf(-1));
        assertEquals(-1, list.indexOf(-2));

        for (int i = 0; i < size; i++) {
            assertEquals(i, list.indexOf(i));
            assertEquals(i, doubleList.indexOf(i));
        }
    }

    @Test
    public void testIndexOfNulls() throws Exception {
        testAll(ReadableListTests::testIndexOfNulls);
    }

    public static <ListType extends List<Integer>> void testIndexOfNulls(TestListFactory<ListType> factory) {
        assertEquals(-1, factory.createList().indexOf(null));
        assertEquals(-1, factory.createList(1, 2, 3, 4, 5).indexOf(null));

        assertEquals(1, factory.createList(1, null, 2, 3).indexOf(null));
        assertEquals(0, factory.createList(null, 1, 2, 3).indexOf(null));
        assertEquals(3, factory.createList(1, 2, 3, null).indexOf(null));

        assertEquals(1, factory.createList(1, null, 2, 3, 1, null, 2, 3).indexOf(null));
        assertEquals(0, factory.createList(null, 1, 2, 3, null, 1, 2, 3).indexOf(null));
        assertEquals(3, factory.createList(1, 2, 3, null, 1, 2, 3, null).indexOf(null));
    }

    @Test
    public void testLastIndexOf() throws Exception {
        testAll(ReadableListTests::testLastIndexOf);
    }

    public static <ListType extends List<Integer>> void testLastIndexOf(TestListFactory<ListType> factory) {
        int size = 5;
        ListType list = factory.createListOfSize(size);
        ListType doubleList = factory.createList(createDoubleArray(size));

        assertEquals(-1, factory.createList().lastIndexOf(-1));
        assertEquals(-1, list.lastIndexOf(-1));
        assertEquals(-1, list.lastIndexOf(-2));

        for (int i = 0; i < size; i++) {
            assertEquals(i, list.lastIndexOf(i));
            assertEquals(size + i, doubleList.lastIndexOf(i));
        }
    }

    @Test
    public void testLastIndexOfNulls() throws Exception {
        testAll(ReadableListTests::testLastIndexOfNulls);
    }

    public static <ListType extends List<Integer>> void testLastIndexOfNulls(TestListFactory<ListType> factory) {
        assertEquals(-1, factory.createList().lastIndexOf(null));
        assertEquals(-1, factory.createList(1, 2, 3, 4, 5).lastIndexOf(null));

        assertEquals(1, factory.createList(1, null, 2, 3).lastIndexOf(null));
        assertEquals(0, factory.createList(null, 1, 2, 3).lastIndexOf(null));
        assertEquals(3, factory.createList(1, 2, 3, null).lastIndexOf(null));

        assertEquals(5, factory.createList(1, null, 2, 3, 1, null, 2, 3).lastIndexOf(null));
        assertEquals(4, factory.createList(null, 1, 2, 3, null, 1, 2, 3).lastIndexOf(null));
        assertEquals(7, factory.createList(1, 2, 3, null, 1, 2, 3, null).lastIndexOf(null));
    }

    @Test
    public void testToArray() throws Exception {
        testAll(ReadableListTests::testToArray);
    }

    public static <ListType extends List<Integer>> void testToArray(TestListFactory<ListType> factory) {
        Integer[] expected = new Integer[]{1, 2, 3, 4, 5};
        Object[] array = factory.createList(expected).toArray();
        assertTrue(array.getClass() == Object[].class);
        assertEquals(expected.length, array.length);

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], array[i]);
        }

        Object[] empty = factory.createList(new Integer[0]).toArray();
        assertTrue(empty.getClass() == Object[].class);
        assertEquals(0, empty.length);
    }

    @Test
    public void testToProvidedArray() throws Exception {
        testAll(ReadableListTests::testToProvidedArray);
    }

    public static <ListType extends List<Integer>> void testToProvidedArray(TestListFactory<ListType> factory) {
        Integer[] expected = new Integer[]{1, 2, 3, 4, 5};
        ListType list = factory.createList(expected);

        Integer[] array;

        array = list.toArray(new Integer[0]);
        assertArrayEquals(expected, array);

        array = list.toArray(new Integer[4]);
        assertArrayEquals(expected, array);

        Integer[] bestArray = new Integer[5];
        array = list.toArray(bestArray);
        assertSame(bestArray, array);
        assertArrayEquals(expected, array);

        Integer[] largeArray = new Integer[7];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = 99;
        }

        array = list.toArray(largeArray);
        assertSame(largeArray, array);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], array[i]);
        }
        assertNull(null, array[5]);
        assertEquals(Integer.valueOf(99), array[6]);
    }

    @Test
    public void testListIteratorRead() throws Exception {
        testAll(ReadableListTests::testListIteratorRead);
    }

    public static <ListType extends List<Integer>> void testListIteratorRead(TestListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = factory.createListOfSize(listSize);

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
        for (int i = listSize - 1; i >= 0; i--) {
            assertTrue(itr.hasPrevious());
            assertEquals(i, itr.previousIndex());
            assertEquals(i, itr.previous().intValue());
            assertEquals(i, itr.nextIndex());
            assertTrue(itr.hasNext());
        }
        assertFalse(itr.hasPrevious());
    }

    @Test
    public void testListIteratorTooManyNext() throws Exception {
        testAll(ReadableListTests::testListIteratorTooManyNext);
    }

    public static <ListType extends List<Integer>> void testListIteratorTooManyNext(
            TestListFactory<ListType> factory) {
        List<Integer> list = factory.createListOfSize(2);
        ListIterator<Integer> itr = list.listIterator();
        itr.next();
        itr.next();

        TestUtils.expectError(NoSuchElementException.class, () -> itr.next());
    }

    @Test
    public void testListIteratorTooManyPrevious() throws Exception {
        testAll(ReadableListTests::testListIteratorTooManyPrevious);
    }

    public static <ListType extends List<Integer>> void testListIteratorTooManyPrevious(
            TestListFactory<ListType> factory) {
        List<Integer> list = factory.createListOfSize(2);
        ListIterator<Integer> itr = list.listIterator();
        TestUtils.expectError(NoSuchElementException.class, () -> itr.previous());
    }

    private static <ListType extends List<Integer>> void testSerialize(
            TestListFactory<ListType> factory, int size) throws IOException, ClassNotFoundException {

        ListType list = factory.createListOfSize(size);
        byte[] serialized = SerializationHelper.serializeObject(list);
        List<?> deserialized = (List<?>) SerializationHelper.deserializeObject(serialized);

        List<?> expected = new ArrayList<>(list);
        List<?> actual = new ArrayList<>(deserialized);
        assertEquals(expected, actual);
    }

    @Test
    public void testSerialize() throws Exception {
        testAll(ReadableListTests::testSerialize);
    }

    public static <ListType extends List<Integer>> void testSerialize(
            TestListFactory<ListType> factory) throws IOException, ClassNotFoundException {
        if (factory.isSublistFactory()) {
            return;
        }

        for (int size = 0; size < 5; size++) {
            testSerialize(factory, size);
        }
        testSerialize(factory, 100);
    }

    @Test
    public void testSize() throws Exception {
        testAll(ReadableListTests::testSize);
    }

    public static <ListType extends List<Integer>> void testSize(TestListFactory<ListType> factory) {
        assertEquals(0, factory.createListOfSize(0).size());
        assertEquals(5, factory.createListOfSize(5).size());
    }

    @Test
    public void testIsEmpty() throws Exception {
        testAll(ReadableListTests::testIsEmpty);
    }

    public static <ListType extends List<Integer>> void testIsEmpty(TestListFactory<ListType> factory) {
        assertTrue(factory.createListOfSize(0).isEmpty());
        assertFalse(factory.createListOfSize(1).isEmpty());
        assertFalse(factory.createListOfSize(5).isEmpty());
    }

    @Test
    public void testContains() throws Exception {
        testAll(ReadableListTests::testContains);
    }

    public static <ListType extends List<Integer>> void testContains(TestListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = factory.createListOfSize(listSize);
        for (int i = 0; i < 5; i++) {
            assertTrue("Must contain: " + i, list.contains(i));
        }
        assertFalse(list.contains(-1));
    }

    @Test
    public void testIterator() throws Exception {
        testAll(ReadableListTests::testIterator);
    }

    public static <ListType extends List<Integer>> void testIterator(TestListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = factory.createListOfSize(listSize);
        Iterator<Integer> itr = list.iterator();

        for (int i = 0; i < listSize; i++) {
            assertTrue(itr.hasNext());
            assertEquals(i, itr.next().intValue());
        }

        assertFalse(itr.hasNext());
    }
}
