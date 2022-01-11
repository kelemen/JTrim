/**
 * Contains classes and interfaces for dispatching and handling event
 * notifications.
 *
 * <h2>Listener Managers</h2>
 * {@link org.jtrim2.event.ListenerManager Listener managers} are
 * designed to store listeners which are to be notified later when the actual
 * event occurs. This container considerably eases the developer's work when
 * event dispatching is required.
 * <P>
 * Note that removing an added listener is different than removing a listener
 * using the usual idiom in Swing (i.e.: invoking a {@code removeXXX} method
 * with the same listener). {@code ListenerManager} implementations return
 * a reference to the added listener, which can be used to remove the listener.
 * This is more flexible way to remove listeners and allows for better
 * implementations.
 *
 * @see org.jtrim2.event.EventListeners
 * @see org.jtrim2.event.ListenerManager
 */
package org.jtrim2.event;
