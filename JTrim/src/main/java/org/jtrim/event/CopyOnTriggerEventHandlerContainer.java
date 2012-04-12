/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
 *
 * @author Kelemen Attila
 */
public final class CopyOnTriggerEventHandlerContainer<ListenerType, ArgType>
implements
        EventHandlerContainer<ListenerType, ArgType> {

    private final Lock readLock;
    private final Lock writeLock;
    private final RefList<ListenerType> listeners;

    public CopyOnTriggerEventHandlerContainer() {
        ReadWriteLock listenerLock = new ReentrantReadWriteLock();
        this.readLock = listenerLock.readLock();
        this.writeLock = listenerLock.writeLock();

        this.listeners = new RefLinkedList<>();
    }

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

    @Override
    public Collection<ListenerType> getListeners() {
        readLock.lock();
        try {
            return listeners.isEmpty()
                    ? Collections.<ListenerType>emptySet()
                    : new ArrayList<>(listeners);
        } finally {
            readLock.unlock();
        }
    }

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
