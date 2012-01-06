package org.jtrim.collections;

import java.util.*;

/**
 * A convenient {@link java.util.Iterator Iterator} for iterating through
 * the element references of a {@link RefList}. This {@code Iterator} will use
 * {@link RefList.ElementRef#getNext()} method of the element references
 * and the iterator remains valid even if the underlying list is changed while
 * it is being iterated. Note however that it doesn't imply thread-safety if the
 * underlying {@code RefList} implementation is not thread-safe.
 *
 * <h3>Thread safety</h3>
 * This class derives its thread-safety properties from the underlying
 * {@code RefList} object.
 *
 * <h4>Synchronization transparency</h4>
 * This class derives its synchronization transparency from the underlying
 * {@code RefList} object. Note however that in general
 * {@code java.util.Collection Collection} implementations are expected to be
 * completely synchronization transparent.
 *
 * @param <E> the type of the elements iterated by this {@code Iterator}
 *
 * @author Kelemen Attila
 */
public class ElementRefIterator<E> implements Iterator<RefList.ElementRef<E>> {
    private RefList.ElementRef<E> prevRef;
    private RefList.ElementRef<E> nextRef;

    /**
     * Creates an iterator with a specified first element reference.
     *
     * @param firstRef the first element reference to be returned by
     *   this {@code Iterator}. This argument can be {@code null} in which case
     *   the iterator is empty.
     */
    public ElementRefIterator(RefList.ElementRef<E> firstRef) {
        this.prevRef = null;
        this.nextRef = firstRef;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean hasNext() {
        return nextRef != null;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public RefList.ElementRef<E> next() {
        if (!hasNext()) {
            throw new NoSuchElementException("The last reference was reached.");
        }
        RefList.ElementRef<E> result = nextRef;
        nextRef = nextRef.getNext(1);
        prevRef = result;
        return result;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void remove() {
        if (prevRef == null) {
            throw new IllegalStateException("next() was not called yet.");
        }
        prevRef.remove();
    }
}
