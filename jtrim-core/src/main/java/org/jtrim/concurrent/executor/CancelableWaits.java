package org.jtrim.concurrent.executor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class CancelableWaits {
    public static boolean awaitCondition(
            CancellationToken cancelToken,
            long timeout,
            TimeUnit timeUnit,
            final Condition condition) {

        // timeUnit is checked by "await"
        // cancelToken is checked by "await"
        ExceptionHelper.checkNotNullArgument(condition, "condition");

        return await(cancelToken, timeout, timeUnit, new InterruptibleWait() {
            @Override
            public boolean await(long nanosToWait) throws InterruptedException {
                return condition.await(nanosToWait, TimeUnit.NANOSECONDS);
            }
        });
    }

    public static void awaitCondition(
            CancellationToken cancelToken,
            final Condition condition) {

        // cancelToken is checked by "await"
        ExceptionHelper.checkNotNullArgument(condition, "condition");

        await(cancelToken, new InterruptibleWait() {
            @Override
            public boolean await(long nanosToWait) throws InterruptedException {
                condition.await();
                return true;
            }
        });
    }

    public static boolean await(
            CancellationToken cancelToken,
            long timeout,
            TimeUnit timeUnit,
            InterruptibleWait wait) {
        // cancelToken is checked by "await/2"
        // timeUnit is always dereferenced
        ExceptionHelper.checkNotNullArgument(wait, "wait");

        TimeoutWait timeoutWait = new TimeoutWait(timeUnit.toNanos(timeout), wait);
        await(cancelToken, timeoutWait);
        return !timeoutWait.isTimeout();
    }

    public static void await(CancellationToken cancelToken, InterruptibleWait wait) {
        ThreadInterrupter interrupter = new ThreadInterrupter(Thread.currentThread());
        ListenerRef<?> listenerRef = cancelToken.addCancellationListener(interrupter);
        boolean interrupted = false;
        try {
            while (true) {
                if (cancelToken.isCanceled()) {
                    interrupted = false;
                    Thread.interrupted(); // clean interrupted status
                    throw new TaskCanceledException();
                }

                try {
                    while (!wait.await(Long.MAX_VALUE)) {
                        // Do nothing just wait
                    }
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }
        } finally {
            try {
                interrupter.stopInterrupt();
                listenerRef.unregister();
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static class ThreadInterrupter implements Runnable {
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

    private CancelableWaits() {
        throw new AssertionError();
    }

    private static class TimeoutWait implements InterruptibleWait {
        private final long timeoutNanos;
        private final long startTime;
        private boolean timeout;
        private final InterruptibleWait waitCondition;

        public TimeoutWait(long timeoutNanos, InterruptibleWait waitCondition) {
            this.startTime = System.nanoTime();
            this.timeoutNanos = timeoutNanos;
            this.waitCondition = waitCondition;
        }

        private long getWaitTimeNanos() {
            return timeoutNanos - (System.nanoTime() - startTime);
        }

        @Override
        public boolean await(long ignored) throws InterruptedException {
            long timeToWait = Math.max(getWaitTimeNanos(), 0);
            timeout = !waitCondition.await(timeToWait);
            return !timeout;
        }

        public boolean isTimeout() {
            return timeout;
        }
    }
}
