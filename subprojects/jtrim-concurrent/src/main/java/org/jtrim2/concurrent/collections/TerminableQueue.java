package org.jtrim2.concurrent.collections;

import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.ReservedElementRef;

/**
 * Defines a simple queue which can be stopped from accepting any new elements. Unlike
 * traditional blocking queues, you can easily prevent {@code put} and {@code take} calls
 * from waiting forever in case one side goes away.
 * <P>
 * Aside from being terminable, {@code TerminableQueue} also support removing elements
 * from the queue without immediately allowing new elements to be added to queue instead
 * of it (assuming the queue limits the number elements or has some other limitation on when
 * an element can be added to it).
 * <P>
 * The ordering of the queue is completely independent.
 * <P>
 * Implementations of this queue may not support {@code null} elements.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface are required to be safely accessible from multiple threads
 * concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this interface are not in general <I>synchronization transparent</I>,
 * because they are explicitly wait for each other. So, extra care needs to be taken
 * when synchronizing them, considering the actual implementation.
 *
 * @param <T> the type of the elements of the queue
 *
 * @see TerminableQueues TerminableQueues
 */
public interface TerminableQueue<T> {
    /**
     * Adds an element to this queue waiting if necessary. This method will wait
     * until any of the following conditions are met:
     * <ul>
     *  <li>The given element is added to this queue.</li>
     *  <li>
     *   The given {@code cancelToken} signals cancellation before the element could
     *   have been added/
     *  </li>
     *  <li>This queue was shut down.</li>
     * </ul>
     *
     * @param cancelToken the {@code CancellationToken} which is checked if the wait should
     *   be abandoned. It is guaranteed, that if cancellation was requested, then this method
     *   will not wait forever. However, there is no stronger guarantee on the timeliness
     *   of the cancellation. This argument cannot be cannot be {@code null}.
     * @param entry the new element to be added to this queue. This argument cannot be
     *   {@code null}.
     *
     * @throws TerminatedQueueException thrown if adding the element was canceled, because
     *   this queue was shut down. If this exception is thrown, then it is guaranteed that the
     *   entry was not added to this queue.
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation request
     *   was detected before the element could have been added to this queue. If this exception
     *   is thrown, then it is guaranteed that the entry was not added to this queue. Note however
     *   that there is no guarantee that this exception is thrown even if the token was canceled before
     *   calling this method.
     */
    public void put(CancellationToken cancelToken, T entry)
            throws TerminatedQueueException;

    /**
     * Adds an element to this queue waiting if necessary. This method will wait
     * until any of the following conditions are met:
     * <ul>
     *  <li>The given element is added to this queue.</li>
     *  <li>
     *   The given timeout was reached before the element could have been added.
     *  </li>
     *  <li>
     *   The given {@code cancelToken} signals cancellation before the element could
     *   have been added.
     *  </li>
     *  <li>This queue was shut down.</li>
     * </ul>
     * <P>
     * Note: If the element can be added right away without waiting, then it will be added
     * regardless how low the timeout is. That is, there is no risk of failing due to timeout
     * compared to the {@link #offer(Object) offer} method.
     *
     * @param cancelToken the {@code CancellationToken} which is checked if the wait should
     *   be abandoned. It is guaranteed, that if cancellation was requested, then this method
     *   will not wait forever. However, there is no stronger guarantee on the timeliness
     *   of the cancellation. This argument cannot be cannot be {@code null}.
     * @param entry the new element to be added to this queue. This argument cannot be
     *   {@code null}.
     * @param timeout the maximum time to wait in the given time unit to add the given
     *   element to the queue. A best effort is made to honor the given timeout, but there
     *   is no strong guarantee on the accuracy. This argument must be greater than or
     *   equal to zero.
     * @param timeoutUnit the time unit in which the {@code timeout} argument is to be
     *   interpreted. This argument cannot be {@code null}.
     * @return {@code true} if the element was added to this queue, {@code false} if
     *   the given timeout elapsed before it could have been added.
     *
     * @throws TerminatedQueueException thrown if adding the element was canceled, because
     *   this queue was shut down. If this exception is thrown, then it is guaranteed that the
     *   entry was not added to this queue.
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation request
     *   was detected before the element could have been added to this queue. If this exception
     *   is thrown, then it is guaranteed that the entry was not added to this queue. Note however
     *   that there is no guarantee that this exception is thrown even if the token was canceled before
     *   calling this method.
     */
    public boolean put(CancellationToken cancelToken, T entry, long timeout, TimeUnit timeoutUnit)
            throws TerminatedQueueException;

