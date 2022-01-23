package org.jtrim2.collections;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jtrim2.collections.RefList.ElementRef;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class CollectionsExTest {
    public static class HashMapTests extends HashFactoryTest<HashMap<Object, Object>> {
        public HashMapTests() {
            super(CollectionsEx::newHashMap, CollectionsEx::newHashMap);
        }
    }

    public static class HashSetTests extends HashFactoryTest<HashSet<Object>> {
        public HashSetTests() {
            super(CollectionsEx::newHashSet, CollectionsEx::newHashSet);
        }
    }

    public static class LinkedHashMapTests extends HashFactoryTest<LinkedHashMap<Object, Object>> {
        public LinkedHashMapTests() {
            super(CollectionsEx::newLinkedHashMap, CollectionsEx::newLinkedHashMap);
        }
    }

    public static class LinkedHashSetTests extends HashFactoryTest<LinkedHashSet<Object>> {
        public LinkedHashSetTests() {
            super(CollectionsEx::newLinkedHashSet, CollectionsEx::newLinkedHashSet);
        }
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(CollectionsEx.class);
    }

    private static void checkFromPosition(List<Integer> list, int startPos, Integer...content) {
        checkFromPositionForward(list, startPos, content);
        checkFromPositionBackward(list, startPos, content);
    }

    private static void checkFromPositionForward(List<Integer> list, int startPos, Integer...content) {
        ListIterator<Integer> itr = list.listIterator(startPos);
        for (int i = startPos; i < content.length; i++) {
            assertEquals(i - 1, itr.previousIndex());
            assertEquals(i, itr.nextIndex());
            assertTrue(itr.hasNext());
            assertEquals(content[i], itr.next());
        }
        assertFalse(itr.hasNext());
    }

    private static void checkFromPositionBackward(List<Integer> list, int startPos, Integer...content) {
        ListIterator<Integer> itr = list.listIterator(startPos);
        for (int i = startPos - 1; i >= 0; i--) {
            assertEquals(i, itr.previousIndex());
            assertEquals(i + 1, itr.nextIndex());
            assertTrue(itr.hasPrevious());
            assertEquals(content[i], itr.previous());
        }
        assertFalse(itr.hasPrevious());
    }


    public static void checkListContent(List<Integer> list, Integer... content) {
        //assertEquals(content.length, list.size());

        Iterator<Integer> itr = list.iterator();
        for (int i = 0; i < content.length; i++) {
            assertTrue(itr.hasNext());
            assertEquals(content[i], itr.next());
        }
        assertFalse(itr.hasNext());

        ListIterator<Integer> listItr = list.listIterator();
        for (int i = 0; i < content.length; i++) {
            assertEquals(i - 1, listItr.previousIndex());
            assertEquals(i, listItr.nextIndex());
            assertTrue(listItr.hasNext());
            assertEquals(content[i], listItr.next());
        }
        assertFalse(listItr.hasNext());
        for (int i = content.length - 1; i >= 0; i--) {
            assertEquals(i, listItr.previousIndex());
            assertEquals(i + 1, listItr.nextIndex());
            assertTrue(listItr.hasPrevious());
            assertEquals(content[i], listItr.previous());
        }
        assertFalse(listItr.hasPrevious());

        // Note: Starting from content.length is allowed.
        for (int i = 0; i <= content.length; i++) {
            checkFromPosition(list, i, content);
        }
    }

    /**
     * Test of newIdentityHashSet method, of class CollectionsEx.
     */
    @Test
    public void testNewIdentityHashSet() {
        // We can only test that it does not fail miserably.
        CollectionsEx.newIdentityHashSet(0);
        CollectionsEx.newIdentityHashSet(1);
        CollectionsEx.newIdentityHashSet(26);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNewIdentityHashSetError() {
        CollectionsEx.newIdentityHashSet(-1);
    }

    private static void expect(Class<? extends Throwable> exception, Runnable task) {
        assert exception != null;
        assert task != null;

        try {
            task.run();
        } catch (Throwable thrown) {
            assertTrue("Expected exception: " + exception.getName() + " but received: " + thrown.getClass().getName(),
                    exception.isAssignableFrom(thrown.getClass()));
            return;
        }
        fail("Expected exception: " + exception.getName());
    }

    public static void checkIfReadOnly(final List<Integer> list) {
        expect(UnsupportedOperationException.class, () -> list.add(5));
        expect(UnsupportedOperationException.class, () -> list.add(0, 5));
        expect(UnsupportedOperationException.class, () -> list.addAll(Arrays.asList(5)));
        expect(UnsupportedOperationException.class, () -> list.addAll(0, Arrays.asList(5)));

        if (!list.isEmpty()) {
            // Collections.emptyList().retainAll(?) does not throw an exception
            expect(UnsupportedOperationException.class, () -> list.retainAll(Arrays.asList(5)));
            // Collections.emptyList().removeAll(?) does not throw an exception
            expect(UnsupportedOperationException.class, () -> list.removeAll(Arrays.asList(5)));
            // Collections.emptyList().remove(?) does not throw an exception
            expect(UnsupportedOperationException.class, () -> list.remove(Integer.valueOf(10)));
            // Collections.emptyList().clear() does not throw an exception
            expect(UnsupportedOperationException.class, list::clear);
            expect(UnsupportedOperationException.class, () -> list.set(0, 5));
            expect(UnsupportedOperationException.class, () -> list.remove(0));
        }
    }

    /**
     * Test of readOnlyCopy method, of class CollectionsEx.
     */
    @Test
    public void testReadOnlyCopy() {
        Integer[] expected = new Integer[]{10, 11, 12};
        final List<Integer> list = CollectionsEx.readOnlyCopy(createLinearList(expected));
        checkListContent(list, expected);
        checkIfReadOnly(list);

        checkIfReadOnly(CollectionsEx.readOnlyCopy(new LinkedList<>()));
    }

    private static List<Integer> createLinearList(Integer... content) {
        return new LinkedList<>(Arrays.asList(content));
    }

    private static List<Integer> createRandomList(Integer... content) {
        return new ArrayList<>(Arrays.asList(content));
    }

    /**
     * Test of viewConcatList method, of class CollectionsEx.
     */
    @Test
    public void testViewConcatList() {
        List<Integer> randomList = CollectionsEx.viewConcatList(createRandomList(11, 12, 13), createRandomList(14, 15));
        assertTrue(randomList instanceof RandomAccessConcatListView);
        checkListContent(randomList, 11, 12, 13, 14, 15);

        List<Integer> list = CollectionsEx.viewConcatList(createLinearList(11, 12, 13), createLinearList(14, 15));
        assertTrue(list instanceof ConcatListView);
        checkListContent(list, 11, 12, 13, 14, 15);

        list = CollectionsEx.viewConcatList(createLinearList(11, 12, 13), createRandomList(14, 15));
        assertTrue(list instanceof ConcatListView);
        checkListContent(list, 11, 12, 13, 14, 15);

        list = CollectionsEx.viewConcatList(createRandomList(11, 12, 13), createLinearList(14, 15));
        assertTrue(list instanceof ConcatListView);
        checkListContent(list, 11, 12, 13, 14, 15);

        list = CollectionsEx.viewConcatList(randomList, createRandomList(16, 17));
        assertTrue(list instanceof ConcatListView);
        checkListContent(list, 11, 12, 13, 14, 15, 16, 17);

        list = CollectionsEx.viewConcatList(createRandomList(10), randomList);
        assertTrue(list instanceof ConcatListView);
        checkListContent(list, 10, 11, 12, 13, 14, 15);
    }

    @Test
    public void testViewListWithHead() {
        List<Integer> view = CollectionsEx.viewListWithHead(13, createRandomList(14, 15, 1));
        checkListContent(view, 13, 14, 15, 1);
    }

    @Test
    public void testViewListWithTail() {
        List<Integer> view = CollectionsEx.viewListWithTail(createRandomList(9, 13, 14), 15);
        checkListContent(view, 9, 13, 14, 15);
    }

    @Test
    public void testGetDetachedListRef() {
        ElementRef<Integer> ref = CollectionsEx.getDetachedListRef(5);
        assertTrue(ref instanceof DetachedListRef);
        assertEquals(5, ref.getElement().intValue());
    }

    @Test
    public void testNaturalComparatorCompare() {
        MyObj obj1 = new MyObj(5);
        MyObj obj2 = new MyObj(2);

        assertEquals(obj1.compareTo(obj2), CollectionsEx.naturalOrder().compare(obj1, obj2));
        assertEquals(obj2.compareTo(obj1), CollectionsEx.naturalOrder().compare(obj2, obj1));
    }

    @Test(expected = NullPointerException.class)
    public void testNaturalComparatorCompareNull1() {
        CollectionsEx.naturalOrder().compare(null, new MyObj(100));
    }

    @Test(expected = NullPointerException.class)
    public void testNaturalComparatorCompareNull2() {
        CollectionsEx.naturalOrder().compare(new MyObj(100), null);
    }

    private Map<TestEnum, Object> testCopyToEnumMapEmpty(
            BiFunction<Class<TestEnum>, Map<TestEnum, Object>, Map<TestEnum, Object>> factory) {

        Map<TestEnum, Object> copy = factory.apply(TestEnum.class, Collections.emptyMap());
        assertTrue("empty", copy.isEmpty());
        return copy;
    }

    private Map<TestEnum, Object> testCopyToEnumMapSingleElement(
            BiFunction<Class<TestEnum>, Map<TestEnum, Object>, Map<TestEnum, Object>> factory) {

        Map<TestEnum, Object> src = Collections.singletonMap(TestEnum.INST2, "Value-34543934");
        Map<TestEnum, Object> copy = factory.apply(TestEnum.class, src);
        assertEquals(src, copy);
        return copy;
    }

    private Map<TestEnum, Object> testCopyToEnumMapMultipleElement(
            BiFunction<Class<TestEnum>, Map<TestEnum, Object>, Map<TestEnum, Object>> factory) {

        Map<TestEnum, Object> src = new HashMap<>();
        for (TestEnum key : TestEnum.values()) {
            src.put(key, "Value-" + key);
        }

        Map<TestEnum, Object> copy = factory.apply(TestEnum.class, src);
        assertEquals(src, copy);
        return copy;
    }

    @Test
    public void testCopyToEnumMapEmpty() {
        testCopyToEnumMapEmpty(CollectionsEx::copyToEnumMap);
    }

    @Test
    public void testCopyToEnumMapSingleElement() {
        testCopyToEnumMapSingleElement(CollectionsEx::copyToEnumMap);
    }

    @Test
    public void testCopyToEnumMapMultipleElement() {
        testCopyToEnumMapMultipleElement(CollectionsEx::copyToEnumMap);
    }

    private static void verifyReadOnlyMap(Map<TestEnum, Object> map) {
        TestUtils.expectError(UnsupportedOperationException.class, () -> {
            map.put(TestEnum.INST1, "ShouldNotSet");
        });
    }

    @Test
    public void testCopyToReadOnlyEnumMapEmpty() {
        Map<TestEnum, Object> result = testCopyToEnumMapEmpty(CollectionsEx::copyToReadOnlyEnumMap);
        verifyReadOnlyMap(result);
    }

    @Test
    public void testCopyToReadOnlyEnumMapSingleElement() {
        Map<TestEnum, Object> result = testCopyToEnumMapSingleElement(CollectionsEx::copyToReadOnlyEnumMap);
        verifyReadOnlyMap(result);
    }

    @Test
    public void testCopyToReadOnlyEnumMapMultipleElement() {
        Map<TestEnum, Object> result = testCopyToEnumMapMultipleElement(CollectionsEx::copyToReadOnlyEnumMap);
        verifyReadOnlyMap(result);
    }

    private <K, V> Map<K, V> testMapBuilder(
            Map<K, V> expected,
            Function<Consumer<? super Map<K, V>>, Map<K, V>> builder) {

        Map<K, V> result = builder.apply(map -> {
            expected.forEach(map::put);
        });

        assertEquals(expected, result);
        return result;
    }

    @Test
    public void testMapBuildersForEnum() {
        Map<TestEnum, String> expected = new HashMap<>();
        expected.put(TestEnum.INST1, "Value1");
        expected.put(TestEnum.INST2, "Value26");
        expected = Collections.unmodifiableMap(expected);

        testMapBuilder(expected, CollectionsEx::newHashMap);
        testMapBuilder(expected, contentConfig -> CollectionsEx.newEnumMap(TestEnum.class, contentConfig));

        Map<TestEnum, String> result = testMapBuilder(
                expected,
                contentConfig -> CollectionsEx.newMap(TestEnum.class, contentConfig));
        assertTrue(result instanceof EnumMap);
    }

    @Test
    public void testMapBuildersForObj() {
        Map<String, String> expected = new HashMap<>();
        expected.put("Key1", "Value1");
        expected.put("Key2", "Value26");
        expected = Collections.unmodifiableMap(expected);

        testMapBuilder(expected, CollectionsEx::newHashMap);
        Map<String, String> result = testMapBuilder(
                expected,
                contentConfig -> CollectionsEx.newMap(String.class, contentConfig));
        assertTrue(result instanceof HashMap);
    }

    private static List<MyObj> myObjList(int... values) {
        List<MyObj> result = new ArrayList<>(values.length);
        for (int value : values) {
            result.add(new MyObj(value));
        }
        return result;
    }

    private static void assertEqualsMyObjs(List<MyObj> actual, int... expected) {
        int[] actualValues = new int[actual.size()];
        for (int i = actual.size() - 1; i >= 0 ; i--) {
            actualValues[i] = actual.get(i).value;
        }
        assertArrayEquals(expected, actualValues);
    }

    @Test
    public void testFilterToNewListEmpty() {
        ArrayList<MyObj> result = CollectionsEx
                .filterToNewList(Collections.emptyList(), e -> e.value % 3 != 0);
        assertEqualsMyObjs(result);
    }

    @Test
    public void testFilterToNewListMany() {
        ArrayList<MyObj> result = CollectionsEx
                .filterToNewList(myObjList(1, 2, 3, 4, 5, 6, 7), e -> e.value % 3 != 0);
        assertEqualsMyObjs(result, 1, 2, 4, 5, 7);
    }

    @Test
    public void testMapToNewListEmpty() {
        ArrayList<Integer> result = CollectionsEx
                .mapToNewList(Collections.<MyObj>emptyList(), e -> e.value + 1);
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testMapToNewListMany() {
        ArrayList<Integer> result = CollectionsEx
                .mapToNewList(myObjList(1, 2, 3, 4, 5, 6, 7), e -> e.value + 1);
        assertEquals(Arrays.asList(2, 3, 4, 5, 6, 7, 8), result);
    }

    @Test
    public void testFilterAndMapToNewListEmpty() {
        ArrayList<Integer> result = CollectionsEx.filterAndMapToNewList(
                Collections.<MyObj>emptyList(),
                e -> e.value % 3 != 0,
                e -> e.value + 2
        );
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testFilterAndMapToNewListMany() {
        ArrayList<Integer> result = CollectionsEx.filterAndMapToNewList(
                myObjList(1, 2, 3, 4, 5, 6, 7),
                e -> e.value % 3 != 0,
                e -> e.value + 2
        );
        assertEquals(Arrays.asList(3, 4, 6, 7, 9), result);
    }

    @Test
    public void testMapAndFilterToNewListEmpty() {
        ArrayList<Integer> result = CollectionsEx.mapAndFilterToNewList(
                Collections.<MyObj>emptyList(),
                e -> e.value + 2,
                e -> e % 3 != 0
        );
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testMapAndFilterToNewListMany() {
        ArrayList<Integer> result = CollectionsEx.mapAndFilterToNewList(
                myObjList(1, 2, 3, 4, 5, 6, 7),
                e -> e.value + 2,
                e -> e % 3 != 0
        );
        assertEquals(Arrays.asList(4, 5, 7, 8), result);
    }

    public abstract static class HashFactoryTest<T> {
        private final HashFactory1<T> factory1;
        private final HashFactory2<T> factory2;

        private HashFactoryTest(HashFactory1<T> factory1, HashFactory2<T> factory2) {
            this.factory1 = factory1;
            this.factory2 = factory2;
        }

        @Test
        public void testNewHash1() {
            // We can only test that it does not fail miserably.
            factory1.create(0);
            factory1.create(1);
            factory1.create(26);
        }

        @Test(expected = IllegalArgumentException.class)
        public void testNewHash1Error() {
            factory1.create(-1);
        }

        @Test
        public void testNewHash2() {
            // We can only test that it does not fail miserably.
            factory2.create(0, 0.50f);
            factory2.create(1, 0.75f);
            factory2.create(26, 100.0f);
        }

        @Test(expected = IllegalArgumentException.class)
        public void testNewHash2Error1() {
            factory2.create(1, 0.0f);
        }

        @Test(expected = IllegalArgumentException.class)
        public void testNewHash2Error2() {
            factory2.create(1, -0.25f);
        }

        @Test(expected = IllegalArgumentException.class)
        public void testNewHash2Error3() {
            factory2.create(1, Float.NaN);
        }
    }

    private interface HashFactory1<T> {
        public T create(int capacity);
    }

    private interface HashFactory2<T> {
        public T create(int capacity, float loadFactor);
    }

    private enum TestEnum {
        INST1,
        INST2
    }

    private static final class MyObj implements Comparable<Object> {
        private final int value;

        public MyObj(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(Object o) {
            return value - ((MyObj) o).value;
        }
    }
}
