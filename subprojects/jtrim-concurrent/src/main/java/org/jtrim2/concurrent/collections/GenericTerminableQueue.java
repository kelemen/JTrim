package org.jtrim2.concurrent.collections;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.cancel.CancelableWaits;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.ReservablePollingQueue;
import org.jtrim2.collections.ReservedElementRef;

final class GenericTerminableQueue<T> implements TerminableQueue<T> {
    private final ReentrantLock queueLock;
    private final Condition checkNotEmptySignal;
    private final Condition checkNotFullSignal;
    private final Condition checkEmptySignal;
    private final ReservablePollingQueue<T> queue;
    private boolean closed;

    public GenericTerminableQueue(ReservablePollingQueue<T> queue) {
        this.queueLock = new ReentrantLock();
        this.checkNotEmptySignal = this.queueLock.newCondition();
        this.checkNotFullSignal = this.queueLock.newCondition();
        this.checkEmptySignal = this.queueLock.newCondition();
        this.queue = Objects.requireNonNull(queue, "queue");
        this.closed = false;
    }

    @Override
    public void put(CancellationToken cancelToken, T entry) throws TerminatedQueueException {
        tryPut(cancelToken, entry, EndlessSignalWaiter.ENDLESS_SIGNAL_WAITER);
    }

    @Override
    public boolean put(CancellationToken cancelToken, T entry, long timeout, TimeUnit timeoutUnit)
            throws TerminatedQueueException {

        return tryPut(cancelToken, entry, new TimeoutSignalWaiter(timeout, timeoutUnit));
    }

    private boolean tryPut(CancellationToken cancelToken, T entry, SignalWaiter waiter)
            throws TerminatedQueueException {

        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(entry, "entry");

        boolean added = false;

        queueLock.lock();
        try {
            while (!closed) {
                added = queue.offer(entry);
                if (added) {
                    checkNotEmptySignal.signal();
                    break;
                }

                if (!waiter.waitForSignal(cancelToken, checkNotFullSignal)) {
                    return false;
                }
            }
        } finally {
            queueLock.unlock();
        }

        if (!added) {
            throw TerminatedQueueException.withoutStackTrace();
        }
        return true;
    }

    @Override
    public ReservedElementRef<T> tryTakeButKeepReserved() throws TerminatedQueueException {
        ReservedElementRef<T> result;

        queueLock.lock();
        try {
            result = queue.pollButKeepReserved();
            if (result == null) {
                if (closed) {
                    throw TerminatedQueueException.withoutStackTrace();
                }
                return null;
            }
        } finally {
            queueLock.unlock();
        }

        return new QueueReservationRefImpl<>(result);
    }

    @Override
    public ReservedElementRef<T> tryTakeButKeepReserved(
            CancellationToken cancelToken,
            long timeout,
            TimeUnit timeoutUnit) throws TerminatedQueueException {

        return takeButReserve(cancelToken, new TimeoutSignalWaiter(timeout, timeoutUnit));
    }

    private ReservedElementRef<T> takeButReserve(CancellationToken cancelToken, SignalWaiter waiter)
            throws TerminatedQueueException {

        Objects.requireNonNull(cancelToken, "cancelToken");

        ReservedElementRef<T> result;

        queueLock.lock();
        try {
            while (true) {
                result = queue.pollButKeepReserved();
                if (result != null) {
                    break;
                }
                if (closed) {
                    throw TerminatedQueueException.withoutStackTrace();
                }

                if (!waiter.waitForSignal(cancelToken, checkNotEmptySignal)) {
                    return null;
                }
            }
        } finally {
            queueLock.unlock();
        }

        return new QueueReservationRefImpl<>(result);
    }

    @Override
    public void clear() {
        queueLock.lock();
        try {
            queue.clear();
            checkNotFullSignal.signalAll();
            checkEmptySignal.signalAll();
        } finally {
            queueLock.unlock();
        }
    }

    @Override
    public void shutdown() {
        queueLock.lock();
        try {
            shutdownUnlocked();
        } finally {
            queueLock.unlock();
        }
    }

    private void shutdownUnlocked() {
        assert queueLock.isHeldByCurrentThread();

        closed = true;
        checkNotEmptySignal.signalAll();
        checkNotFullSignal.signalAll();
    }

    @Override
    public void shutdownAndWaitUntilEmpty(CancellationToken cancelToken) {
        shutdownAndWaitUntilEmpty(cancelToken, EndlessSignalWaiter.ENDLESS_SIGNAL_WAITER);
    }

    @Override
    public boolean shutdownAndTryWaitUntilEmpty(CancellationToken cancelToken, long timeout, TimeUnit timeoutUnit) {
        return shutdownAndWaitUntilEmpty(cancelToken, new TimeoutSignalWaiter(timeout, timeoutUnit));
    }

    private boolean shutdownAndWaitUntilEmpty(CancellationToken cancelToken, SignalWaiter waiter) {
        queueLock.lock();
        try {
            shutdownUnlocked();

            while (!queue.isEmptyAndNoReserved()) {
                if (!waiter.waitForSignal(cancelToken, checkEmptySignal)) {
                    return false;
                }
            }
        } finally {
            queueLock.unlock();
        }
        return true;
    }

    private enum EndlessSignalWaiter implements SignalWaiter {
        ENDLESS_SIGNAL_WAITER;

        @Override
        public boolean waitForSignal(CancellationToken cancelToken, Condition signal) {
            cancelToken.checkCanceled();
            CancelableWaits.await(cancelToken, signal);
            return true;
        }
    }

    private static final class TimeoutSignalWaiter implements SignalWaiter {
        private final long startTime;
        private final long waitNanos;

        public TimeoutSignalWaiter(long timeout, TimeUnit timeoutUnit) {
            this(timeoutUnit.toNanos(timeout));
        }

        public TimeoutSignalWaiter(long waitNanos) {
            this.startTime = System.nanoTime();
            this.waitNanos = waitNanos;
        }

        @Override
        public boolean waitForSignal(CancellationToken cancelToken, Condition signal) {
            cancelToken.checkCanceled();
            long remainingNanos = waitNanos - (System.nanoTime() - startTime);
            if (remainingNanos <= 0) {
                return false;
            }
            return CancelableWaits.await(cancelToken, remainingNanos, TimeUnit.NANOSECONDS, signal);
        }
    }

    private interface SignalWaiter {
        public boolean waitForSignal(CancellationToken cancelToken, Condition signal);
    }

    private final class QueueReservationRefImpl<T> implements ReservedElementRef<T> {
        private final ReservedElementRef<T> wrapped;

        public QueueReservationRefImpl(ReservedElementRef<T> wrapped) {
            this.wrapped = Objects.requireNonNull(wrapped, "wrapped");
        }

        @Override
        public T element() {
            return wrapped.element();
        }

        @Override
        public void release() {
            queueLock.lock();
            try {
                wrapped.release();
                checkNotFullSignal.signal();
                if (queue.isEmptyAndNoReserved()) {
                    checkEmptySignal.signalAll();
                }
            } finally {
                queueLock.unlock();
            }
        }
    }
}
