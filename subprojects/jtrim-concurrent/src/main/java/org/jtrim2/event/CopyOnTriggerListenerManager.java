package org.jtrim2.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.collections.RefCollection;
import org.jtrim2.collections.RefLinkedList;
import org.jtrim2.collections.RefList;

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
 */
public final class CopyOnTriggerListenerManager<ListenerType>
implements
        ListenerManager<ListenerType> {
    private static final Logger LOGGER = Logger.getLogger(CopyOnTriggerListenerManager.class.getName());

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
    public ListenerRef registerListener(ListenerType listener) {
        Objects.requireNonNull(listener, "listener");

        RefCollection.ElementRef<ListenerType> listenerRef;
        writeLock.lock();
        try {
            listenerRef = listeners.addGetReference(listener);
        } finally {
            writeLock.unlock();
        }

        return () -> {
            writeLock.lock();
            try {
                listenerRef.remove();
            } finally {
                writeLock.unlock();
            }
        };
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: In case an exception is thrown by a
     * registered listener, other listeners will still be invoked. The thrown
     * exception will be logged on a {@code SEVERE} log level.
     */
    @Override
    public <ArgType> void onEvent(
            EventDispatcher<? super ListenerType, ? super ArgType> eventDispatcher,
            ArgType arg) {
        Objects.requireNonNull(eventDispatcher, "eventDispatcher");

        for (ListenerType listener: getListeners()) {
            try {
                eventDispatcher.onEvent(listener, arg);
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Unexpected exception in listener.", ex);
            }
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
}
