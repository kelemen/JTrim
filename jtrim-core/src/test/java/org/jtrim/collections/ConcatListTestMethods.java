package org.jtrim.collections;

import java.util.ArrayList;
import java.util.List;

import static org.jtrim.collections.CollectionsExTest.checkListContent;
import static org.junit.Assert.*;

/**
 * Contains test methods to test both RandomAccessConcatListView and
 * ConcatListView.
 *
 * @author Kelemen Attila
 */
public final class ConcatListTestMethods {
    public static interface ListFactory {
        public <E> List<E> concatView(List<? extends E> list1, List<? extends E> list2);
    }

    private static List<Integer> createArrayList(int... elements) {
        List<Integer> result = new ArrayList<>(elements.length);
        for (int i = 0; i < elements.length; i++) {
            result.add(elements[i]);
        }
        return result;
    }

    private static Integer[] createArray(List<Integer> list1, List<Integer> list2) {
        List<Integer> result = new ArrayList<>(list1.size() + list2.size());
        for (Integer value: list1) {
            result.add(value);
        }
        for (Integer value: list2) {
            result.add(value);
        }
        return result.toArray(new Integer[result.size()]);
    }

    public static void checkSimpleCreate(ListFactory factory) {
        List<Integer> list = factory.concatView(createArrayList(10, 11, 12, 13), createArrayList(14, 15, 16));
        assertEquals(7, list.size());
        assertFalse(list.isEmpty());
        CollectionsExTest.checkListContent(list, 10, 11, 12, 13, 14, 15, 16);
    }

    public static void checkIterator(ListFactory factory) {
        checkIteratorWithAdd(factory);
        checkIteratorWithRemove(factory);
    }

    private static void checkIteratorWithRemove(ListFactory factory) {
        List<Integer> list1 = createArrayList(10, 11, 12, 13, 14, 15);
        List<Integer> list2 = createArrayList(16, 17, 18, 19, 20);
        List<Integer> list = factory.concatView(list1, list2);

        checkListContent(list, createArray(list1, list2));
        list1.remove(2);
        checkListContent(list, createArray(list1, list2));
        list1.remove(list1.size() - 1);
        checkListContent(list, createArray(list1, list2));
        list1.remove(0);
        checkListContent(list, createArray(list1, list2));

        list2.remove(3);
        checkListContent(list, createArray(list1, list2));
        list2.remove(list2.size() - 1);
        checkListContent(list, createArray(list1, list2));
        list2.remove(0);
        checkListContent(list, createArray(list1, list2));

        list1.clear();
        list2.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        checkListContent(list);
    }

    private static void checkIteratorWithAdd(ListFactory factory) {
        List<Integer> list1 = createArrayList();
        List<Integer> list2 = createArrayList();
        List<Integer> list = factory.concatView(list1, list2);

        checkListContent(list, createArray(list1, list2));
        list1.add(11);
        checkListContent(list, createArray(list1, list2));
        list1.add(13);
        checkListContent(list, createArray(list1, list2));
        list1.add(0, 10);
        checkListContent(list, createArray(list1, list2));
        list1.add(2, 12);
        checkListContent(list, createArray(list1, list2));

        list2.add(15);
        checkListContent(list, createArray(list1, list2));
        list2.add(17);
        checkListContent(list, createArray(list1, list2));
        list2.add(0, 14);
        checkListContent(list, createArray(list1, list2));
        list2.add(2, 16);
        checkListContent(list, createArray(list1, list2));
    }

    public static void checkContains(ListFactory factory) {
        List<Integer> list1 = createArrayList(10, 11, 12, 13, 14, 15);
        List<Integer> list2 = createArrayList(16, 17, 18, 19, 20);
        List<Integer> list = factory.concatView(list1, list2);

        for (Integer element: list1) {
            assertTrue("Must contain element: " + element, list.contains(element));
        }
        for (Integer element: list2) {
            assertTrue("Must contain element: " + element, list.contains(element));
        }

        assertFalse(list.contains(100));

        assertTrue(list.containsAll(createArrayList(13, 15, 19)));
        assertTrue(list.containsAll(createArrayList(11, 15, 14)));
        assertTrue(list.containsAll(createArrayList(10)));
        assertTrue(list.containsAll(createArrayList(20)));
        assertTrue(list.containsAll(createArrayList(19, 17, 20)));

        assertFalse(list.containsAll(createArrayList(-1, 13, 15, 19)));
        assertFalse(list.containsAll(createArrayList(11, 15, -1, 14)));
        assertFalse(list.containsAll(createArrayList(10, -1)));
        assertFalse(list.containsAll(createArrayList(-1, 20)));
        assertFalse(list.containsAll(createArrayList(19, 17, 20, -1)));
    }

