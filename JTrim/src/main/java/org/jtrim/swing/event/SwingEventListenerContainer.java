/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.swing.event;

import java.util.*;
import java.util.concurrent.locks.*;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.EventHandlerContainer;

/**
 * @deprecated This implementation may leak if there are no events.
 * @author Kelemen Attila
 */
@Deprecated
public final class SwingEventListenerContainer<ListenerType> implements EventHandlerContainer<ListenerType> {
    private final List<ListenerType> eventListeners;
    private final List<ListenerType> newEventListeners;
    private final List<ListenerType> listenersToRemove;
    private final ReentrantLock mainLock;

    public SwingEventListenerContainer() {
        this.eventListeners = new LinkedList<>();
        this.newEventListeners = new LinkedList<>();
        this.listenersToRemove = new LinkedList<>();
        this.mainLock = new ReentrantLock();
    }

    private void updateListenersUnlocked() {
        assert mainLock.isHeldByCurrentThread();

        if (!listenersToRemove.isEmpty()) {
            Iterator<ListenerType> listenerIterator = eventListeners.iterator();
            while (!listenersToRemove.isEmpty() && listenerIterator.hasNext()) {
                ListenerType listener = listenerIterator.next();

                Iterator<ListenerType> toRemoveIterator = listenersToRemove.iterator();
                while (toRemoveIterator.hasNext()) {
                    ListenerType toRemove = toRemoveIterator.next();
                    if (toRemove == listener) {
                        listenerIterator.remove();
                        toRemoveIterator.remove();
                        break;
                    }
                }
            }

            listenersToRemove.clear();
        }

        if (!newEventListeners.isEmpty()) {
            eventListeners.addAll(newEventListeners);
            newEventListeners.clear();
        }
    }

    private void updateListeners() {
        mainLock.lock();
        try {
            updateListenersUnlocked();
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public void registerListener(ListenerType listener) {
        if (listener != null) {
            mainLock.lock();
            try {
                newEventListeners.add(listener);
            } finally {
                mainLock.unlock();
            }
        }
    }

    @Override
    public void removeListener(ListenerType listener) {
        if (listener != null) {
            mainLock.lock();

            removeBlock:
            try {
                if (!newEventListeners.isEmpty()) {
                    Iterator<ListenerType> newListenersItr = newEventListeners.iterator();
                    while (newListenersItr.hasNext()) {
                        ListenerType currentListener = newListenersItr.next();
                        if (currentListener == listener) {
                            newListenersItr.remove();
                            break removeBlock;
                        }
                    }
                }

                listenersToRemove.add(listener);
            } finally {
                mainLock.unlock();
            }
        }
    }

    @Override
    public void onEvent(EventDispatcher<ListenerType> eventHandler) {
        if (eventHandler != null) {
            updateListeners();
            for (ListenerType listener: eventListeners) {
                eventHandler.onEvent(listener);
            }
        }
    }

    @Override
    public Collection<ListenerType> getListeners() {
        mainLock.lock();
        try {
            updateListenersUnlocked();

            assert listenersToRemove.isEmpty();
            assert newEventListeners.isEmpty();

            return new ArrayList<>(eventListeners);
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public int getListenerCount() {
        mainLock.lock();
        try {
            updateListenersUnlocked();
            return eventListeners.size();
        } finally {
            mainLock.unlock();
        }
    }
}
