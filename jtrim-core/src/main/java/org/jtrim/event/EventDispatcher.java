package org.jtrim.event;

/**
 * Defines an interface to invoke specific kinds of event handlers.
 * <P>
 * This interface is used as a generic wrapper for all kinds of events, so that
 * {@link ListenerManager} can invoke them. They should do little more
 * than invoking the event handler method of the listener with the appropriate
 * arguments.
 * <P>
 * The {@link #onEvent(Object, Object) onEvent} method accepts an arbitrary
 * argument, therefore it is enough to define a single {@code EventDispatcher}
 * instance for each kind of event handler method. So it is recommended, to
 * define a single static {@code EventDispatcher} instance for every such
 * methods for better performance.
 *
 * <h3>Thread safety</h3>
 * Implementations should maintain the thread-safety property of the event
 * handler method they notify.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations should maintain the synchronization transparency property of
 * the event handler method they notify.
 *
 * @param <EventListenerType> the type of the listeners whose event handler
 *   method is to be invoked by the {@link #onEvent(Object, Object) onEvent}
 *   method
 * @param <ArgType> the type of the argument to be passed to the
 *   {@link #onEvent(Object, Object) onEvent} method. Since event handlers
 *   typically does not have more than one argument, this can be the same
 *   type as the one passed to the actual event handler method. In case, the
 *   event handler does not need any kind of arguments, this type is recommended
 *   to be the type {@link Void}.
 *
 * @see ListenerManager
 *
 * @author Kelemen Attila
 */
public interface EventDispatcher<EventListenerType, ArgType> {
    /**
     * Invokes the event handler method of the specified listener with the
     * appropriate arguments. The event handler must be called synchronously
     * on the current calling thread.
     * <P>
     * An argument can be specified for the event to be dispatched. This
     * argument is typically the same argument as the one required by the
     * event listener.
     *
     * @param eventListener the listener whose event handler method is to be
     *   called by this method. This argument cannot be {@code null}.
     * @param arg an argument for the current event to be dispatched. The exact
     *   meaning of this argument depends on the event handler to be invoked.
     *   This argument may be {@code null}, if the listeners supports
     *   {@code null} arguments.
     *
     * @throws NullPointerException implementations may choose to throw this
     *   exception if the specified listener is {@code null}
     */
    public void onEvent(EventListenerType eventListener, ArgType arg);
}
