/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.event;

import java.util.*;
import java.util.concurrent.locks.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class GenericEventHandlerContainer<ListenerType>
        implements EventHandlerContainer<ListenerType> {

    public static interface StorageStrategy<E> {
        public Store<E> createStorage(Store<E> elements);
    }

    public static interface Store<E> extends Iterable<E> {
        public boolean removeOne(E toRemove);
        public void removeMany(Store<E> toRemove);

        public void addOne(E toAdd);
        public void addMany(Store<E> toAdd);

        public int size();
        public boolean isEmpty();
    }

    // Lock order: modifyLock, mainLock.writeLock()
    //             Holding mainLock.readLock() will not block any call
    //             only makes calls less efficient.

    // Locks:
    // - modifyLock: Protects the listeners, listenersToRemove and
    //               listenersToAdd fields from modification.
    //               Only those owning this lock allowed to modify
    //               these fields or replace them.
    //
    // - mainLock: Protects the content of the listeners field.
    //             Those who want to read the content of the listeners
    //             field must own mainLock.readLock(), those who want
    //             to change its content must own mainLock.writeLock().


    private final StorageStrategy<ListenerType> storageStrategy;
    private final ReentrantLock modifyLock;
    private final ReentrantReadWriteLock mainLock;
    private volatile Store<ListenerType> listeners;

    private Store<ListenerType> listenersToRemove;
    private Store<ListenerType> listenersToAdd;

    public GenericEventHandlerContainer(StorageStrategy<ListenerType> storageStrategy) {
        this.storageStrategy = storageStrategy;

        this.modifyLock = new ReentrantLock();
        this.mainLock = new ReentrantReadWriteLock();

        this.listeners = storageStrategy.createStorage(null);
        this.listenersToRemove = null;
        this.listenersToAdd = null;
    }

    private static boolean hasElements(Store<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    private void copyAndRemoveListener(ListenerType listener) {
        assert !mainLock.isWriteLockedByCurrentThread();
        assert modifyLock.isHeldByCurrentThread();

        Store<ListenerType> newListeners;

        newListeners = storageStrategy.createStorage(listeners);
        newListeners.removeOne(listener);

        listeners = newListeners;
    }

    private void copyAndRegisterListener(ListenerType listener) {
        assert !mainLock.isWriteLockedByCurrentThread();
        assert modifyLock.isHeldByCurrentThread();

        Store<ListenerType> newListeners;

        newListeners = storageStrategy.createStorage(listeners);
        newListeners.addOne(listener);

        listeners = newListeners;
    }

    private void removeLater(ListenerType listener) {
        modifyLock.lock();
        try {
            if (listenersToAdd == null || !listenersToAdd.removeOne(listener)) {
                Store<ListenerType> removeList = listenersToRemove;
                if (removeList == null) {
                    removeList = storageStrategy.createStorage(null);
                    listenersToRemove = removeList;
                }

                removeList.addOne(listener);
            }
        } finally {
            modifyLock.unlock();
        }
    }

    private void registerLater(ListenerType listener) {
        modifyLock.lock();
        try {
            if (listenersToRemove == null || !listenersToRemove.removeOne(listener)) {
                Store<ListenerType> addList = listenersToAdd;
                if (addList == null) {
                    addList = storageStrategy.createStorage(null);
                    listenersToAdd = addList;
                }

                addList.addOne(listener);
            }
        } finally {
            modifyLock.unlock();
        }
    }

    private void makeChangesVisible() {
        assert !modifyLock.isHeldByCurrentThread();
        assert !mainLock.isWriteLockedByCurrentThread();

        modifyLock.lock();
        try {
            Store<ListenerType> toRemove;
            Store<ListenerType> toAdd;

            toRemove = listenersToRemove;
            toAdd = listenersToAdd;

            listenersToRemove = null;
            listenersToAdd = null;

            // check if there is anything to do
            if (!hasElements(toRemove) && !hasElements(toAdd)) {
                return;
            }

            // Note that there is no listener in toAdd that is
            // in toRemove as well.
            // So we may remove the listeners before adding the new listeners

            Lock wLock = mainLock.writeLock();
            if (wLock.tryLock()) {
                try {
                    Store<ListenerType> currentListeners = listeners;

                    if (toRemove != null) {
                        currentListeners.removeMany(toRemove);
                    }

                    if (toAdd != null) {
                        currentListeners.addMany(toAdd);
                    }
                } finally {
                    wLock.unlock();
                }
            }
            else {
                Store<ListenerType> newListeners;
                newListeners = storageStrategy.createStorage(listeners);

                if (toRemove != null) {
                    newListeners.removeMany(toRemove);
                }

                if (toAdd != null) {
                    newListeners.addMany(toAdd);
                }

                listeners = newListeners;
            }
        } finally {
            modifyLock.unlock();
        }

    }

    @Override
    public void registerListener(ListenerType listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        assert !mainLock.isWriteLockedByCurrentThread();

        // An onEvent call is currently in progress on the current thread.
        if (mainLock.getReadHoldCount() > 0) {
            registerLater(listener);
            return;
        }

        makeChangesVisible();

        modifyLock.lock();
        try {
            Lock wLock = mainLock.writeLock();
            if (wLock.tryLock()) {
                try {
                    listeners.addOne(listener);
                } finally {
                    wLock.unlock();
                }
            }
            else {
                copyAndRegisterListener(listener);
            }
        } finally {
            modifyLock.unlock();
        }
    }

    @Override
    public void removeListener(ListenerType listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        assert !mainLock.isWriteLockedByCurrentThread();

        // An onEvent call is currently in progress on the current thread.
        if (mainLock.getReadHoldCount() > 0) {
            removeLater(listener);
            return;
        }

        makeChangesVisible();

        modifyLock.lock();
        try {
            Lock wLock = mainLock.writeLock();
            if (wLock.tryLock()) {
                try {
                    listeners.removeOne(listener);
                } finally {
                    wLock.unlock();
                }
            }
            else {
                copyAndRemoveListener(listener);
            }
        } finally {
            modifyLock.unlock();
        }
    }

    @Override
    public Collection<ListenerType> getListeners() {
        makeChangesVisible();

        Lock rLock = mainLock.readLock();
        rLock.lock();
        try {
            Collection<ListenerType> result = new ArrayList<>(listeners.size());
            for (ListenerType listener: listeners) {
                result.add(listener);
            }

            return result;
        } finally {
            rLock.unlock();
        }
    }

    @Override
    public void onEvent(EventDispatcher<ListenerType> eventHandler) {
        makeChangesVisible();

        Lock rLock = mainLock.readLock();
        rLock.lock();
        try {
            Store<ListenerType> currentListeners = listeners;
            for (ListenerType listener: currentListeners) {
                eventHandler.onEvent(listener);
            }
        } finally {
            rLock.unlock();
        }

        // Even without this call, the listener container would work.
        // But this call will allow the GC to collect those listeners
        // that were removed during the event calls.
        makeChangesVisible();
    }

    @Override
    public int getListenerCount() {
        Lock rLock = mainLock.readLock();
        rLock.lock();
        try {
            return listeners.size();
        } finally {
            rLock.unlock();
        }
    }
}
