package org.jtrim2.collections;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

/**
 * @see ArraysEx#viewAsList(Object[], int, int)
 */
final class ArrayView<E> extends AbstractList<E>
implements
        RandomAccess, Serializable {
    private static final long serialVersionUID = 6130770601174237790L;

    private final int offset;
    private final int length;
    private final E[] array;

    public ArrayView(E[] array, int offset, int length) {
        Objects.requireNonNull(array, "array");

        if (length < 0) {
            throw new ArrayIndexOutOfBoundsException(
                    "length must be non-negative.");
        }

        if (offset < 0) {
            throw new ArrayIndexOutOfBoundsException(
                    "offset must be non-negative.");
        }

        int endIndex = offset + length;
        if (array.length < endIndex || endIndex < 0) {
            // if offset + length overflows the result will be a negative value.
            throw new ArrayIndexOutOfBoundsException(
                    "The array is not long enough."
                    + " Size: " + array.length
                    + ", Required: " + ((long) offset + (long) length));
        }

        this.offset = offset;
        this.length = length;
        this.array = array;
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public boolean isEmpty() {
        return length == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[length];
        System.arraycopy(array, offset, result, 0, length);
        return result;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < length) {

            @SuppressWarnings("unchecked")
            T[] result = Arrays.copyOfRange(array, offset, offset + length,
                    (Class<? extends T[]>) a.getClass());

            return result;
        }

        System.arraycopy(array, offset, a, 0, length);

        if (a.length > length) {
            a[length] = null;
        }

        return a;
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
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " is not in range: [0, " + length + ")");
        }
       return array[offset + index];
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
        final int endOffset = offset + length;
        if (o == null) {
            for (int i = offset; i < endOffset; i++) {
                if (array[i] == null) {
                    return i - offset;
                }
            }
        } else {
            for (int i = offset; i < endOffset; i++) {
                if (o.equals(array[i])) {
                    return i - offset;
                }
            }
        }

        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        final int lastIndex = offset + length - 1;
        final int firstIndex = offset;

        if (o == null) {
            for (int i = lastIndex; i >= firstIndex; i--) {
                if (array[i] == null) {
                    return i - offset;
                }
            }
        } else {
            for (int i = lastIndex; i >= firstIndex; i--) {
                if (o.equals(array[i])) {
                    return i - offset;
                }
            }
        }

        return -1;
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return new ArrayView<>(array, offset + fromIndex, toIndex - fromIndex);
    }
}
