package org.jtrim.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a {@code SimpleListenerRegistry} which is backed by another
 * {@code SimpleListenerRegistry}. The backing {@code SimpleListenerRegistry}
 * can be replaced by another {@code SimpleListenerRegistry} by calling the
 * {@link #replaceRegistry(SimpleListenerRegistry) replaceRegistry} method.
 * <P>
 * Listeners added to this listener registry will also be added to the backing
 * {@code SimpleListenerRegistry}. When replacing the backing listener registry,
 * these listeners will be unregistered from the registry backing the
 * {@code ProxyListenerRegistry} and will be registered with the newly added
 * listener registry.
 * <P>
 * <B>Warning</B>: Adding a listener to the {@code ProxyListenerRegistry}
 * will retain a reference to the listener even if the listener is
 * automatically unregistered by the backing listener registry.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @param <ListenerType> the type of the event listeners can possibly be added
 *   to the container
 *
 * @author Kelemen Attila
 */
public final class ProxyListenerRegistry<ListenerType>
implements
        SimpleListenerRegistry<ListenerType> {

    private final Lock mainLock;
    private final RefList<ListenerAndRef<ListenerType>> listeners;
    private SimpleListenerRegistry<? super ListenerType> currentRegistry;

    /**
     * Creates a new {@code ProxyListenerRegistry} which is initially backed by
     * the passed {@code ListenerRegistry}. This listener registry can be
     * replaced later by a {@code replaceRegistry} call.
     * <P>
     * Note that the {@code ProxyListenerRegistry} is only aware of listeners
     * added through the {@code ProxyListenerRegistry} itself. Listeners
     * directly added to the backing listener registry will not affect
     * the {@code ProxyListenerRegistry}.
     *
     * @param initialRegistry the initial backing listener registry. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener registry
     *   is {@code null}
     */
    public ProxyListenerRegistry(SimpleListenerRegistry<? super ListenerType> initialRegistry) {
        ExceptionHelper.checkNotNullArgument(initialRegistry, "initialRegistry");

        this.mainLock = new ReentrantLock();
        this.listeners = new RefLinkedList<>();
        this.currentRegistry = initialRegistry;
    }

    /**
     * Unregisters already added listeners from the current backing registry
     * and registers them with the specified listener registry. The listener
     * registry passed to this method will be the new registry backing this
     * {@code ProxyListenerRegistry}.
     * <P>
     * Note that this {@code ProxyListenerRegistry} is only aware of listeners
     * added through the {@code ProxyListenerRegistry} itself. Listeners
     * directly added to the backing listener registry will not affect
     * this {@code ProxyListenerRegistry}.
     * <P>
     * <B>Warning</B>: Listeners will not be notified by this method. If you
     * need to notify client code, you have to do so manually by calling the
     * {@link #onEvent(EventDispatcher, Object) onEvent} method.
     *
     * @param registry the new listener registry backing this
     *   {@code ProxyListenerRegistry}. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener registry
     *   is {@code null}
     */
    public void replaceRegistry(SimpleListenerRegistry<? super ListenerType> registry) {
        ExceptionHelper.checkNotNullArgument(registry, "registry");

        mainLock.lock();
        try {
            currentRegistry = registry;
            for (ListenerAndRef<ListenerType> ref: listeners) {
                ref.registerWith(registry);
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Invokes the {@link EventDispatcher#onEvent(Object, Object) onEvent}
     * method of the specified {@code EventDispatcher} with the currently
     * registered listeners and the argument specified. The {@code onEvent}
     * method is called synchronously in the current thread. Note that this
     * method will only notify listeners added through this
     * {@code ProxyListenerRegistry} as it does not know about listeners
     * added directly to the backing listener registry.
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
     *
     * @param eventDispatcher the {@code EventDispatcher} whose {@code onEvent}
     *   method is to be called for every registered listener with the specified
     *   argument. The {@code onEvent} method will be called as many times as
     *   many currently registered listeners are. This argument cannot be
     *   {@code null}.
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
     * @see org.jtrim.concurrent.TaskScheduler
     */
    public <ArgType> void onEvent(
            EventDispatcher<? super ListenerType, ? super ArgType> eventDispatcher,
            ArgType arg) {
        ExceptionHelper.checkNotNullArgument(eventDispatcher, "eventDispatcher");

        List<ListenerType> toNotify;
        mainLock.lock();
        try {
            toNotify = new ArrayList<>(listeners.size());

            for (ListenerAndRef<ListenerType> ref: listeners) {
                toNotify.add(ref.getListener());
            }
        } finally {
            mainLock.unlock();
        }

        Throwable toThrow = null;
        for (ListenerType listener: toNotify) {
            try {
                eventDispatcher.onEvent(listener, arg);
            } catch (Throwable ex) {
                if (toThrow == null) toThrow = ex;
                else toThrow.addSuppressed(ex);
            }
        }

        ExceptionHelper.rethrowIfNotNull(toThrow);
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Warning</B>: Adding a listener to the {@code ProxyListenerRegistry}
     * may retain a reference to the listener even if the listener is
     * automatically unregistered by the backing listener registry.
     */
    @Override
    public ListenerRef registerListener(ListenerType listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        final ListenerAndRef<ListenerType> newRef = new ListenerAndRef<>(listener);
        final RefCollection.ElementRef<?> listRef;

        mainLock.lock();
        try {
            listRef = listeners.addLastGetReference(newRef);
            newRef.registerWith(currentRegistry);
        } finally {
            mainLock.unlock();
        }

        return new ProxyListenerRef(newRef, listRef);
    }

    /**
     * Returns the number of listeners currently being proxied by this
     * {@code ProxyListenerRegistry}. This method is added only for testing
     * purposes, so we can test that we do not proxy listeners unregistered
     * right after being added.
     *
     * @return the number of listeners currently being proxied by this
     *   {@code ProxyListenerRegistry}. This method always returns a value
     *   greater than or equal to zero.
     */
    int getNumberOfProxiedListeners() {
        mainLock.lock();
        try {
            return listeners.size();
        } finally {
            mainLock.unlock();
        }
    }

    private static final class ListenerAndRef<ListenerType> {
        private final ListenerType listener;
        private ListenerRef listenerRef;

        public ListenerAndRef(ListenerType listener) {
            this.listener = listener;
            this.listenerRef = null;
        }

        public ListenerType getListener() {
            return listener;
        }

        public void registerWith(SimpleListenerRegistry<? super ListenerType> registry) {
            ListenerRef currentRef = listenerRef;
            if (currentRef != null) {
                currentRef.unregister();
            }

            listenerRef = registry.registerListener(listener);
        }

        public void unregister() {
            ListenerRef currentRef = listenerRef;
            if (currentRef != null) {
                currentRef.unregister();
                listenerRef = null;
            }
        }
    }

    private class ProxyListenerRef implements ListenerRef {
        private ListenerAndRef<ListenerType> newRef;
        private RefCollection.ElementRef<?> listRef;

        public ProxyListenerRef(
                ListenerAndRef<ListenerType> newRef,
                RefCollection.ElementRef<?> listRef) {
            assert newRef != null;
            assert listRef != null;

            this.newRef = newRef;
            this.listRef = listRef;
        }

        @Override
        public boolean isRegistered() {
            mainLock.lock();
            try {
                return listRef != null;
            } finally {
                mainLock.unlock();
            }
        }

        @Override
        public void unregister() {
            mainLock.lock();
            try {
                // We null out references so that we no longer retain a
                // reference to the listener, allowing it to be garbage
                // collected.
                if (listRef != null) {
                    listRef.remove();
                    listRef = null;
                }
                if (newRef != null) {
                    newRef.unregister();
                    newRef = null;
                }
            } finally {
                mainLock.unlock();
            }
        }
    }
}
