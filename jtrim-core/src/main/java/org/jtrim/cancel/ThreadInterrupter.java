package org.jtrim.cancel;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Kelemen Attila
 */
final class ThreadInterrupter implements Runnable {
    private final Lock mainLock;
    private Thread thread;

    public ThreadInterrupter(Thread thread) {
        assert thread != null;
        this.mainLock = new ReentrantLock();
        this.thread = thread;
    }

    public void stopInterrupt() {
        mainLock.lock();
        try {
            thread = null;
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public void run() {
        mainLock.lock();
        try {
            Thread currentThread = thread;
            if (currentThread != null) {
                currentThread.interrupt();
            }
        } finally {
            mainLock.unlock();
        }
    }

}
