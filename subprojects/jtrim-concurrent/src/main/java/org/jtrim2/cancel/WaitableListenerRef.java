package org.jtrim2.cancel;

import org.jtrim2.event.ListenerRef;

/**
 * Defines a {@code ListenerRef} which can be used to unregister the associated
 * listener and wait until it can be ensured that the listener is not going to
 * be executed anymore.
 *
 * <h2>Thread safety</h2>
 * The methods of this interface must be safe to be accessed by multiple threads
 * concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The {@code unregisterAndWait} method does not need to be
 * <I>synchronization transparent</I> but methods inherited from
 * {@code ListenerRef} are required to be <I>synchronization transparent</I>.
 *
 * @see Cancellation#listenForCancellation(CancellationToken, Runnable)
 */
public interface WaitableListenerRef extends ListenerRef {
    /**
     * Unregisters the associated listener (as the {@link #unregister() unregister}
     * method and waits until it can be ensured that the listener is not going
     * to be notified anymore.
     * <B>Warning</B>: It is forbidden to call any of the {@code close} methods
     * from within the associated listener.
     * <P>
     * If you make a valid call to this method (valid arguments and in valid
     * state), the listener can be considered unregistered as if
     * {@code unregister()} has been called.
     * <P>
     * If this method returns normally without throwing an exception, then
     * subsequent call to this method must succeed immediately without throwing
     * an exception (not even {@code OperationCanceledException}.
     *
     * @param cancelToken the {@code CancellationToken} which may signal that
     *   this method is to return immediately without waiting throwing an
     *   {@code OperationCanceledException}. This argument cannot be
     *   {@code null}.
     *
     * @throws OperationCanceledException thrown if this method has been
     *   canceled due to being requested through the specified
     *   {@code CancellationToken}
     * @throws NullPointerException implementations may throw this exception
     *   if the specified {@code CancellationToken} is {@code null}
     */
    public void unregisterAndWait(CancellationToken cancelToken);
}
