package org.jtrim.collections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public final class ListTestMethods {
    public static void executeTest(String methodName, ListFactory<?> factory) throws Throwable {
        try {
            Method method = ListTestMethods.class.getMethod(methodName, ListFactory.class);
            for (int prefixSize = 0; prefixSize < 2; prefixSize++) {
                for (int suffixSize = 0; suffixSize < 2; suffixSize++) {
                    method.invoke(null, factory);
                }
            }
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private static <ListType extends List<Integer>> ListType createListOfSize(ListFactory<ListType> factory, int size) {
        Integer[] array = new Integer[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
        return factory.createList(array);
    }

    public static <ListType extends List<Integer>> void testSize(ListFactory<ListType> factory) {
        assertEquals(0, createListOfSize(factory, 0).size());
        assertEquals(5, createListOfSize(factory, 5).size());
    }

    public static <ListType extends List<Integer>> void testIsEmpty(ListFactory<ListType> factory) {
        assertTrue(createListOfSize(factory, 0).isEmpty());
        assertFalse(createListOfSize(factory, 1).isEmpty());
        assertFalse(createListOfSize(factory, 5).isEmpty());
    }

    public static <ListType extends List<Integer>> void testContains(ListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = createListOfSize(factory, listSize);
        for (int i = 0; i < 5; i++) {
            assertTrue("Must contain: " + i, list.contains(i));
        }
        assertFalse(list.contains(-1));
    }

    public static <ListType extends List<Integer>> void testIterator(ListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = createListOfSize(factory, listSize);
        Iterator<Integer> itr = list.iterator();

        for (int i = 0; i < listSize; i++) {
            assertTrue(itr.hasNext());
            assertEquals(i, itr.next().intValue());
        }

        assertFalse(itr.hasNext());
    }

    public static <ListType extends List<Integer>> void testAddAndGetAtIndex(ListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = createListOfSize(factory, listSize);
        for (int i = 0; i < listSize; i++) {
            assertEquals(i, list.get(i).intValue());
        }
    }

    public static <ListType extends List<Integer>> void testRemoveObject(ListFactory<ListType> factory) {
        int listSize = 5;
        ListType list = createListOfSize(factory, listSize);

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

    public static <ListType extends List<Integer>> void testClear(ListFactory<ListType> factory) {
        List<Integer> list = createListOfSize(factory, 5);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    public static <ListType extends List<Integer>> void testSetAtIndex(ListFactory<ListType> factory) {
        ListType list = createListOfSize(factory, 5);

        list.set(3, 13);
        factory.checkListContent(list, 0, 1, 2, 13, 4);

        list.set(4, 14);
        factory.checkListContent(list, 0, 1, 2, 13, 14);

        list.set(0, 10);
        factory.checkListContent(list, 10, 1, 2, 13, 14);
    }

    public static <ListType extends List<Integer>> void testAddAtIndex(ListFactory<ListType> factory) {
        ListType list = createListOfSize(factory, 5);

        list.add(3, 13);
        factory.checkListContent(list, 0, 1, 2, 13, 3, 4);

        list.add(6, 16);
        factory.checkListContent(list, 0, 1, 2, 13, 3, 4, 16);

        list.add(0, 10);
        factory.checkListContent(list, 10, 0, 1, 2, 13, 3, 4, 16);
    }

    public static <ListType extends List<Integer>> void testRemoveAtIndex(ListFactory<ListType> factory) {
        ListType list = createListOfSize(factory, 5);

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

    //@Test(expected = NoSuchElementException.class)
    public static <ListType extends List<Integer>> void testListIteratorTooManyNext(ListFactory<ListType> factory) {
        List<Integer> list = createListOfSize(factory, 2);
        ListIterator<Integer> itr = list.listIterator();
        try {
            itr.next();
            itr.next();
        } catch (NoSuchElementException ex) {
            throw new RuntimeException(ex);
        }

        itr.next();
    }

    //@Test(expected = NoSuchElementException.class)
    public static <ListType extends List<Integer>> void testListIteratorTooManyPrevious(ListFactory<ListType> factory) {
        List<Integer> list = createListOfSize(factory, 2);
        ListIterator<Integer> itr = list.listIterator();
        itr.previous();
    }

    //@Test(expected = IllegalStateException.class)
    public static <ListType extends List<Integer>> void testListIteratorEarlyRemove(ListFactory<ListType> factory) {
        List<Integer> list = createListOfSize(factory, 2);
        ListIterator<Integer> itr = list.listIterator();
        itr.remove();
    }

    //@Test(expected = IllegalStateException.class)
    public static <ListType extends List<Integer>> void testListIteratorTwoRemove(ListFactory<ListType> factory) {
        List<Integer> list = createListOfSize(factory, 2);
        ListIterator<Integer> itr = list.listIterator();
        try {
            itr.next();
            itr.remove();
        } catch (IllegalStateException ex) {
            throw new RuntimeException(ex);
        }

        itr.remove();
    }

    //@Test(expected = IllegalStateException.class)
    public static <ListType extends List<Integer>> void testListIteratorRemoveAfterAdd(ListFactory<ListType> factory) {
        List<Integer> list = createListOfSize(factory, 2);
        ListIterator<Integer> itr = list.listIterator();
        try {
            itr.next();
            itr.add(100);
        } catch (IllegalStateException ex) {
            throw new RuntimeException(ex);
        }

        itr.remove();
    }

    //@Test(expected = IllegalStateException.class)
    public static <ListType extends List<Integer>> void testListIteratorSetWithoutNext(ListFactory<ListType> factory) {
        List<Integer> list = createListOfSize(factory, 2);
        ListIterator<Integer> itr = list.listIterator();
        itr.set(0);
    }

    public static <ListType extends List<Integer>> void testListIteratorRead(ListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = createListOfSize(factory, listSize);

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

    public static <ListType extends List<Integer>> void testListIteratorEdit(ListFactory<ListType> factory) {
        ListType list = createListOfSize(factory, 5);

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
        factory.checkListContent(list, 0, 1, 2, 13, 4);

        assertEquals(13, itr.next().intValue());
        assertEquals(4, itr.next().intValue());
        itr.set(14);
        factory.checkListContent(list, 0, 1, 2, 13, 14);

        assertEquals(14, itr.previous().intValue());
        assertEquals(13, itr.previous().intValue());
        itr.add(99);
        assertEquals(13, itr.next().intValue());
        assertEquals(13, itr.previous().intValue());

        factory.checkListContent(list, 0, 1, 2, 99, 13, 14);

        itr.remove();
        factory.checkListContent(list, 0, 1, 2, 99, 14);

        assertEquals(99, itr.previous().intValue());
        assertEquals(2, itr.previous().intValue());
        assertEquals(2, itr.next().intValue());
        itr.remove();

        factory.checkListContent(list, 0, 1, 99, 14);

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

    public static <ListType extends List<Integer>> void testListIteratorFromIndex(ListFactory<ListType> factory) {
        int listSize = 5;
        int startIndex = 2;
        List<Integer> list = createListOfSize(factory, listSize);

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

    public static <ListType extends List<Integer>> void testListIteratorFromIndex0(ListFactory<ListType> factory) {
        int listSize = 5;
        int startIndex = 0;
        List<Integer> list = createListOfSize(factory, listSize);

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

    public static <ListType extends List<Integer>> void testListIteratorFromEnd(ListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = createListOfSize(factory, listSize);

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

    public static <ListType extends List<Integer>> void testIndexOf(ListFactory<ListType> factory) {
        int size = 5;
        ListType list = createListOfSize(factory, size);
        ListType doubleList = factory.createList(createDoubleArray(size));

        assertEquals(-1, factory.createList().indexOf(-1));
        assertEquals(-1, list.indexOf(-1));
        assertEquals(-1, list.indexOf(-2));

        for (int i = 0; i < size; i++) {
            assertEquals(i, list.indexOf(i));
            assertEquals(i, doubleList.indexOf(i));
        }
    }

    public static <ListType extends List<Integer>> void testIndexOfNulls(ListFactory<ListType> factory) {
        assertEquals(-1, factory.createList().indexOf(null));
        assertEquals(-1, factory.createList(1, 2, 3, 4, 5).indexOf(null));

        assertEquals(1, factory.createList(1, null, 2, 3).indexOf(null));
        assertEquals(0, factory.createList(null, 1, 2, 3).indexOf(null));
        assertEquals(3, factory.createList(1, 2, 3, null).indexOf(null));

        assertEquals(1, factory.createList(1, null, 2, 3, 1, null, 2, 3).indexOf(null));
        assertEquals(0, factory.createList(null, 1, 2, 3, null, 1, 2, 3).indexOf(null));
        assertEquals(3, factory.createList(1, 2, 3, null, 1, 2, 3, null).indexOf(null));
    }

    public static <ListType extends List<Integer>> void testLastIndexOf(ListFactory<ListType> factory) {
        int size = 5;
        ListType list = createListOfSize(factory, size);
        ListType doubleList = factory.createList(createDoubleArray(size));

        assertEquals(-1, factory.createList().lastIndexOf(-1));
        assertEquals(-1, list.lastIndexOf(-1));
        assertEquals(-1, list.lastIndexOf(-2));

        for (int i = 0; i < size; i++) {
            assertEquals(i, list.lastIndexOf(i));
            assertEquals(size + i, doubleList.lastIndexOf(i));
        }
    }

    public static <ListType extends List<Integer>> void testLastIndexOfNulls(ListFactory<ListType> factory) {
        assertEquals(-1, factory.createList().lastIndexOf(null));
        assertEquals(-1, factory.createList(1, 2, 3, 4, 5).lastIndexOf(null));

        assertEquals(1, factory.createList(1, null, 2, 3).lastIndexOf(null));
        assertEquals(0, factory.createList(null, 1, 2, 3).lastIndexOf(null));
        assertEquals(3, factory.createList(1, 2, 3, null).lastIndexOf(null));

        assertEquals(5, factory.createList(1, null, 2, 3, 1, null, 2, 3).lastIndexOf(null));
        assertEquals(4, factory.createList(null, 1, 2, 3, null, 1, 2, 3).lastIndexOf(null));
        assertEquals(7, factory.createList(1, 2, 3, null, 1, 2, 3, null).lastIndexOf(null));
    }

    public static <ListType extends List<Integer>> void testToArray(ListFactory<ListType> factory) {
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

    public static <ListType extends List<Integer>> void testToProvidedArray(ListFactory<ListType> factory) {
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

    public static interface ListFactory<ListType extends List<Integer>> {
        public ListType createList(Integer... content);

        public void checkListContent(ListType list, Integer... content);
    }

    private ListTestMethods() {
        throw new AssertionError();
    }
}
