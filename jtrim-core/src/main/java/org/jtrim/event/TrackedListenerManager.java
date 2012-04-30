package org.jtrim.event;

/**
 * Defines a {@link ListenerRegistry} which allows events to be dispatched to
 * registered listeners directly and the {@code TrackedListenerManager} will
 * supply the causes of the events for the listener.
 * <P>
 * This interface is similar to {@link ListenerManager} but will also specify
 * the cause of the event for the registered listeners. In the usual case, an
 * {@link EventTracker} keeps track of the causes of the events and provides the
 * {@code TrackedListenerManager} instances.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The {@code onEvent} method of this interface does not required to be
 * <I>synchronization transparent</I> but methods inherited from
 * {@link ListenerRegistry} are required to be
 * <I>synchronization transparent</I> as specified by {@code ListenerRegistry}.
 *
 * @param <ArgType> the type of the event argument which is to be passed to
 *   the {@link #onEvent(Object) onEvent} method
 *
 * @see EventTracker
 * @see ListenerManager
 * @see TrackedEvent
 * @see TrackedEventListener
 *
 * @author Kelemen Attila
 */
public interface TrackedListenerManager<ArgType>
extends
        ListenerRegistry<TrackedEventListener<ArgType>> {

    /**
     * Invokes the {@link TrackedEventListener#onEvent(TrackedEvent) onEvent}
     * method of the registered {@link TrackedEventListener} instances. The
     * {@code onEvent} method is called synchronously in the current thread.
     * <P>
     * Adding new listeners to this container will have no effect on the
     * current call and the listeners being notified. That is, if a notified
     * listener adds a new listener to this container, the newly added listener
     * will not be notified in this call, only in subsequent {@code onEvent}
     * calls.
     * <P>
     * The order in which the listener are notified is undefined. Also note,
     * that multiply added listener might be notified multiple times depending
     * on the exact implementation.
     * <P>
     * Note that usually it is required that event listeners be invoked in the
     * order the events occur. This can easily be done when events can only
     * occur on single thread (as they do in Swing). However, when events can
     * occur on multiple threads concurrently they are likely to occur while
     * holding a lock and listeners should be notified right away to be notified
     * of the event to keep the order of events. Note that calling an event
     * handler method while holding a lock is prone to dead-lock (and it is very
     * likely to be possible since it is hard to reason about what listeners
     * might do). In this case you should consider using a
     * {@link org.jtrim.concurrent.TaskScheduler TaskScheduler} to invoke the
     * {@code onEvent} method.
     *
     * @param arg the event specific argument to be passed to the registered
     *   listeners. The causes will be supplied by this
     *   {@code TrackedListenerManager}. This argument can be {@code null}, if
     *   the listeners accept {@code null} arguments for this event.
     */
    public void onEvent(ArgType arg);
}
