package org.jtrim2.event;

/**
 * Defines a {@link ListenerRegistry} which allows events to be dispatched to
 * registered listeners directly.
 * <P>
 * The listeners can be notified synchronously on any thread and also be
 * provided with a single argument.
 * <P>
 * Here is a simple usage of this interface assuming {@code Runnable} to be a
 * listener:
 * <pre>{@code
 * ListenerManager<Runnable, Void> listeners = ...;
 * listeners.registerListener(listener1);
 * listeners.registerListener(listener2);
 *
 * // ...
 *
 * // Sometimes later, dispatch the event to "listener1" and "listener2"
 * listeners.onEvent((Runnable eventListener, Void arg) -> {
 *   eventListener.run();
 * }, null);
 * }</pre>
 * The above code will call the {@code run} method of both {@code listener1} and
 * {@code listener2} in the {@code onEvent} method. Notice that the
 * {@code EventDispatcher} is always the same and as such a single static
 * instance can be created for better performance.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The {@code onEvent} method of this interface does not required to be
 * <I>synchronization transparent</I> but methods inherited from
 * {@link ListenerRegistry} are required to be
 * <I>synchronization transparent</I> as specified by {@code ListenerRegistry}.
 *
 * @param <ListenerType> the type of the event listeners can possibly be added
 *   to the container
 *
 * @see CopyOnTriggerListenerManager
 * @see OneShotListenerManager
 */
public interface ListenerManager<ListenerType>
extends
        ListenerRegistry<ListenerType> {
    /**
     * Invokes the {@link EventDispatcher#onEvent(Object, Object) onEvent}
     * method of the specified {@code EventDispatcher} with the currently
     * registered listeners and the argument specified. The {@code onEvent}
     * method is called synchronously in the current thread.
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
     * {@link org.jtrim2.executor.TaskScheduler TaskScheduler} to invoke the
     * {@code onEvent} method.
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
     *
     * @param <ArgType> the type of the argument which is passed to event
     *   listeners
     *
     * @see org.jtrim2.executor.TaskScheduler
     */
    public <ArgType> void onEvent(
            EventDispatcher<? super ListenerType, ? super ArgType> eventDispatcher,
            ArgType arg);
}
