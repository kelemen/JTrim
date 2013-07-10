package org.jtrim.event;

/**
 * Defines a reference of an event handler which has been registered to be
 * notified of a certain event. The event handler can be
 * {@link #unregister() unregistered} once no longer needed to be notified of
 * the event. Once a listener has been unregistered, there is no way to register
 * it again through this interface. That is, it should be registered as it was
 * done previously.
 * <P>
 * There are some cases when you want to unregister a listener in the code of
 * the listener itself. In this case use the {@link InitLaterListenerRef} class.
 * <P>
 * Listeners of this reference are usually added to an
 * {@link ListenerRegistry}. See its documentation for further details.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are required to be
 * <I>synchronization transparent</I>.
 *
 * @see InitLaterListenerRef
 * @see ListenerRegistry
 *
 * @author Kelemen Attila
 */
public interface ListenerRef {
    /**
     * Checks whether the listener is currently registered to receive
     * notifications of events.
     * <P>
     * In case this method returns {@code true}, the method
     * {@link #unregister() unregister()} can be called to stop the listener
     * from receiving notifications of the events occurring. If this method
     * returns {@code false}, the {@code unregister()} method has no useful
     * effect and listener notifications can be expected to stop at some time
     * in the future. Note that even if this method returns {@code false}, some
     * pending event notifications might be forwarded.
     *
     * @return {@code true} if the listener is currently registered to be
     *   notified of occurring events, {@code false} if the listener was
     *   unregistered, so calling {@code unregister} is no longer necessary
     */
    public boolean isRegistered();

    /**
     * Unregisters the listener, so it does not need to be notified of
     * subsequent events. Calling this method ensures that there will be a point
     * in time in the future from when notifications of new events will no
     * longer be forwarded. That is, it is possible that some subsequent events
     * will still be forwarded to the associated listener but forwarding of
     * these events will eventually stop without further interaction.
     * <P>
     * This method must be idempotent. That is, invoking it multiple times has
     * the same effect as invoking it only once.
     */
    public void unregister();
}
