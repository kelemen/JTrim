package org.jtrim.collections;

import java.io.Serializable;
import java.util.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see ArraysEx#viewAsList(Object[], int, int)
 * @author Kelemen Attila
 */
final class ArrayView<E> extends AbstractList<E> implements RandomAccess, Serializable {
    private static final long serialVersionUID = 6130770601174237790L;

    private final int offset;
    private final int length;
    private final E[] array;

    public ArrayView(E[] array, int offset, int length) {
        ExceptionHelper.checkNotNullArgument(array, "array");

        if (length < 0) {
            throw new ArrayIndexOutOfBoundsException("length must be non-negative.");
        }

        if (offset < 0) {
            throw new ArrayIndexOutOfBoundsException("offset must be non-negative.");
        }

        int endIndex = offset + length;
        if (array.length < endIndex || endIndex < 0) {
            // if offset + length overflows the result will be a negative value.
            throw new ArrayIndexOutOfBoundsException(
                    "The array is not long enough."
                    + " Size: " + array.length
                    + ", Required: " + ((long)offset + (long)length));
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
                    return i;
                }
            }
        }
        else {
            for (int i = offset; i < endOffset; i++) {
                if (o.equals(array[i])) {
                    return i;
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
                    return i;
                }
            }
        }
        else {
            for (int i = lastIndex; i >= firstIndex; i--) {
                if (o.equals(array[i])) {
                    return i;
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
