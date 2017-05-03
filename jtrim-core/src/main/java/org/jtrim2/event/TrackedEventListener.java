package org.jtrim2.event;

/**
 * Defines an event listener for {@link TrackedListenerManager} instances of an
 * {@link EventTracker}. This listener interface accepts the causes of the
 * events and an additional event specific argument. Although this listener
 * interface was designed to be used by {@code EventTracker} instances, the
 * general use of this listener encouraged where a listener of events is
 * required.
 *
 * <h3>Thread safety</h3>
 * Listeners are not generally required to be safe to be accessed by multiple
 * threads concurrently. The source of events defines the required thread-safety
 * property for the listener instances.
 *
 * <h4>Synchronization transparency</h4>
 * Listeners are not required to be <I>synchronization transparent</I>. Note
 * however, that usually listeners should only do computationally cheap
 * operations and should also not wait for external events to occur or for other
 * threads. This is because usually multiple listeners needed to be notified
 * and a slow listener might hurt the responsiveness of an application.
 *
 * @param <ArgType> the type of the event specific argument
 *
 * @see EventTracker
 * @see TrackedEvent
 * @see TrackedListenerManager
 *
 * @author Kelemen Attila
 */
public interface TrackedEventListener<ArgType> {
    /**
     * This method is called when the event of which this listener had been
     * registered to be notified, occurs.
     * <P>
     * It is highly recommended to write listeners to be computationally cheap,
     * so it may not hurt the responsiveness of an application.
     *
     * @param trackedEvent the causes of the events and the event specific
     *   argument. This argument cannot be {@code null}.
     */
    public void onEvent(TrackedEvent<ArgType> trackedEvent);
}
