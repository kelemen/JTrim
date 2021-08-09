package org.jtrim2.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

final class SingleEntryPollingQueue<T> implements ReservablePollingQueue<T>, Serializable {
    private static final long serialVersionUID = 1L;

    private T queuedEntry;
    private final BooleanRef reservedRef;

    public SingleEntryPollingQueue() {
        this.queuedEntry = null;
        this.reservedRef = new BooleanRef(false);
    }

    @Override
    public boolean isEmpty() {
        return queuedEntry == null;
    }

    @Override
    public boolean isEmptyAndNoReserved() {
        return queuedEntry == null && !reservedRef.value;
    }

    @Override
    public boolean offer(T entry) {
        Objects.requireNonNull(entry, "entry");

        if (!isEmptyAndNoReserved()) {
            return false;
        }

        queuedEntry = entry;
        return true;
    }

    @Override
    public ReservedElementRef<T> pollButKeepReserved() {
        T returnedEntry = queuedEntry;
        if (returnedEntry == null) {
            return null;
        }

        reservedRef.value = true;
        queuedEntry = null;

        return new QueueReservationRefImpl<>(reservedRef, returnedEntry);
    }

    @Override
    public T poll() {
        T result = queuedEntry;
        queuedEntry = null;
        return result;
    }

    @Override
    public void clear() {
        poll();
    }

    @Override
    public String toString() {
        T currentEntry = queuedEntry;
        if (currentEntry == null) {
            return "[]";
        } else {
            return "[" + queuedEntry + "]";
        }
    }

    private static class BooleanRef implements Serializable {
        private static final long serialVersionUID = 1L;

        public boolean value;

        public BooleanRef(boolean value) {
            this.value = value;
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeBoolean(value);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            value = in.readBoolean();
        }
    }

    private static final class QueueReservationRefImpl<T> implements ReservedElementRef<T>, Serializable {
        private static final long serialVersionUID = 1L;

        private final BooleanRef reservedRef;
        private final T element;
        private boolean released;

        public QueueReservationRefImpl(BooleanRef reservedRef, T element) {
            this.reservedRef = reservedRef;
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
                reservedRef.value = false;
            }
        }
    }
}
