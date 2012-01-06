/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.event;

import java.util.*;
import java.util.concurrent.locks.*;

/**
 *
 * @author Kelemen Attila
 */
public final class CopyOnTriggerEventHandlerContainer<ListenerType> implements EventHandlerContainer<ListenerType> {

    private final Lock listenerReadLock;
    private final Lock listenerWriteLock;
    private final List<ListenerType> listeners;

    public CopyOnTriggerEventHandlerContainer() {
        ReadWriteLock listenerLock = new ReentrantReadWriteLock();
        this.listenerReadLock = listenerLock.readLock();
        this.listenerWriteLock = listenerLock.writeLock();

        this.listeners = new LinkedList<>();
    }

    @Override
    public void registerListener(ListenerType listener) {
        if (listener != null) {
            listenerWriteLock.lock();
            try {
                listeners.add(listener);
            } finally {
                listenerWriteLock.unlock();
            }
        }
    }

    @Override
    public void removeListener(ListenerType listener) {
        if (listener != null) {
            listenerWriteLock.lock();
            try {
                Iterator<ListenerType> listenerIterator = listeners.iterator();
                while (listenerIterator.hasNext()) {
                    ListenerType currentListener = listenerIterator.next();
                    if (currentListener == listener) {
                        listenerIterator.remove();
                        break;
                    }
                }
            } finally {
                listenerWriteLock.unlock();
            }
        }
    }

    @Override
    public void onEvent(EventDispatcher<ListenerType> eventHandler) {
        if (eventHandler != null) {
            List<ListenerType> tmpListeners = null;
            listenerReadLock.lock();
            try {
                if (!listeners.isEmpty()) {
                    tmpListeners = new ArrayList<>(listeners);
                }
            } finally {
                listenerReadLock.unlock();
            }

            if (tmpListeners != null) {
                for (ListenerType listener: tmpListeners) {
                    eventHandler.onEvent(listener);
                }
            }
        }
    }

    @Override
    public Collection<ListenerType> getListeners() {
        listenerReadLock.lock();
        try {
            return new ArrayList<>(listeners);
        } finally {
            listenerReadLock.unlock();
        }
    }

    @Override
    public int getListenerCount() {
        listenerReadLock.lock();
        try {
            return listeners.size();
        } finally {
            listenerReadLock.unlock();
        }
    }

}
