package org.jtrim2.collections;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 *
 * @see CollectionsEx#viewConcatList(java.util.List, java.util.List)
 */
final class ConcatListView<E> extends AbstractList<E> implements Serializable {
    private static final long serialVersionUID = 5322734243164292647L;

    private final List<? extends E>[] lists;

    private static <E> void addLists(List<? extends E> list,
            List<List<? extends E>> result) {

        if (list instanceof ConcatListView<?>) {
            ConcatListView<? extends E> concatList
                    = (ConcatListView<? extends E>)list;

            result.addAll(Arrays.asList(concatList.lists));
        }
        else if (list instanceof RandomAccessConcatListView<?>) {
            RandomAccessConcatListView<? extends E> concatList
                    = (RandomAccessConcatListView<? extends E>)list;

            result.addAll(Arrays.asList(concatList.simpleView.lists));
        }
        else {
            result.add(list);
        }
    }

    public ConcatListView(List<? extends E> list1, List<? extends E> list2) {
        Objects.requireNonNull(list1, "list1");
        Objects.requireNonNull(list2, "list2");

        List<List<? extends E>> simpleLists = new LinkedList<>();
        addLists(list1, simpleLists);
        addLists(list2, simpleLists);

        @SuppressWarnings("unchecked")
        List<? extends E>[] currentLists
                = (List<? extends E>[])new List<?>[simpleLists.size()];

        int index = 0;
        for (List<? extends E> list: simpleLists) {
            currentLists[index] = list;
            index++;
        }

        assert index == currentLists.length;
        this.lists = currentLists;
    }

    @Override
    public int size() {
        int result = 0;
        for (List<? extends E> list: lists) {
            result += list.size();
        }

        return result;
    }

