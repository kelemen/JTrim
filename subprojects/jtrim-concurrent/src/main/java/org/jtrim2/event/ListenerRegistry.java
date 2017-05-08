package org.jtrim2.event;

/**
 * Defines a collection of event listeners which can be notified of events in an
 * implementation dependent way. This interface extends
 * {@link SimpleListenerRegistry} allowing to return the number of currently
 * registered listeners. Since the number of added listeners is rarely needed,
 * you should consider relying on the {@code SimpleListenerRegistry} interface
 * because it is not always simple to return the number of registered listeners.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of the methods of this interface must be
 * <I>synchronization transparent</I>.
 *
 * @param <ListenerType> the type of the event listeners can possibly be added
 *   to the container
 *
 * @see ListenerManager
 * @see CopyOnTriggerListenerManager
 */
public interface ListenerRegistry<ListenerType>
extends
        SimpleListenerRegistry<ListenerType> {
    /**
     * {@inheritDoc }
     */
    @Override
    public ListenerRef registerListener(ListenerType listener);

    /**
     * Returns the number of currently {@link #registerListener(Object) added}
     * but not yet {@link ListenerRef#unregister() removed} listeners.
     *
     * @return the number of currently {@link #registerListener(Object) added}
     *   but not yet {@link ListenerRef#unregister() removed} listeners. This
     *   method always returns an integer greater than or equal to zero.
     */
    public int getListenerCount();
}
