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

    public static <ListType extends List<Integer>> void testSize(ListFactory<ListType> factory) {
        assertEquals(0, factory.createListOfSize(0).size());
        assertEquals(5, factory.createListOfSize(5).size());
    }

    public static <ListType extends List<Integer>> void testIsEmpty(ListFactory<ListType> factory) {
        assertTrue(factory.createListOfSize(0).isEmpty());
        assertFalse(factory.createListOfSize(1).isEmpty());
        assertFalse(factory.createListOfSize(5).isEmpty());
    }

    public static <ListType extends List<Integer>> void testContains(ListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = factory.createListOfSize(listSize);
        for (int i = 0; i < 5; i++) {
            assertTrue("Must contain: " + i, list.contains(i));
        }
        assertFalse(list.contains(-1));
    }

    public static <ListType extends List<Integer>> void testIterator(ListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = factory.createListOfSize(listSize);
        Iterator<Integer> itr = list.iterator();

        for (int i = 0; i < listSize; i++) {
            assertTrue(itr.hasNext());
            assertEquals(i, itr.next().intValue());
        }

        assertFalse(itr.hasNext());
    }

    public static <ListType extends List<Integer>> void testAddAndGetAtIndex(ListFactory<ListType> factory) {
        int listSize = 5;
        List<Integer> list = factory.createListOfSize(listSize);
        for (int i = 0; i < listSize; i++) {
            assertEquals(i, list.get(i).intValue());
        }
    }

    public static <ListType extends List<Integer>> void testRemoveObject(ListFactory<ListType> factory) {
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

    public static <ListType extends List<Integer>> void testClear(ListFactory<ListType> factory) {
        List<Integer> list = factory.createListOfSize(5);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    public static <ListType extends List<Integer>> void testSetAtIndex(ListFactory<ListType> factory) {
        ListType list = factory.createListOfSize(5);

        list.set(3, 13);
        factory.checkListContent(list, 0, 1, 2, 13, 4);

        list.set(4, 14);
        factory.checkListContent(list, 0, 1, 2, 13, 14);

        list.set(0, 10);
        factory.checkListContent(list, 10, 1, 2, 13, 14);
    }

    public static <ListType extends List<Integer>> void testAddAtIndex(ListFactory<ListType> factory) {
        ListType list = factory.createListOfSize(5);

        list.add(3, 13);
        factory.checkListContent(list, 0, 1, 2, 13, 3, 4);

        list.add(6, 16);
        factory.checkListContent(list, 0, 1, 2, 13, 3, 4, 16);

        list.add(0, 10);
        factory.checkListContent(list, 10, 0, 1, 2, 13, 3, 4, 16);
    }

    public static <ListType extends List<Integer>> void testRemoveAtIndex(ListFactory<ListType> factory) {
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

    //@Test(expected = NoSuchElementException.class)
    public static <ListType extends List<Integer>> void testListIteratorTooManyNext(ListFactory<ListType> factory) {
        List<Integer> list = factory.createListOfSize(2);
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
        List<Integer> list = factory.createListOfSize(2);
        ListIterator<Integer> itr = list.listIterator();
        itr.previous();
    }

    //@Test(expected = IllegalStateException.class)
    public static <ListType extends List<Integer>> void testListIteratorEarlyRemove(ListFactory<ListType> factory) {
        List<Integer> list = factory.createListOfSize(2);
        ListIterator<Integer> itr = list.listIterator();
        itr.remove();
    }

    //@Test(expected = IllegalStateException.class)
    public static <ListType extends List<Integer>> void testListIteratorTwoRemove(ListFactory<ListType> factory) {
        List<Integer> list = factory.createListOfSize(2);
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
        List<Integer> list = factory.createListOfSize(2);
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
        List<Integer> list = factory.createListOfSize(2);
        ListIterator<Integer> itr = list.listIterator();
        itr.set(0);
    }

    public static <ListType extends List<Integer>> void testListIteratorRead(ListFactory<ListType> factory) {
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

    public static <ListType extends List<Integer>> void testListIteratorEdit(ListFactory<ListType> factory) {
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

    public static <ListType extends List<Integer>> void testListIteratorFromIndex0(ListFactory<ListType> factory) {
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

    public static <ListType extends List<Integer>> void testListIteratorFromEnd(ListFactory<ListType> factory) {
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

    public static interface ListFactory<ListType extends List<Integer>> {
        /**
         * Creates a list of the specified size where each element is its index.
         *
         * @param size the size of the list to be returned. This argument must
         *   be greater than or equal to zero.
         * @return the list of the specified size where each element is its
         *   index. This method never returns {@code null}.
         */
        public ListType createListOfSize(int size);
        public ListType createList(int... content);

        public void checkListContent(ListType list, int... content);
    }

    private ListTestMethods() {
        throw new AssertionError();
    }
}
