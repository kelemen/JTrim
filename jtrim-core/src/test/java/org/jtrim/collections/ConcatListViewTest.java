package org.jtrim.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Kelemen Attila
 */
public class ConcatListViewTest {

    public ConcatListViewTest() {
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

    @Test
    public void testSimpleCreate() {
        ConcatListTestMethods.checkSimpleCreate(LinearListFactory.INSTANCE);
    }

    @Test
    public void testContains() {
        ConcatListTestMethods.checkContains(LinearListFactory.INSTANCE);
    }

    @Test
    public void testGet() {
        ConcatListTestMethods.checkGet(LinearListFactory.INSTANCE);
    }

    @Test
    public void testIndexOf() {
        ConcatListTestMethods.checkIndexOf(LinearListFactory.INSTANCE);
    }

    @Test
    public void testIterator() {
        ConcatListTestMethods.checkIterator(LinearListFactory.INSTANCE);
    }

    @Test
    public void testLastIndexOf() {
        ConcatListTestMethods.checkLastIndexOf(LinearListFactory.INSTANCE);
    }

    @Test
    public void testToArray() {
        ConcatListTestMethods.checkToArray(LinearListFactory.INSTANCE);
    }

    @Test
    public void testManyLists() {
        List<Integer> list1 = Arrays.asList(10, 11, 12);
        List<Integer> list2 = Arrays.asList(13, 14);
        List<Integer> list3 = Arrays.asList(15, 16, 17, 18);
        List<Integer> list4 = Arrays.asList(19, 20, 21);

        ConcatListTestMethods.ListFactory factory = LinearListFactory.INSTANCE;

        List<Integer> concatList = factory.concatView(list1, new RandomAccessConcatListView<>(list2, list3));
        ConcatListTestMethods.checkListContent(concatList, 10, 11, 12, 13, 14, 15, 16, 17, 18);

        concatList = factory.concatView(list1, new ConcatListView<>(list2, list3));
        ConcatListTestMethods.checkListContent(concatList, 10, 11, 12, 13, 14, 15, 16, 17, 18);

        concatList = factory.concatView(new RandomAccessConcatListView<>(list1, list2), list3);
        ConcatListTestMethods.checkListContent(concatList, 10, 11, 12, 13, 14, 15, 16, 17, 18);

        concatList = factory.concatView(new ConcatListView<>(list1, list2), list3);
        ConcatListTestMethods.checkListContent(concatList, 10, 11, 12, 13, 14, 15, 16, 17, 18);

        concatList = factory.concatView(
                new ConcatListView<>(list1, Collections.<Integer>emptyList()),
                new ConcatListView<>(list2, list3));
        ConcatListTestMethods.checkListContent(concatList, 10, 11, 12, 13, 14, 15, 16, 17, 18);

        concatList = factory.concatView(
                new ConcatListView<>(list1, list2),
                new ConcatListView<>(Collections.<Integer>emptyList(), Collections.<Integer>emptyList()));
        ConcatListTestMethods.checkListContent(concatList, 10, 11, 12, 13, 14);

        concatList = factory.concatView(
                new ConcatListView<>(Collections.<Integer>emptyList(), Collections.<Integer>emptyList()),
                new ConcatListView<>(Collections.<Integer>emptyList(), Collections.<Integer>emptyList()));
        ConcatListTestMethods.checkListContent(concatList);

        concatList = factory.concatView(
                new ConcatListView<>(list1, list2),
                new ConcatListView<>(list3, list4));
        ConcatListTestMethods.checkListContent(concatList, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);

        concatList = factory.concatView(
                new ConcatListView<>(list1, list2),
                new RandomAccessConcatListView<>(list3, list4));
        ConcatListTestMethods.checkListContent(concatList, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);

        concatList = factory.concatView(
                new RandomAccessConcatListView<>(list1, list2),
                new ConcatListView<>(list3, list4));
        ConcatListTestMethods.checkListContent(concatList, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);

        concatList = factory.concatView(
                new RandomAccessConcatListView<>(list1, list2),
                new RandomAccessConcatListView<>(list3, list4));
        ConcatListTestMethods.checkListContent(concatList, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInvalidGet1() {
        createList().get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInvalidGet2() {
        List<Integer> list = createList();
        list.get(list.size());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInvalidListIteratorStart1() {
        createList().listIterator(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testInvalidListIteratorStart2() {
        List<Integer> list = createList();
        list.listIterator(list.size() + 1);
    }

    @Test(expected = NoSuchElementException.class)
    public void testInvalidListIteratorPrevious() {
        createList().listIterator().previous();
    }

    @Test(expected = NoSuchElementException.class)
    public void testInvalidListIteratorNext() {
        Iterator<Integer> itr;
        try {
            List<Integer> list = createList();
            itr = list.listIterator(list.size());
        } catch (NoSuchElementException ex) {
            throw new RuntimeException(ex);
        }
        itr.next();
    }

    @Test(expected = NoSuchElementException.class)
    public void testInvalidIteratorNext() {
        Iterator<Integer> itr;
        try {
            List<Integer> list = createList();
            itr = list.iterator();
            int size = list.size();
            for (int i = 0; i < size; i++) {
                itr.next();
            }
        } catch (NoSuchElementException ex) {
            throw new RuntimeException(ex);
        }
        itr.next();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAdd() {
        createList().add(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemove() {
        createList().remove(Integer.valueOf(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAll() {
        createList().addAll(Arrays.asList(10));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllAtIndex() {
        createList().addAll(1, Arrays.asList(1, 10));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAll() {
        createList().removeAll(Arrays.asList(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRetainAll() {
        createList().retainAll(Arrays.asList(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClear() {
        createList().clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetAtIndex() {
        createList().set(0, 100);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAtIndex() {
        createList().add(1, 100);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveAtIndex() {
        createList().remove(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorRemove() {
        Iterator<Integer> itr = createList().iterator();
        itr.next();
        itr.remove();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorRemove() {
        ListIterator<Integer> itr = createList().listIterator();
        itr.next();
        itr.remove();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorAdd() {
        ListIterator<Integer> itr = createList().listIterator();
        itr.next();
        itr.add(100);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorSet() {
        ListIterator<Integer> itr = createList().listIterator();
        itr.next();
        itr.set(100);
    }

    private static List<Integer> createList() {
        List<Integer> list = LinearListFactory.INSTANCE.concatView(Arrays.asList(1, 2), Arrays.asList(3, 4, 5));
        Assert.assertEquals(5, list.size());
        return list;
    }

    private enum LinearListFactory implements ConcatListTestMethods.ListFactory {
        INSTANCE;

        @Override
        public <E> List<E> concatView(List<? extends E> list1, List<? extends E> list2) {
            return new ConcatListView<>(list1, list2);
        }
    }
}
