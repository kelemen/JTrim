/**
 * Contains classes and interfaces for dispatching and handling event
 * notifications.
 *
 * <h3>Listener Managers</h3>
 * {@link org.jtrim.event.ListenerManager Listener managers} are
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
 * <h3>Event trackers</h3>
 * {@link org.jtrim.event.EventTracker Event trackers} help keep tracking of
 * what event was cause by what event. This enables to detect recursive
 * invocation of events and makes it possible to avoid infinite loops.
 *
 * @see org.jtrim.event.EventTracker
 * @see org.jtrim.event.EventListeners
 * @see org.jtrim.event.ListenerManager
 */
package org.jtrim.event;