    @Override
    public boolean isEmpty() {
        for (List<? extends E> list: lists) {
            if (!list.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    @SuppressWarnings("element-type-mismatch")
    public boolean contains(Object o) {
        for (List<? extends E> list: lists) {
            if (list.contains(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[size()];
        int index = 0;

        for (List<? extends E> list: lists) {
            Object[] elements = list.toArray();
            System.arraycopy(elements, 0, result, index, elements.length);
            index += elements.length;
        }

        return result;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        int reqLength = size();

        Object[] result;
        if (a.length >= reqLength) {
            result = a;
        }
        else {
            result = (Object[])java.lang.reflect.Array.newInstance(
                    a.getClass().getComponentType(), reqLength);
        }

        int index = 0;

        for (List<? extends E> list: lists) {
            Object[] elements = list.toArray();
            System.arraycopy(elements, 0, result, index, elements.length);
            index += elements.length;
        }

        if (index < result.length) {
            result[index] = null;
        }

        @SuppressWarnings("unchecked")
        T[] toReturn = (T[])result;
        return toReturn;
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
        if (index < 0) {
            throw new IndexOutOfBoundsException("Invalid starting index for a list: " + index);
        }

        int offset = 0;
        for (List<? extends E> list: lists) {
            int currentSize = list.size();
            int nextOffset = offset + currentSize;
            if (index < nextOffset) {
                return list.get(index - offset);
            }
            offset = nextOffset;
        }

        throw new IndexOutOfBoundsException("The index is too large: "
                + index + ". Upper bound: " + size());
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
        int listCount = lists.length;
        int offset = 0;
        for (int i = 0; i < listCount; i++) {
            List<? extends E> list = lists[i];

            int index = list.indexOf(o);
            if (index >= 0) {
                return index + offset;
            }
            offset += list.size();
        }

        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int offset = size();
        for (int i = lists.length - 1; i >= 0; i--) {
            List<? extends E> list = lists[i];
            offset -= list.size();

            int index = list.lastIndexOf(o);
            if (index >= 0) {
                return index + offset;
            }
        }

        return -1;
    }

    @Override
    public Iterator<E> iterator() {
        return new ConcatIterator<>(lists);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new ConcatListIterator<>(lists, 0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new ConcatListIterator<>(lists, index);
    }

    private static class ConcatIterator<E> implements Iterator<E> {
        private final Iterator<? extends E>[] itrs;
        private int itrIndex;

        public ConcatIterator(List<? extends E>[] lists) {
            @SuppressWarnings("unchecked")
            Iterator<? extends E>[] currentItrs
                    = (Iterator<? extends E>[])new Iterator<?>[lists.length];

            for (int i = 0; i < lists.length; i++) {
                currentItrs[i] = lists[i].iterator();
            }

            this.itrs = currentItrs;
            this.itrIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return itrs[itrIndex].hasNext();
        }

        @Override
        public E next() {
            Iterator<? extends E> current = itrs[itrIndex];
            E result = current.next();

            int maxIndex = itrs.length - 1;
            if (itrIndex < maxIndex && !current.hasNext()) {
                int newIndex = itrIndex + 1;
                current = itrs[newIndex];

                while (newIndex < maxIndex && !current.hasNext()) {
                    newIndex++;
                    current = itrs[newIndex];
                }

                itrIndex = newIndex;
            }

            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("This list is readonly.");
        }
    }

    private static class ConcatListIterator<E> implements ListIterator<E> {
        private final ListIterator<? extends E>[] itrs;
        private int itrIndex;
        private int nextIndex;

        public ConcatListIterator(List<? extends E>[] lists, int startIndex) {
            if (startIndex < 0) {
                throw new IndexOutOfBoundsException("Invalid starting index for a list: " + startIndex);
            }

            @SuppressWarnings("unchecked")
            ListIterator<? extends E>[] currentItrs
                    = (ListIterator<? extends E>[])new ListIterator<?>[lists.length];

            int offset = 0;
            int index = 0;
            for (int i = 0; i < lists.length; i++) {
                List<? extends E> list = lists[i];

                if (offset < startIndex) {
                    int currentSize = list.size();
                    int itrStart = Math.min(currentSize, startIndex - offset);
                    currentItrs[i] = list.listIterator(itrStart);
                    offset += itrStart;
                    index = i;
                }
                else {
                    currentItrs[i] = list.listIterator();
                }
            }

            if (offset != startIndex) {
                throw new IndexOutOfBoundsException("The iterator cannot start"
                        + " at " + startIndex);
            }

            this.itrs = currentItrs;
            this.itrIndex = index;
            this.nextIndex = startIndex;

            moveNextNotEmpty();
        }

        @Override
        public boolean hasNext() {
            return itrs[itrIndex].hasNext();
        }

        private void moveNextNotEmpty() {
            // Move forward and skip empty iterators.
            ListIterator<? extends E> current = itrs[itrIndex];
            int maxIndex = itrs.length - 1;
            if (itrIndex < maxIndex && !current.hasNext()) {
                int newIndex = itrIndex + 1;
                current = itrs[newIndex];

                while (newIndex < maxIndex && !current.hasNext()) {
                    newIndex++;
                    current = itrs[newIndex];
                }

                itrIndex = newIndex;
            }
        }

        @Override
        public E next() {
            ListIterator<? extends E> current = itrs[itrIndex];

            E result = current.next();
            nextIndex++;
            moveNextNotEmpty();

            return result;
        }

        @Override
        public boolean hasPrevious() {
            // Check previous iterators skipping empty iterators.
            for (int i = itrIndex; i >= 0; i--) {
                ListIterator<? extends E> current = itrs[i];
                if (current.hasPrevious()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public E previous() {
            // Check previous iterators skipping empty iterators.
            for (int i = itrIndex; i >= 0; i--) {
                ListIterator<? extends E> current = itrs[i];
                if (current.hasPrevious()) {
                    itrIndex = i;
                    nextIndex--;
                    return current.previous();
                }
            }

            throw new NoSuchElementException(
                    "The beginning of the list was reached.");
        }

        @Override
        public int nextIndex() {
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            return nextIndex - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("This list is readonly.");
        }

        @Override
        public void set(E e) {
            throw new UnsupportedOperationException("This list is readonly.");
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException("This list is readonly.");
        }

    }
}
