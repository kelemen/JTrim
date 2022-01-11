package org.jtrim2.collections;

import java.util.Iterator;
import java.util.Objects;

/**
 * A convenient {@link Iterable} for iterating through the element references of
 * a {@link RefList}. This {@code Iterable} will use
 * {@link RefList.ElementRef#getNext(int)} method of the element references of
 * the underlying {@code RefList} and the returned iterator remains valid even
 * if the underlying list is changed while it is being iterated. Note however
 * that it doesn't imply thread-safety if the underlying {@code RefList}
 * implementation is not thread-safe.
 *
 * <h2>Thread safety</h2>
 * This class derives its thread-safety properties from the underlying
 * {@code RefList} object.
 *
 * <h3>Synchronization transparency</h3>
 * This class derives its synchronization transparency from the underlying
 * {@code RefList} object. Note however that in general
 * {@code java.util.Collection Collection} implementations are expected to be
 * completely synchronization transparent.
 *
 * @param <E> the type of the elements iterated by this {@code Iterable}
 */
public final class ElementRefIterable<E>
implements
        Iterable<RefList.ElementRef<E>>, java.io.Serializable {
    private static final long serialVersionUID = -6579680445229300067L;

    private final RefList<E> wrappedList;

    /**
     * Creates a new {@link Iterable} based on the specified {@link RefList}.
     * This {@code Iterable} will always start iterating from the first element
     * of the specified list even if it was changed after creating this
     * {@code Iterable}.
     *
     * @param wrappedList the underlying {@code RefList} to be used. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the supplied {@code RefList} is
     *   {@code null}
     */
    public ElementRefIterable(RefList<E> wrappedList) {
        Objects.requireNonNull(wrappedList, "wrappedList");

        this.wrappedList = wrappedList;
    }

    /**
     * Returns an iterator which will iterate through the elements of the
     * underlying {@link RefList}. The iterator will always start iterating from
     * the first element of the list.
     *
     * @return an iterator iterating through each of the elements of
     *   the underlying {@code RefList}. This method never returns {@code null}.
     */
    @Override
    public Iterator<RefList.ElementRef<E>> iterator() {
        return new ElementRefIterator<>(wrappedList.getFirstReference());
    }
}
