package org.jtrim.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jtrim.collections.RefCollection;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.utils.ExceptionHelper;

/**
 * An {@link ListenerManager} implementation which creates a copy of the
 * currently registered listeners when dispatching events. That is, in the
 * {@link #onEvent(EventDispatcher, java.lang.Object) onEvent} method, it first
 * creates copy of the currently added listeners as done by the
 * {@link #getListeners() getListeners()} method and then dispatches the event
 * to all of them.
 * <P>
 * This implementation allows to register the same listener multiple times and
 * listeners registered multiple times will be notified multiple times as well
 * when dispatching events.
 * <P>
 * Adding and removing listeners are constant time operations but, of course,
 * dispatching events requires linear time in the number of registered
 * listeners (plus the time the listeners need).
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
public final class CopyOnTriggerListenerManager<ListenerType, ArgType>
implements
        ListenerManager<ListenerType, ArgType> {

    private final Lock readLock;
    private final Lock writeLock;
    private final RefList<ListenerType> listeners;

    /**
     * Creates a new {@code CopyOnTriggerListenerManager} with no
     * listeners registered.
     */
    public CopyOnTriggerListenerManager() {
        ReadWriteLock listenerLock = new ReentrantReadWriteLock();
        this.readLock = listenerLock.readLock();
        this.writeLock = listenerLock.writeLock();

        this.listeners = new RefLinkedList<>();
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: Adding and removing (using the returned
     * reference) a listener is a constant time operation.
     */
    @Override
    public ListenerRef<ListenerType> registerListener(ListenerType listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        RefCollection.ElementRef<ListenerType> listenerRef;
        writeLock.lock();
        try {
            listenerRef = listeners.addGetReference(listener);
        } finally {
            writeLock.unlock();
        }
        return new CollectionBasedListenerRef<>(readLock, writeLock, listenerRef);
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: In case an exception is thrown by a
     * registered listener, other listeners will still be invoked. After
     * notifying all the listeners, the first exception thrown by the listeners
     * will be rethrown by this method suppressing all other exceptions (i.e.:
     * adding them as {@link Throwable#addSuppressed(Throwable) suppressed}
     * exceptions to the thrown exception).
     */
    @Override
    public void onEvent(
            EventDispatcher<? super ListenerType, ? super ArgType> eventDispatcher,
            ArgType arg) {
        ExceptionHelper.checkNotNullArgument(eventDispatcher, "eventDispatcher");

        Throwable error = null;

        for (ListenerType listener: getListeners()) {
            try {
                eventDispatcher.onEvent(listener, arg);
            } catch (Throwable ex) {
                if (error == null) {
                    error = ex;
                }
                else {
                    error.addSuppressed(ex);
                }
            }
        }

        if (error != null) {
            ExceptionHelper.rethrow(error);
        }
    }

    /**
     * Returns the collection of listeners
     * {@link #registerListener(Object) added} but not yet
     * {@link ListenerRef#unregister() removed} in no particular order.
     * <P>
     * The returned collection must be a snapshot of the currently
     * added listeners. That is, listeners subsequently added to this
     * {@code ListenerManager} must have no effect on the previously
     * returned listener collection. The returned collection is may or may not
     * be read-only but if it can be modified, modifications to the returned
     * collection must have no effect on this {@code ListenerManager}.
     * <P>
     * Retrieving the listeners is a linear time operation in the number of
     * registered listeners.
     *
     * @return the collection of listeners
     *   {@link #registerListener(Object) added} but not yet
     *   {@link ListenerRef#unregister() removed} in no particular order. This
     *   method never returns {@code null} and the returned collection can be
     *   read-only.
     */
    Collection<ListenerType> getListeners() {
        readLock.lock();
        try {
            return listeners.isEmpty()
                    ? Collections.<ListenerType>emptySet()
                    : new ArrayList<>(listeners);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: Retrieving the number of listeners is a
     * constant time operation.
     */
    @Override
    public int getListenerCount() {
        readLock.lock();
        try {
            return listeners.size();
        } finally {
            readLock.unlock();
        }
    }

    private static class CollectionBasedListenerRef<ListenerType>
    implements
            ListenerRef<ListenerType> {

        private final Lock readLock;
        private final Lock writeLock;
        private final RefCollection.ElementRef<ListenerType> listenerRef;

        public CollectionBasedListenerRef(Lock readLock, Lock writeLock,
                RefCollection.ElementRef<ListenerType> listenerRef) {

            assert readLock != null;
            assert writeLock != null;
            assert listenerRef != null;

            this.readLock = readLock;
            this.writeLock = writeLock;
            this.listenerRef = listenerRef;
        }

        @Override
        public boolean isRegistered() {
            readLock.lock();
            try {
                return !listenerRef.isRemoved();
            } finally {
                readLock.unlock();
            }
        }

        @Override
        public void unregister() {
            writeLock.lock();
            try {
                listenerRef.remove();
            } finally {
                writeLock.unlock();
            }
        }

        @Override
        public ListenerType getListener() {
            readLock.lock();
            try {
                return listenerRef.getElement();
            } finally {
                readLock.unlock();
            }
        }
    }
}
