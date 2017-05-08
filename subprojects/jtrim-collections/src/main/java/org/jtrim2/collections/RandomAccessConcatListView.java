package org.jtrim2.collections;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

/**
 * @see CollectionsEx#viewConcatList(java.util.List, java.util.List)
 */
final class RandomAccessConcatListView<E> extends AbstractList<E>
        implements RandomAccess, Serializable {
    private static final long serialVersionUID = 4956280583605644080L;

    // Accessed by ConcantListView
    final ConcatListView<E> simpleView;

    public RandomAccessConcatListView(
            List<? extends E> list1, List<? extends E> list2) {

        this.simpleView = new ConcatListView<>(list1, list2);
    }

    @Override
    public int size() {
        return simpleView.size();
    }

    @Override
    public boolean isEmpty() {
        return simpleView.isEmpty();
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean contains(Object o) {
        return simpleView.contains(o);
    }

    @Override
    public Object[] toArray() {
        return simpleView.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return simpleView.toArray(a);
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("This list is readonly.");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("This list is readonly.");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return simpleView.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException("This list is readonly.");
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException("This list is readonly.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("This list is readonly.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("This list is readonly.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("This list is readonly.");
    }

    @Override
    public E get(int index) {
        return simpleView.get(index);
    }

    @Override
    public E set(int index, E element) {
        throw new UnsupportedOperationException("This list is readonly.");
    }

    @Override
    public void add(int index, E element) {
        throw new UnsupportedOperationException("This list is readonly.");
    }

    @Override
    public E remove(int index) {
        throw new UnsupportedOperationException("This list is readonly.");
    }

    @Override
    public int indexOf(Object o) {
        return simpleView.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return simpleView.lastIndexOf(o);
    }
}
