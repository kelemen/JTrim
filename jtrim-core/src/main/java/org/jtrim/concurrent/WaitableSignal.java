package org.jtrim.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.cancel.CancelableWaits;
import org.jtrim.cancel.CancellationToken;

/**
 *
 * @author Kelemen Attila
 */
public final class WaitableSignal {
    private final Lock lock;
    private final Condition waitSignal;
    private volatile boolean signaled;

    public WaitableSignal() {
        this.lock = new ReentrantLock();
        this.waitSignal = lock.newCondition();
        this.signaled = false;
    }

    public void signal() {
        signaled = true;
        lock.lock();
        try {
            waitSignal.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public boolean isSignaled() {
        return signaled;
    }

    public void waitSignal(CancellationToken cancelToken) {
        if (signaled) {
            return;
        }

        lock.lock();
        try {
            while (!signaled) {
                CancelableWaits.await(cancelToken, waitSignal);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean waitSignal(CancellationToken cancelToken,
            long timeout, TimeUnit timeUnit) {

        if (signaled) {
            return true;
        }

        long timeoutNanos = timeUnit.toNanos(timeout);
        long startTime = System.nanoTime();
        lock.lock();
        try {
            while (!signaled) {
                long elapsed = System.nanoTime() - startTime;
                long timeToWait = timeoutNanos - elapsed;
                if (timeToWait <= 0) {
                    return false;
                }
                CancelableWaits.await(cancelToken,
                        timeToWait, TimeUnit.NANOSECONDS, waitSignal);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }
}
