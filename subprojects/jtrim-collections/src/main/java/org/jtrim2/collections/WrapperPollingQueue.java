package org.jtrim2.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.Queue;
import org.jtrim2.utils.ExceptionHelper;

final class WrapperPollingQueue<T> implements ReservablePollingQueue<T>, Serializable {
    private static final long serialVersionUID = 1L;

    private final Queue<T> impl;
    private final int maxCapacity;
    private final CounterRef reserveCountRef;

    public WrapperPollingQueue(Queue<T> impl, int maxCapacity) {
        this.impl = Objects.requireNonNull(impl, "impl");
        this.maxCapacity = ExceptionHelper.checkArgumentInRange(maxCapacity, 1, Integer.MAX_VALUE, "maxCapacity");
        this.reserveCountRef = new CounterRef(0);
    }

    @Override
    public boolean isEmpty() {
        return impl.isEmpty();
    }

    @Override
    public boolean isEmptyAndNoReserved() {
        return impl.isEmpty() && reserveCountRef.value <= 0;
    }

    @Override
    public boolean offer(T entry) {
        Objects.requireNonNull(entry, "entry");

        if (impl.size() + reserveCountRef.value >= maxCapacity) {
            return false;
        }

        return impl.offer(entry);
    }

    @Override
    public ReservedElementRef<T> pollButKeepReserved() {
        T returnedEntry = impl.poll();
        if (returnedEntry == null) {
            return null;
        }

        reserveCountRef.value++;
        return new QueueReservationRefImpl<>(reserveCountRef, returnedEntry);
    }

    @Override
    public T poll() {
        return impl.poll();
    }

    @Override
    public void clear() {
        impl.clear();
    }

    @Override
    public String toString() {
        return impl.toString();
    }

    private static class CounterRef implements Serializable {
        private static final long serialVersionUID = 1L;

        public int value;

        public CounterRef(int initialValue) {
            this.value = initialValue;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeInt(value);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            value = in.readInt();
        }
    }

    private static final class QueueReservationRefImpl<T> implements ReservedElementRef<T>, Serializable {
        private static final long serialVersionUID = 1L;

        private final CounterRef reserveCountRef;
        private final T element;
        private boolean released;

        public QueueReservationRefImpl(CounterRef reserveCountRef, T element) {
            this.reserveCountRef = reserveCountRef;
            this.element = element;
            this.released = false;
        }

        @Override
        public T element() {
            return element;
        }

        @Override
        public void release() {
            if (!released) {
                released = true;
                reserveCountRef.value--;
            }
        }
    }
}
