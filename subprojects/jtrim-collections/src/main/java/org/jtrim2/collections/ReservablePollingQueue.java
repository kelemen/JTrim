package org.jtrim2.collections;

/**
 * Defines a simple queue from which you can poll elements, but still keep reserving
 * space in a limited capacity implementation after the element was polled.
 * <P>
 * Note: Implementations of this queue may not support {@code null} elements.
 * <P>
 * See {@link ReservablePollingQueues} for some default implementations.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are not required to be thread-safe and
 * it is implementation dependent, if they are allowed to be called from
 * multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are required to be
 * <I>synchronization transparent</I>, so they can be called in any context
 * (e.g.: while holding a lock).
 *
 * @param <T> the type of the elements allowed in the queue
 *
 * @see ReservablePollingQueues
 */
public interface ReservablePollingQueue<T> {
    /**
     * Checks if this queue contains any element or not. If this method return
     * {@code true}, then {@link #poll() poll} and {@link #pollButKeepReserved() pollButKeepReserved}
     * will return {@code null} unless this queue gets new element added (normally by
     * calling {@link #offer(Object) offer}).
     * <P>
     * Note that an empty queue does not mean that you will be able to add
     * elements to it, even if this queue has a capacity more than zero,
     * because space reservation not released are not counted for the purpose
     * of {@code isEmpty}.
     *
     * @return {@code true} if this queue has some elements to be polled,
     *   {@code false} otherwise
     */
    boolean isEmpty();

    /**
     * Checks if this queue contains any element or not, and that there is no element reservation left.
     * If this method return {@code true}, then {@link #poll() poll} and
     * {@link #pollButKeepReserved() pollButKeepReserved} will return {@code null} unless this queue
     * gets new element added (normally by calling {@link #offer(Object) offer}).
     * <P>
     * If this method returns {@code true} and the queue has no implementation specific extra
     * restrictions, then {@link #offer(Object) adding an element} will succeed.
     *
     * @return {@code true} if this queue has some elements to be polled,
     *   {@code false} otherwise
     */
    boolean isEmptyAndNoReserved();

    /**
     * Adds the given element to the queue unless this queue as at its maximum
     * capacity. If the queue is currently at full capacity, then this method
     * will do nothing, but return {@code false}.
     *
     * @param entry the new element to be added to the queue. This argument cannot
     *   be {@code null}.
     * @return {@code true} if the given element was added to the queue, {@code false}
     *   if the queue was full and the queue remains unchanged
     */
    boolean offer(T entry);

    /**
     * Removes one element from this queue, if there is one, but if this queue has limited
     * capacity, don't keep reserving space preventing adding new element. This space
     * reservation can be released by calling the {@link ReservedElementRef#release() release}
     * on the returned reference. That is, until you call {@code release}, the {@link #offer(Object) offset}
     * method will behave as if you did not yet remove this element.
     *
     * @return the reference to the polled element or {@code null} if this queue is currently empty
     */
    ReservedElementRef<T> pollButKeepReserved();

    /**
     * Removes one element from this queue, if there is one. This method is effectively
     * equivalent to calling {@link #pollButKeepReserved() pollButKeepReserved} and immediately
     * {@link ReservedElementRef#release() release} the reference.
     *
     * @return the element polled from this queue or {@code null} if this queue is currently empty
     */
    default T poll() {
        ReservedElementRef<T> result = pollButKeepReserved();
        if (result == null) {
            return null;
        }

        result.release();
        return result.element();
    }
}
