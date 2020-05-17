package org.jtrim2.collections;

/**
 * Defines an element removed from a collection, but still taking up space as if it was there.
 * To release the space this reference
 *
 * <h3>Thread safety</h3>
 * The thread-safety of the instances of this class depends on its owning collection. If the
 * collection is not safe to be used from multiple threads concurrently, then this reference
 * must not be accessed concurrently by the owning collection.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are required to be
 * <I>synchronization transparent</I>, so they can be called in any context
 * (e.g.: while holding a lock).
 *
 * @param <T> the type of the element referenced
 *
 * @see ReservablePollingQueue
 * @see ReservablePollingQueues
 */
public interface ReservedElementRef<T> {
    /**
     * Returns the referenced element. The element may be retrieved multiple times even after calling
     * {@link #release() release}, and each call will return the same object (keeping identity).
     *
     * @return the referenced element. May only return {@code null}, iff the owning collection supports
     *   {@code null} values.
     */
    public T element();

    /**
     * Completely frees the reservation of this element from the owning collection, so that this element
     * takes up no space there. Freeing the reservation must have happened before this method returns to
     * the caller, unless the owning collection otherwise defines it.
     * <P>
     * This method is idempotent, and subsequent calls to this method will have no further effect.
     */
    public void release();
}