    /**
     * Adds an element to this queue if possible, or returns {@code false} if the queue
     * does not accept the new element this time. This method never blocks, if it can't
     * add the element, then it returns immediately.
     * <P>
     * This method is <I>synchronization transparent</I>, therefore it can be used in
     * any context safely.
     * <P>
     * <I>Default implementation</I>: The default implementation completely relies on the
     * {@link #put(CancellationToken, Object, long, TimeUnit) put(CancellationToken, T, long, TimeUnit)} method.
     *
     * @param entry the new element to be added to this queue. This argument cannot be
     *   {@code null}.
     * @return {@code true} if the element was successfully added, {@code false} if
     *   it was not added
     *
     * @throws TerminatedQueueException thrown if adding the element was canceled, because
     *   this queue was shut down. If this exception is thrown, then it is guaranteed that the
     *   entry was not added to this queue.
     */
    public default boolean offer(T entry) throws TerminatedQueueException {
        return put(Cancellation.UNCANCELABLE_TOKEN, entry, 0, TimeUnit.NANOSECONDS);
    }

    /**
     * Removes the current head of the queue and returns it waiting the given the time if necessary,
     * but keeps reserving the space for the removed element until the returned element is
     * {@link ReservedElementRef#release() released}; or returns {@code null} if the timeout elapses
     * before any element could have been retrieved. That is, not releasing the returned will make attempting
     * to add new elements to this queue, as if the element was not removed. However, removing a new
     * element will return the next element, and won't return the currently returned element again.
     * <P>
     * This method will wait until any of the following conditions are met:
     * <ul>
     *  <li>The head was removed from this queue.</li>
     *  <li>
     *   The given timeout was reached before the head could have been retrieved.
     *  </li>
     *  <li>
     *   The given {@code cancelToken} signals cancellation before the head could have
     *   been removed.
     *  </li>
     *  <li>This queue was shut down and no new elements will be allowed to be added to this list.</li>
     * </ul>
     * <P>
     * Note: If the head is available right away without waiting, then it will be removed
     * regardless how low the timeout is. That is, there is no risk of failing due to timeout
     * compared to the {@link #tryTakeButKeepReserved() tryTakeButKeepReserved()} method.
     *
     * @param cancelToken the {@code CancellationToken} which is checked if the wait should
     *   be abandoned. It is guaranteed, that if cancellation was requested, then this method
     *   will not wait forever. However, there is no stronger guarantee on the timeliness
     *   of the cancellation. This argument cannot be cannot be {@code null}.
     * @param timeout the maximum time to wait in the given time unit to remove the head of
     *   the queue. A best effort is made to honor the given timeout, but there is no strong
     *   guarantee on the accuracy. This argument must be greater than or equal to zero.
     * @param timeoutUnit the time unit in which the {@code timeout} argument is to be
     *   interpreted. This argument cannot be {@code null}.
     * @return the reference to the now removed head of the queue, or {@code null} if the timeout elapsed
     *   before any element could have been removed
     *
     * @throws TerminatedQueueException thrown if this queue was shut down, and is empty. Throwing this
     *   exception guarantees that this queue is empty, and that it will stay empty forever. Note that this queue
     *   is empty only in the sense that no more elements can be removed from it. That is, there is no guarantee
     *   that all elements were already {@link ReservedElementRef#release() released}.
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation request
     *   was detected before an element could have been removed from this queue. If this exception
     *   is thrown, then it is guaranteed that this queue was not modified by this method. Note however that there
     *   is no guarantee that this exception is thrown even if the token was canceled before calling this method.
     */
    public ReservedElementRef<T> tryTakeButKeepReserved(
            CancellationToken cancelToken,
            long timeout,
            TimeUnit timeoutUnit) throws TerminatedQueueException;

