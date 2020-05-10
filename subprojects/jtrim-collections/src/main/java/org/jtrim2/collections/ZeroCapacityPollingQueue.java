package org.jtrim2.collections;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Objects;

final class ZeroCapacityPollingQueue<T> implements ReservablePollingQueue<T>, Serializable {
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isEmptyAndNoReserved() {
        return true;
    }

    @Override
    public boolean offer(T entry) {
        Objects.requireNonNull(entry, "entry");
        return false;
    }

    @Override
    public ReservedElementRef<T> pollButKeepReserved() {
        return null;
    }

    @Override
    public T poll() {
        return null;
    }

    @Override
    public String toString() {
        return "[]";
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    }

    private Object readResolve() throws ObjectStreamException {
        return ReservablePollingQueues.zeroCapacityQueue();
    }
}
