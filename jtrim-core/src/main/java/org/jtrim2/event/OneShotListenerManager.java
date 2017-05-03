package org.jtrim2.event;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.utils.ExceptionHelper;

/**
 * A {@link ListenerRegistry} implementation which will automatically unregister
 * listeners once they have been notified by an {@code onEvent} call. No
 * listener will be notified more than once unless the listener has been added
 * multiple times. Listeners will not even be notified multiple times, even
 * if the {@code onEvent} method is called more than once.
 * <P>
 * This implementation is useful for events which can trigger only once. A
 * termination event is a typical example for an event which can occur only once
 * (if restart is not possible).
 * <P>
 * If you need to notify the listener even if the event has been dispatched
 * previously, consider using the
 * {@link #registerOrNotifyListener(Object) registerOrNotifyListener} method.
 *
 * <h3>Thread safety</h3>
 * As required by {@code ListenerManager}, the methods of this class are
 * safe to be accessed concurrently by multiple threads.
 *
 * <h4>Synchronization transparency</h4>
 * As required by {@code ListenerManager}, except for the {@code onEvent}
 * method, methods of this class are <I>synchronization transparent</I>.
 *
 * @param <ListenerType> the type of the event handlers can possibly be added
 *   to the container
 * @param <ArgType> the type of the argument which can be passed to event
 *   handlers by the {@code onEvent} method
 *
 * @author Kelemen Attila
 */
public final class OneShotListenerManager<ListenerType, ArgType>
implements
        ListenerRegistry<ListenerType> {

    private final ListenerManager<SingleShotListener<ListenerType>> listenerManager;

    private volatile boolean notified;
    private volatile DispatcherWithArg<ListenerType, ArgType> lastEvent;
    private final SingleShotDispatcher<ListenerType, ArgType> dispatcher;

    /**
     * Creates a new {@code OneShotListenerManager} with no listeners
     * registered.
     */
    public OneShotListenerManager() {
        this.listenerManager = new CopyOnTriggerListenerManager<>();
        this.lastEvent = null;
        this.notified = false;
        this.dispatcher = new SingleShotDispatcher<>();
    }

    /**
     * Invokes the {@link EventDispatcher#onEvent(Object, Object) onEvent}
     * method of the specified {@code EventDispatcher} with the currently
     * registered listeners and the argument specified unless this method has
     * already been called. If this method has already been called, then calling
     * this method has no effect. That is, this method call is idempotent.
     * <P>
     * The {@code onEvent} method is called synchronously in the current thread.
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
     *
     * @see org.jtrim2.concurrent.TaskScheduler
     */
    public void onEvent(
            EventDispatcher<? super ListenerType, ? super ArgType> eventDispatcher,
            ArgType arg) {

        DispatcherWithArg<ListenerType, ArgType> currentEvent;
        currentEvent = new DispatcherWithArg<>(eventDispatcher, arg);
        lastEvent = currentEvent;
        notified = true;

        listenerManager.onEvent(dispatcher, currentEvent);
    }

    /**
     * Adds an event listener to this container or notifies the listener if
     * {@code onEvent} has already been called and returns a reference which
     * can later be used to removed the listener added. To notify the listener,
     * the {@link EventDispatcher} and the argument from the last
     * {@code onEvent} call will be used.
     * <P>
     * If the {@code onEvent} is always called with the same arguments, this
     * method is effectively the same as calling the {@code registerListener}
     * and then the {@code onEvent} method. Although this method is more
     * efficient.
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
    public ListenerRef registerOrNotifyListener(ListenerType listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        // This is just a quick check and is not required for correctness.
        if (notified) {
            // lastEvent never becomes null after it has been set,
            // so there is no need for a local copy.
            lastEvent.dispatch(listener);
            return UnregisteredListenerRef.INSTANCE;
        }

        ListenerRef result = registerListener(listener);
        if (lastEvent != null) {
            listenerManager.onEvent(dispatcher, lastEvent);
        }
        return result;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ListenerRef registerListener(ListenerType listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        SingleShotListener<ListenerType> wrapped = new SingleShotListener<>(listener);
        ListenerRef ref = listenerManager.registerListener(wrapped);
        wrapped.setListenerRef(ref);
        return ref;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getListenerCount() {
        return listenerManager.getListenerCount();
    }

    private static class SingleShotDispatcher<ListenerType, ArgType>
    implements
            EventDispatcher<SingleShotListener<ListenerType>, DispatcherWithArg<ListenerType, ArgType>> {

        @Override
        public void onEvent(
                SingleShotListener<ListenerType> eventListener,
                DispatcherWithArg<ListenerType, ArgType> arg) {

            eventListener.dispatch(arg);
        }
    }

    private static class SingleShotListener<ListenerType> {
        private final AtomicReference<ListenerType> listenerRef;
        private volatile ListenerRef registerRef;

        public SingleShotListener(ListenerType listener) {
            this.listenerRef = new AtomicReference<>(listener);
            this.registerRef = null;
        }

        public void setListenerRef(ListenerRef registerRef) {
            this.registerRef = registerRef;
            if (listenerRef.get() == null) {
                registerRef.unregister();
                this.registerRef = null;
            }
        }

        public <ArgType> void dispatch(DispatcherWithArg<ListenerType, ArgType> arg) {
            ListenerType listener = listenerRef.getAndSet(null);
            if (listener != null) {
                try {
                    arg.dispatch(listener);
                } finally {
                    ListenerRef currentRegisterRef = registerRef;
                    if (currentRegisterRef != null) {
                        currentRegisterRef.unregister();
                        registerRef = null;
                    }
                }
            }
        }
    }

    private static class DispatcherWithArg<ListenerType, ArgType> {
        private final EventDispatcher<? super ListenerType, ? super ArgType> eventDispatcher;
        private final ArgType arg;

        public DispatcherWithArg(
                EventDispatcher<? super ListenerType, ? super ArgType> eventDispatcher,
                ArgType arg) {
            ExceptionHelper.checkNotNullArgument(eventDispatcher, "eventDispatcher");

            this.eventDispatcher = eventDispatcher;
            this.arg = arg;
        }

        public void dispatch(ListenerType listener) {
            eventDispatcher.onEvent(listener, arg);
        }
    }
}
