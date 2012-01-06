/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.event;

import java.util.*;

import static org.jtrim.event.GenericEventHandlerContainer.Store;

/**
 *
 * @author Kelemen Attila
 */
public final class FifoEventHandlerContainer<ListenerType>
        implements EventHandlerContainer<ListenerType> {

    private final EventHandlerContainer<ListenerType> wrapped;

    public FifoEventHandlerContainer() {
        this.wrapped = new GenericEventHandlerContainer<>(
                new FifoStorageStrategy<ListenerType>());
    }

    @Override
    public void registerListener(ListenerType listener) {
        wrapped.registerListener(listener);
    }

    @Override
    public void removeListener(ListenerType listener) {
        wrapped.removeListener(listener);
    }

    @Override
    public Collection<ListenerType> getListeners() {
        return wrapped.getListeners();
    }

    @Override
    public void onEvent(EventDispatcher<ListenerType> eventDispatcher) {
        wrapped.onEvent(eventDispatcher);
    }

    @Override
    public int getListenerCount() {
        return wrapped.getListenerCount();
    }

    private static class FifoStore<E> implements Store<E> {

        private static <E> FifoStore<E> cast(Store<E> store) {
            return (FifoStore<E>)store;
        }

        private final LinkedList<E> elements;

        public FifoStore(Store<E> elements) {
            if (elements != null) {
                this.elements = new LinkedList<>(cast(elements).elements);
            }
            else {
                this.elements = new LinkedList<>();
            }
        }

        private boolean removeOneFromList(E toRemove) {
            Iterator<?> itr = elements.descendingIterator();
            while (itr.hasNext()) {
                Object currentElement = itr.next();

                if (currentElement == toRemove) {
                    itr.remove();
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean removeOne(E toRemove) {
            return removeOneFromList(toRemove);
        }

        @Override
        public void removeMany(Store<E> toRemove) {
            for (E element: toRemove) {
                removeOneFromList(element);
            }
        }

        @Override
        public void addOne(E toAdd) {
            elements.add(toAdd);
        }

        @Override
        public void addMany(Store<E> toAdd) {
            elements.addAll(cast(toAdd).elements);
        }

        @Override
        public int size() {
            return elements.size();
        }

        @Override
        public boolean isEmpty() {
            return elements.isEmpty();
        }

        @Override
        public Iterator<E> iterator() {
            return elements.iterator();
        }
    }

    private static class FifoStorageStrategy<E>
            implements GenericEventHandlerContainer.StorageStrategy<E> {

        @Override
        public Store<E> createStorage(Store<E> elements) {
            return new FifoStore<>(elements);
        }
    }
}