    public static void checkToArray(ListFactory factory) {
        List<Integer> list1 = createArrayList(10, 11, 12, 13, 14, 15);
        List<Integer> list2 = createArrayList(16, 17, 18, 19, 20);
        List<Integer> list = factory.concatView(list1, list2);
        Integer[] expectedArray = createArray(list1, list2);

        Integer[] array1 = list.toArray(new Integer[0]);
        assertArrayEquals(expectedArray, array1);

        Integer[] array2 = list.toArray(new Integer[10]);
        assertArrayEquals(expectedArray, array2);

        Integer[] array3 = list.toArray(new Integer[11]);
        assertArrayEquals(expectedArray, array3);

        Integer[] container4 = new Integer[50];
        Integer container4Element = Integer.MAX_VALUE;
        for (int i = 0; i < container4.length; i++) {
            container4[i] = container4Element;
        }

        Integer[] array4 = list.toArray(container4);
        for (int i = 0; i < expectedArray.length; i++) {
            assertEquals(expectedArray[i], array4[i]);
        }
        assertNull(array4[expectedArray.length]);
        for (int i = expectedArray.length + 1; i < container4.length; i++) {
            assertSame(container4[i], container4Element);
        }
    }

    public static void checkGet(ListFactory factory) {
        List<Integer> list1 = createArrayList(10, 11, 12, 13, 14, 15);
        List<Integer> list2 = createArrayList(16, 17, 18, 19, 20);
        List<Integer> list = factory.concatView(list1, list2);

        Integer[] content = createArray(list1, list2);
        for (int i = 0; i < content.length; i++) {
            assertEquals(content[i], list.get(i));
        }
    }

    public static void checkIndexOf(ListFactory factory) {
        List<Integer> list1 = createArrayList(10, 11, 12, 13, 14, 15, 12);
        List<Integer> list2 = createArrayList(16, 17, 18, 19, 20, 11, 14, 15, 19);
        List<Integer> list = factory.concatView(list1, list2);

        assertEquals(0, list.indexOf(10));
        assertEquals(1, list.indexOf(11));
        assertEquals(2, list.indexOf(12));
        assertEquals(3, list.indexOf(13));
        assertEquals(4, list.indexOf(14));
        assertEquals(5, list.indexOf(15));
        assertEquals(7, list.indexOf(16));
        assertEquals(8, list.indexOf(17));
        assertEquals(9, list.indexOf(18));
        assertEquals(10, list.indexOf(19));
        assertEquals(11, list.indexOf(20));
        assertEquals(-1, list.indexOf(100));
    }

    public static void checkLastIndexOf(ListFactory factory) {
        List<Integer> list1 = createArrayList(10, 11, 12, 13, 14, 15, 12);
        List<Integer> list2 = createArrayList(16, 17, 18, 19, 20, 11, 14, 15, 19);
        List<Integer> list = factory.concatView(list1, list2);

        assertEquals(0, list.lastIndexOf(10));
        assertEquals(12, list.lastIndexOf(11));
        assertEquals(6, list.lastIndexOf(12));
        assertEquals(3, list.lastIndexOf(13));
        assertEquals(13, list.lastIndexOf(14));
        assertEquals(14, list.lastIndexOf(15));
        assertEquals(7, list.lastIndexOf(16));
        assertEquals(8, list.lastIndexOf(17));
        assertEquals(9, list.lastIndexOf(18));
        assertEquals(15, list.lastIndexOf(19));
        assertEquals(11, list.lastIndexOf(20));
        assertEquals(-1, list.lastIndexOf(100));
    }

    private ConcatListTestMethods() {
        throw new AssertionError();
    }
}