    /**
     * Removes the current head of the queue and returns it, but keeps reserving the space
     * for the removed element until the returned element is {@link ReservedElementRef#release() released};
     * or returns {@code null} if the queue is currently empty. That is, not releasing the
     * returned will make attempting to add new elements to this queue, as if the element was not removed.
     * However, removing a new element will return the next element, and won't return the currently
     * returned element again.
     * <P>
     * This method never blocks, and if the queue is currently empty, this method will returns immediately.
     * <P>
     * This method is <I>synchronization transparent</I>, therefore it can be used in
     * any context safely.
     * <P>
     * <I>Default implementation</I>: The default implementation completely relies on the
     * {@link #tryTakeButKeepReserved(CancellationToken, long, TimeUnit) tryTakeButKeepReserved(CancellationToken, long, TimeUnit)}
     * method.
     *
     * @return the reference to head of the queue, or {@code null} if this queue was empty
     *
     * @throws TerminatedQueueException thrown if this queue was shut down, and is empty. Throwing this
     *   exception guarantees that this queue is empty, and that it will stay empty forever. Note that this queue
     *   is empty only in the sense that no more elements can be removed from it. That is, there is no guarantee
     *   that all elements were already {@link ReservedElementRef#release() released}.
     */
    public default ReservedElementRef<T> tryTakeButKeepReserved() throws TerminatedQueueException {
        return tryTakeButKeepReserved(Cancellation.UNCANCELABLE_TOKEN, 0, TimeUnit.NANOSECONDS);
    }

    /**
     * Removes the current head of the queue and returns it, or returns {@code null} if the queue is currently empty.
     * This method method immediately releases the space the returned element takes up in the queue.
     * <P>
     * This method never blocks, and if the queue is currently empty, this method will returns immediately.
     * <P>
     * This method is <I>synchronization transparent</I>, therefore it can be used in
     * any context safely.
     * <P>
     * <I>Default implementation</I>: The default implementation completely relies on the
     * {@link #tryTakeButKeepReserved() tryTakeButKeepReserved()} method.
     *
     * @return the head element removed from this queue, or {@code null} if this queue was empty
     *
     * @throws TerminatedQueueException thrown if this queue was shut down, and is empty. Throwing this
     *   exception guarantees that this queue is empty, and that it will stay empty forever. Note that this queue
     *   is empty only in the sense that no more elements can be removed from it. That is, there is no guarantee
     *   that all elements were already {@link ReservedElementRef#release() released}.
     */
    public default T tryTake() throws TerminatedQueueException {
        ReservedElementRef<T> ref = tryTakeButKeepReserved();
        if (ref == null) {
            return null;
        }

        ref.release();
        return ref.element();
    }

    /**
     * Removes the current head of the queue and returns it waiting the given timeout if the queue is currently empty.
     * If the queue is empty even after the specified timeout elapses, this method will return {@code null}.
     * This method method immediately releases the space the returned element takes up in the queue.
     * <P>
     * This method will wait until any of the following conditions are met:
     * <ul>
     *  <li>The head was removed from this queue.</li>
     *  <li>
     *   The given timeout was reached before the head could have been retrieved.
     *  </li>
     *  <li>
     *   The given {@code cancelToken} signals cancellation before the head could have
     *   been removed.
     *  </li>
     *  <li>This queue was shut down and no new elements will be allowed to be added to this list.</li>
     * </ul>
     * <P>
     * <I>Default implementation</I>: The default implementation completely relies on the
     * {@link #tryTakeButKeepReserved(CancellationToken, long, TimeUnit) tryTakeButKeepReserved(CancellationToken, long, TimeUnit)}
     * method.
     *
     * @param cancelToken the {@code CancellationToken} which is checked if the wait should
     *   be abandoned. It is guaranteed, that if cancellation was requested, then this method
     *   will not wait forever. However, there is no stronger guarantee on the timeliness
     *   of the cancellation. This argument cannot be cannot be {@code null}.
     * @param timeout the maximum time to wait in the given time unit to remove the head of
     *   the queue. A best effort is made to honor the given timeout, but there is no strong
     *   guarantee on the accuracy. This argument must be greater than or equal to zero.
     * @param timeoutUnit the time unit in which the {@code timeout} argument is to be
     *   interpreted. This argument cannot be {@code null}.
     * @return the head element removed from this queue, or {@code null} if this queue was empty and the given
     *   timeout elapsed
     *
     * @throws TerminatedQueueException thrown if this queue was shut down, and is empty. Throwing this
     *   exception guarantees that this queue is empty, and that it will stay empty forever. Note that this queue
     *   is empty only in the sense that no more elements can be removed from it. That is, there is no guarantee
     *   that all elements were already {@link ReservedElementRef#release() released}.
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation request
     *   was detected before an element could have been removed from this queue. If this exception
     *   is thrown, then it is guaranteed that this queue was not modified by this method. Note however that there
     *   is no guarantee that this exception is thrown even if the token was canceled before calling this method.
     */
    public default T tryTake(CancellationToken cancelToken, long timeout, TimeUnit timeoutUnit)
            throws TerminatedQueueException {

        ReservedElementRef<T> ref = tryTakeButKeepReserved(cancelToken, timeout, timeoutUnit);
        if (ref == null) {
            return null;
        }

        ref.release();
        return ref.element();
    }

