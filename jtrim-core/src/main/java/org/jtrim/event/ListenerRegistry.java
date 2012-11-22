package org.jtrim.event;

/**
 * Defines a collection of event listeners which can be notified of events in an
 * implementation dependent way. It is possible to
 * {@link #registerListener(Object) add} and
 * {@link ListenerRef#unregister() remove} listeners to and from the collection
 * concurrently anytime.
 * <P>
 * This interface defines similar functionality to the class
 * {@code java.awt.AWTEventMulticaster} but allows for more efficient
 * implementations and also more generic (can be used with any kind of
 * listeners, not just AWT). The most notable difference is the way of removing
 * registered listeners: While with usual AWT listeners one must invoke a
 * {@code removeXXX} method with the registered listener instance, this
 * interface returns a reference when registering a listener and this reference
 * can be used to remove the registered listener. The method provided is safer
 * to use and usually more convenient, since lots of different reference of
 * different listener registrations can be grouped and be removed in a single
 * loop rather than having a distinct way of removing every different listener.
 * <P>
 * Notice that this interface does not define how events can be triggered, for
 * typical event triggering mechanism see the {@link ListenerManager} interface.
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
 *
 * @author Kelemen Attila
 */
public interface ListenerRegistry<ListenerType> {
    /**
     * Adds an event listener to this container and returns a reference which
     * can later be used to removed the listener added.
     * <P>
     * Subsequent event notifications will notify the added listener unless it
     * is removed using the returned reference.
     * <P>
     * It doesn't matter if the listener was already added. In case the given
     * listener have been added to this {@code ListenerRegistry} it will be
     * added again causing the added listener to be notified as many times as
     * it has been added. Note that the {@code equals} method of the listener
     * should have no effect regarding this {@code ListenerRegistry}.
     * <P>
     * If a listener is removed but it was added multiple times, it will be
     * removed only once. So, further event notifications will be delivered to
     * the listener one less time.
     *
     * @param listener the listener to be added to this container and be
     *   notified in subsequent event notifications method calls. This argument
     *   cannot be {@code null}.
     * @return the reference which can be used to remove the currently added
     *   listener. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
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
