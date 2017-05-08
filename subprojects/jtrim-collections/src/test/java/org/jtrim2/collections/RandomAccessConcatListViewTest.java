package org.jtrim2.collections;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.junit.Assert;
import org.junit.Test;

public class RandomAccessConcatListViewTest {
    @Test
    public void testSerialize() throws IOException, ClassNotFoundException {
        ConcatListTestMethods.checkSerialization(RandomListFactory.INSTANCE);
    }

    @Test
    public void testSimpleCreate() {
        ConcatListTestMethods.checkSimpleCreate(RandomListFactory.INSTANCE);
    }

    @Test
    public void testContains() {
        ConcatListTestMethods.checkContains(RandomListFactory.INSTANCE);
    }

    @Test
    public void testGet() {
        ConcatListTestMethods.checkGet(RandomListFactory.INSTANCE);
    }

    @Test
    public void testIndexOf() {
        ConcatListTestMethods.checkIndexOf(RandomListFactory.INSTANCE);
    }

    @Test
    public void testIterator() {
        ConcatListTestMethods.checkIterator(RandomListFactory.INSTANCE);
    }

    @Test
    public void testLastIndexOf() {
        ConcatListTestMethods.checkLastIndexOf(RandomListFactory.INSTANCE);
    }

    @Test
    public void testToArray() {
        ConcatListTestMethods.checkToArray(RandomListFactory.INSTANCE);
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
        list.get(list.size() + 1);
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
        List<Integer> list = RandomListFactory.INSTANCE.concatView(Arrays.asList(1, 2), Arrays.asList(3, 4, 5));
        Assert.assertEquals(5, list.size());
        return list;
    }

    private enum RandomListFactory implements ConcatListTestMethods.ListFactory {
        INSTANCE;

        @Override
        public <E> List<E> concatView(List<? extends E> list1, List<? extends E> list2) {
            return new RandomAccessConcatListView<>(list1, list2);
        }
    }
}
