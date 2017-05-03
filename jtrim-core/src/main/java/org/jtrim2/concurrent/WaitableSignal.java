package org.jtrim2.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.cancel.CancelableWaits;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines thread-safe signal for which threads can wait. That is, initially
 * all {@code WaitableSignal} is in the non-signaled state but after invoking
 * the {@link #signal()} method, it will permanently enter the signaled state.
 * Other threads can wait for this signal by calling the
 * {@link #waitSignal(CancellationToken) waitSignal} or the
 * {@link #tryWaitSignal(CancellationToken, long, TimeUnit) tryWaitSignal}
 * method.
 * <P>
 * Note that this class is similar to a
 * {@code java.util.concurrent.CountDownLatch} with one as the initial "count"
 * but this implementation is simpler and relies on
 * {@link org.jtrim2.cancel.OperationCanceledException} rather than on thread
 * interrupts.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not <I>synchronization transparent</I>.
 *
 * @author Kelemen Attila
 */
public final class WaitableSignal {
    /**
     * A {@code WaitableSignal} which is already in the signaling state. That
     * is, calling any of its wait method is effectively a no-op.
     */
    public static final WaitableSignal SIGNALING_SIGNAL = newSignalingSignal();

    private final Lock lock;
    private final Condition waitSignal;
    private volatile boolean signaled;

    /**
     * Creates a new {@code WaitableSignal} in the non-signaled state.
     */
    public WaitableSignal() {
        this.lock = new ReentrantLock();
        this.waitSignal = lock.newCondition();
        this.signaled = false;
    }

    private static WaitableSignal newSignalingSignal() {
        WaitableSignal result = new WaitableSignal();
        result.signal();
        return result;
    }

    /**
     * Sets the state of this {@code WaitableSignal} to the signaled state and
     * allows the {@code waitSignal} or the {@code tryWaitSignal} method to
     * return immediately.
     * <P>
     * This method is idempotent. That is, calling this method multiple times
     * has no further effect.
     */
    public void signal() {
        if (!signaled) {
            signaled = true;
            lock.lock();
            try {
                waitSignal.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Returns {@code true} if {@link #signal()} has been already called on this
     * {@code WaitableSignal} object.
     * <P>
     * If this method returns {@code true}, subsequent {@code waitSignal} or
     * {@code tryWaitSignal} method calls will immediately return without
     * waiting (or throwing an exception).
     *
     * @return {@code true} if {@link #signal()} has been already called on this
     *   {@code WaitableSignal} object, {@code false} otherwise
     */
    public boolean isSignaled() {
        return signaled;
    }

    /**
     * Waits until another thread invokes the {@link #signal()} method or until
     * the specified {@code CancellationToken} signals a cancellation request.
     * <P>
     * Note that if the {@code signal()} method has been called prior to this
     * {@code waitSignal} method call, this method will always return
     * immediately without throwing an exception (even if the
     * {@code CancellationToken} signals a cancellation request).
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   for cancellation request. A cancellation request will cause this method
     *   to throw an {@code OperationCanceledException} exception. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code CancellationToken} is {@code null}
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if the
     *   specified {@code CancellationToken} signals a cancellation request
     *   before {@code signal()} has been called.
     */
    public void waitSignal(CancellationToken cancelToken) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");

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

    /**
     * Waits until another thread invokes the {@link #signal()} method or until
     * the specified {@code CancellationToken} signals a cancellation request or
     * until the specified timeout elapses.
     * <P>
     * Note that if the {@code signal()} method has been called prior to this
     * {@code tryWaitSignal} method call, this method will always return with
     * {@code true} immediately without throwing an exception (even if the
     * {@code CancellationToken} signals a cancellation request).
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   for cancellation request. A cancellation request will cause this method
     *   to throw an {@code OperationCanceledException} exception. This argument
     *   cannot be {@code null}.
     * @param timeout the maximum time to wait for the signal in the given time
     *   unit before returning. If the timeout elapses, this method will return
     *   with {@code false}. This argument must be greater than or equal to
     *   zero.
     * @param timeUnit the time unit of the {@code timeout} argument. This
     *   argument cannot be {@code null}
     * @return {@code true} if this method has detected that the
     *   {@code signal()} method has been called, {@code false} if the specified
     *   timeout elapsed first.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code CancellationToken} or {@code TimeUnit} is {@code null}
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if the
     *   specified {@code CancellationToken} signals a cancellation request
     *   before {@code signal()} has been called.
     */
    public boolean tryWaitSignal(CancellationToken cancelToken,
            long timeout, TimeUnit timeUnit) {
        ExceptionHelper.checkNotNullArgument(cancelToken, "cancelToken");
        ExceptionHelper.checkArgumentInRange(timeout, 0, Long.MAX_VALUE, "timeout");
        ExceptionHelper.checkNotNullArgument(timeUnit, "timeUnit");

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
