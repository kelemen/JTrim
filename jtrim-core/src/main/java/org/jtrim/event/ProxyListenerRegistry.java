package org.jtrim.event;

import java.util.ArrayList;
import java.util.Iterator;
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
 * may retain a reference to the listener even if the listener is
 * automatically unregistered by the backing listener registry. Therefore
 * you cannot rely on automatic unregistering. The only exception from this
 * rule is when the backing listener registry unregister the listeners
 * immediately. This implementation will detect this and will not retain a
 * reference to the listener added.
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
     * Creates a new {@code ProxyListenerRegistry} which is initally backed by
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
     * <B>Warning</B>: This method relies on the
     * {@link ListenerRef#isRegistered() isRegistered} method of the
     * {@link ListenerRef} returned by the backing listener registry to
     * determine if the listener needs to be called or not. If
     * {@code isRegistered} returns {@code true} even if the listener is
     * actually unregistered, then this method will notify the listener not
     * knowing about the listener being unregistered.
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

            Iterator<ListenerAndRef<ListenerType>> it = listeners.iterator();
            while (it.hasNext()) {
                ListenerAndRef<ListenerType> ref = it.next();
                if (ref.isRegistered()) {
                    toNotify.add(ref.getListener());
                }
                else {
                    it.remove();
                }
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
     * automatically unregistered by the backing listener registry. Therefore
     * you cannot rely on automatic unregistering. The only exception from this
     * rule is when the backing listener registry unregister the listeners
     * immediately. This implementation will detect this and will not retain a
     * reference to the listener added.
     */
    @Override
    public ListenerRef registerListener(ListenerType listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        final ListenerAndRef<ListenerType> newRef = new ListenerAndRef<>(listener);
        final RefCollection.ElementRef<?> listRef;

        mainLock.lock();
        try {
            listRef = listeners.addLastGetReference(newRef);

            // It is often the case that listeners are automatically
            // unregistered and clients know this and won't bother to unregister
            // the listener. This would cause a memory leak because we would
            // keep the reference in our list forever. However, if a listener
            // is automatically unregistered, well behaving listener registries
            // should return a ListenerRef always signalling unregistered state.
            // So this check is to detect this case.
            if (!newRef.registerWith(currentRegistry)) {
                listRef.remove();
                return UnregisteredListenerRef.INSTANCE;
            }
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

        public boolean registerWith(SimpleListenerRegistry<? super ListenerType> registry) {
            ListenerRef currentRef = listenerRef;
            if (currentRef != null) {
                currentRef.unregister();
            }

            currentRef = registry.registerListener(listener);
            if (currentRef.isRegistered()) {
                listenerRef = currentRef;
                return true;
            }
            else {
                listenerRef = null;
                return false;
            }
        }

        public boolean isRegistered() {
            ListenerRef currentRef = listenerRef;
            return currentRef != null ? currentRef.isRegistered() : false;
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
                ListenerAndRef<ListenerType> currentRef = newRef;
                return currentRef != null ? currentRef.isRegistered() : false;
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
