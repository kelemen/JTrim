package org.jtrim.event;

import java.util.Collection;

/**
 * Defines a collection of event listeners and allows to dispatch events to all
 * the added listeners. It is possible to {@link #registerListener(Object) add}
 * and {@link ListenerRef#unregister() remove} listeners to and from the
 * collection concurrently anytime.
 * <P>
 * This interface defines similar functionality to the class
 * {@link java.awt.AWTEventMulticaster} but allows for more efficient
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
 * Here is a simple usage of this interface assuming {@code Runnable} to be a
 * listener:
 * <code><pre>
 * EventHandlerContainer&lt;Runnable, Void&gt; listeners = ...;
 * listeners.registerListener(listener1);
 * listeners.registerListener(listener2);
 *
 * // ...
 *
 * // Sometimes later, dispatch the event to "listener1" and "listener2"
 * listeners.onEvent(new EventDispatcher&lt;Runnable, Void&gt;() {
 *   public void onEvent(Runnable eventListener, Void arg) {
 *     eventListener.run();
 *   }
 * }, null);
 * </pre></code>
 * The above code will call the {@code run} method of both {@code listener1} and
 * {@code listener2} in the {@code onEvent} method. Notice that the
 * {@code EventDispatcher} is always the same and as such a single static
 * instance can be created for better performance.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Except for the {@code onEvent} method (which must call listeners),
 * implementations of the methods of this interface must be
 * <I>synchronization transparent</I>.
 *
 * @param <ListenerType> the type of the event handlers can possibly be added
 *   to the container
 * @param <ArgType> the type of the argument which can be passed to event
 *   handlers by the {@code onEvent} method
 *
 * @see CopyOnTriggerEventHandlerContainer
 *
 * @author Kelemen Attila
 */
public interface EventHandlerContainer<ListenerType, ArgType> {
    /**
     * Adds an event handler to this container and returns a reference which
     * can later be used to removed the listener added.
     * <P>
     * Subsequent calls to the {@link #onEvent(EventDispatcher, Object) onEvent}
     * method will notify the added listener unless it is removed using the
     * returned reference.
     * <P>
     * In case the same listener was already added to this container, there are
     * two ways an implementation is allowed to act:
     * <ul>
     *  <li>
     *   Add the listener again, so it will be called multiple times by the
     *   {@code onEvent} method. Removing this newly added listener will only
     *   remove a single listener from the container.
     *  </li>
     *  <li>
     *   Do not add the listener again, so the listener will be called only once
     *   by the {@code onEvent} method. The returned reference to the listener
     *   will appear to be removed. That is, the
     *   {@link ListenerRef#isRegistered()} method of the returned reference
     *   will return {@code false}.
     *  </li>
     * </ul>
     * Note that it is not recommended to add the same listener multiple times
     * to an {@code EventHandlerContainer}.
     *
     * @param listener the listener to be added to this container and be
     *   notified in the {@code onEvent} method calls. This argument cannot be
     *   {@code null}.
     * @return the reference which can be used to remove the currently added
     *   listener. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public ListenerRef<ListenerType> registerListener(ListenerType listener);

    /**
     * Returns the collection of listeners
     * {@link #registerListener(Object) added} but not yet
     * {@link ListenerRef#unregister() removed} in no particular order.
     * <P>
     * The returned collection must be a snapshot of the currently
     * added listeners. That is, listeners subsequently added to this
     * {@code EventHandlerContainer} must have no effect on the previously
     * returned listener collection. The returned collection is may or may not
     * be read-only but if it can be modified, modifications to the returned
     * collection must have no effect on this {@code EventHandlerContainer}.
     *
     * @return the collection of listeners
     *   {@link #registerListener(Object) added} but not yet
     *   {@link ListenerRef#unregister() removed} in no particular order. This
     *   method never returns {@code null} and the returned collection can be
     *   read-only.
     */
    public Collection<ListenerType> getListeners();

    /**
     * Returns the number of currently {@link #registerListener(Object) added}
     * but not yet {@link ListenerRef#unregister() removed} listeners. This
     * method is effectively the same as calling {@code getListeners().size()}
     * but likely to be more efficient.
     *
     * @return the number of currently {@link #registerListener(Object) added}
     *   but not yet {@link ListenerRef#unregister() removed} listeners. This
     *   method always returns an integer greater than or equal to zero.
     */
    public int getListenerCount();

    /**
     * Invokes the {@link EventDispatcher#onEvent(Object, Object) onEvent}
     * method of the specified {@code EventDispatcher} with the currently
     * registered listeners and the argument specified. The {@code onEvent}
     * method is called synchronously in the current thread.
     * <P>
     * Adding new listeners to this container will have no effect on the
     * current call they are being notified. That is, if a notified listener
     * adds a new listener to this container, the newly added listener will not
     * be notified in this call, only in subsequent {@code onEvent} calls.
     * <P>
     * The order in which the listener are notified is undefined. Also note,
     * that multiply added listener might be notified multiple times depending
     * on the exact implementation.
     *
     * @param eventDispatcher the {@code EventDispatcher} whose {@code onEvent}
     *   method is to be called for every registered listener with the specified
     *   argument. The {@code onEvent} method will be called as many times as
     *   many currently registered listeners are (i.e.: the number the
     *   {@link #getListenerCount() getListenerCount()} method returns). This
     *   argument cannot be {@code null}.
     * @param arg the argument to be passed to every invocation of the
     *   {@code onEvent} method of the specified {@code EventDispatcher}. This
     *   argument can be {@code null} if the {@code EventDispatcher} allows for
     *   {@code null} arguments.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code EventDispatcher} is {@code null}
     */
    public void onEvent(
            EventDispatcher<? super ListenerType, ? super ArgType> eventDispatcher,
            ArgType arg);
}