    /**
     * Removes the current head of the queue and returns it waits until it becomes available if the queue is
     * currently empty, but keeps reserving the space for the removed element until the returned element is
     * {@link ReservedElementRef#release() released}. That is, not releasing the returned will make attempting
     * to add new elements to this queue, as if the element was not removed. However, removing a new
     * element will return the next element, and won't return the currently returned element again.
     * <P>
     * This method will wait until any of the following conditions are met:
     * <ul>
     *  <li>The head was removed from this queue.</li>
     *  <li>
     *   The given {@code cancelToken} signals cancellation before the head could have
     *   been removed.
     *  </li>
     *  <li>This queue was shut down and no new elements will be allowed to be added to this list.</li>
     * </ul>
     * <P>
     * <I>Default implementation</I>: The default implementation completely relies on the
     * {@link #tryTakeButKeepReserved(CancellationToken, long, TimeUnit) tryTakeButKeepReserved(CancellationToken, long, TimeUnit)}
     * method. That is, the default implementation loop on {@code tryTakeButKeepReserved}, until it returns
     * a non-null object.
     *
     * @param cancelToken the {@code CancellationToken} which is checked if the wait should
     *   be abandoned. It is guaranteed, that if cancellation was requested, then this method
     *   will not wait forever. However, there is no stronger guarantee on the timeliness
     *   of the cancellation. This argument cannot be cannot be {@code null}.
     * @return the reference to the now removed head of the queue. This method never returns {@code null}.
     *
     * @throws TerminatedQueueException thrown if this queue was shut down, and is empty. Throwing this
     *   exception guarantees that this queue is empty, and that it will stay empty forever. Note that this queue
     *   is empty only in the sense that no more elements can be removed from it. That is, there is no guarantee
     *   that all elements were already {@link ReservedElementRef#release() released}.
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation request
     *   was detected before an element could have been removed from this queue. If this exception
     *   is thrown, then it is guaranteed that this queue was not modified by this method. Note however that there
     *   is no guarantee that this exception is thrown even if the token was canceled before calling this method.
     */
    public default ReservedElementRef<T> takeButKeepReserved(CancellationToken cancelToken)
            throws TerminatedQueueException {

        ReservedElementRef<T> result;
        do {
            result = tryTakeButKeepReserved(cancelToken, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } while (result == null);
        return result;
    }

    /**
     * Removes the current head of the queue and returns it waits until it becomes available if the queue is
     * currently empty. This method method immediately releases the space the returned element takes up in the queue.
     * <P>
     * This method will wait until any of the following conditions are met:
     * <ul>
     *  <li>The head was removed from this queue.</li>
     *  <li>
     *   The given {@code cancelToken} signals cancellation before the head could have
     *   been removed.
     *  </li>
     *  <li>This queue was shut down and no new elements will be allowed to be added to this list.</li>
     * </ul>
     * <P>
     * <I>Default implementation</I>: The default implementation completely relies on the
     * {@link #takeButKeepReserved(CancellationToken) takeButKeepReserved(CancellationToken)}
     * method.
     *
     * @param cancelToken the {@code CancellationToken} which is checked if the wait should
     *   be abandoned. It is guaranteed, that if cancellation was requested, then this method
     *   will not wait forever. However, there is no stronger guarantee on the timeliness
     *   of the cancellation. This argument cannot be cannot be {@code null}.
     * @return the head element removed from this queue. This method never returns {@code null}.
     *
     * @throws TerminatedQueueException thrown if this queue was shut down, and is empty. Throwing this
     *   exception guarantees that this queue is empty, and that it will stay empty forever. Note that this queue
     *   is empty only in the sense that no more elements can be removed from it. That is, there is no guarantee
     *   that all elements were already {@link ReservedElementRef#release() released}.
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation request
     *   was detected before an element could have been removed from this queue. If this exception
     *   is thrown, then it is guaranteed that this queue was not modified by this method. Note however that there
     *   is no guarantee that this exception is thrown even if the token was canceled before calling this method.
     */
    public default T take(CancellationToken cancelToken) throws TerminatedQueueException {
        ReservedElementRef<T> ref = takeButKeepReserved(cancelToken);
        ref.release();
        return ref.element();
    }

    /**
     * Removes all the elements from this queue. Note that reservations might still remain after this call.
     * <P>
     * <B>Note</B>: If for all element addition <I>happens-before</I> calling this {@code clear} method, then
     * it is guaranteed that the queue is empty after the call. However, you can't have guarantee about the
     * state of the queue.
     * <P>
     * The default implementation calls {@link #tryTake() tryTake()} until it returns {@code null}
     * or throws a {@code TerminatedQueueException}.
     */
    public default void clear() {
        try {
            while (tryTake() != null) {
                // Do nothing, just keep draining.
            }
        } catch (TerminatedQueueException ex) {
            // Expected if the queue was shutdown already.
        }
    }

    /**
     * Prevents new elements to be added to this queue, but keeps the already added elements.
     * If {@code shutdown} <I>happens-before</I> an element addition method to this queue, then
     * adding the element is guaranteed to fail with a {@code TerminatedQueueException}. If there is
     * no <I>happens-before</I> relationship between the {@code shutdown} call and the element addition call,
     * then the element is either added to this queue or a {@code TerminatedQueueException} is thrown, but not both.
     * <P>
     * Note however, that already added element can still be removed from the queue as if this method has
     * not been called until this queue becomes empty. Once this queue is empty, element removals will also
     * keep failing with a {@code TerminatedQueueException}, meaning that the queue will remain entry from there on.
     * In another words: If a {@code TerminatedQueueException} thrown by a method of this queue
     * <I>happens-before</I> an attempt at removing or adding an element to this queue, then it is guaranteed
     * that the attempted method call will also throw a {@code TerminatedQueueException}.
     * <P>
     * This method is idempotent. That is, calling it multiple times have the same effect as calling it once.
     */
    public void shutdown();

    /**
     * Prevents new elements to be added to this queue, and waits until no more elements are remaining
     * in this queue. This method waits until the elements are not just removed, but also
     * {@link ReservedElementRef#release() released}. That is, this method call is not equivalent to
     * calling {@link #shutdown() shutdown} and then looping on {@link #take(CancellationToken) take} until
     * a {@code TerminatedQueueException} is thrown, because this method also waits for references to be
     * released, while {@code TerminatedQueueException} is thrown when this queue will not have any more
     * elements to be removed.
     * <P>
     * If this method returns normally, then it is guaranteed that a second attempt to call this method
     * will return immediately.
     * <P>
     * Note: It is explicitly allowed to call {@link #shutdown() shutdown} before this method.
     *
     * @param cancelToken the {@code CancellationToken} which is checked if the wait should
     *   be abandoned. It is guaranteed, that if cancellation was requested, then this method
     *   will not wait forever. However, there is no stronger guarantee on the timeliness
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation request
     *   was detected. Note however that there is no guarantee that this exception is thrown
     *   even if the token was canceled before calling this method.
     *
     * @see #shutdown() shutdown
     */
    public void shutdownAndWaitUntilEmpty(CancellationToken cancelToken);

    /**
     * Prevents new elements to be added to this queue, and waits until no more elements are remaining
     * in this queue or the given timeout expires. This method waits until the elements are not just removed,
     * but also {@link ReservedElementRef#release() released}. That is, this method call is not equivalent to
     * calling {@link #shutdown() shutdown} and then looping on {@link #take(CancellationToken) take} until
     * a {@code TerminatedQueueException} is thrown, because this method also waits for references to be
     * released, while {@code TerminatedQueueException} is thrown when this queue will not have any more
     * elements to be removed.
     * <P>
     * If this method returns {@code true}, then it is guaranteed that a second attempt to call this method
     * will return immediately (returning {@code true}.
     * <P>
     * Note: It is explicitly allowed to call {@link #shutdown() shutdown} before this method.
     *
     * @param cancelToken the {@code CancellationToken} which is checked if the wait should
     *   be abandoned. It is guaranteed, that if cancellation was requested, then this method
     *   will not wait forever. However, there is no stronger guarantee on the timeliness
     * @param timeout the maximum time to wait in the given time unit to wait for this queue to become empty.
     *   A best effort is made to honor the given timeout, but there is no strong guarantee on the accuracy.
     *   This argument must be greater than or equal to zero.
     * @param timeoutUnit the time unit in which the {@code timeout} argument is to be
     *   interpreted. This argument cannot be {@code null}.
     * @return {@code true} if this queue is now empty, {@code false} if
     *   the given timeout elapsed before this queue was detected to become empty.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if cancellation request
     *   was detected. Note however that there is no guarantee that this exception is thrown
     *   even if the token was canceled before calling this method.
     *
     * @see #shutdown() shutdown
     */
    public boolean shutdownAndTryWaitUntilEmpty(CancellationToken cancelToken, long timeout, TimeUnit timeoutUnit);
}
