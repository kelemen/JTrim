package org.jtrim.cancel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines static helper methods to wait for a cancellation request instead of
 * thread interruption.
 * <P>
 * Java usually employs thread interruption for canceling methods waiting for a
 * particular event. Since relying on thread interruption is error prone and
 * inconvenient to use, the <I>JTrim</I> library uses {@link CancellationToken}
 * to detect that an operation has been canceled. This library contains methods
 * to convert methods which can be canceled by thread interruption to a method
 * call which can be canceled using a {@code CancellationToken}.
 * <P>
 * The order of the arguments of these methods are always as follows:
 * <ol>
 *  <li>
 *   The {@code CancellationToken}.
 *  </li>
 *  <li>
 *   Optionally a timeout value and its {@code TimeUnit} specifying the maximum
 *   time to wait.
 *  </li>
 *  <li>
 *   The object whose "await" method is to be called to wait for a particular
 *   event. The method of the object might not actually be called "await".
 *   Every method states in its documentation which method it actually calls.
 *  </li>
 * </ol>
 * <P>
 * The two general purpose method is which takes an {@link InterruptibleWait}
 * as their last argument and this {@code InterruptibleWait} must actually be
 * implemented to wait for the desired condition.
 * <P>
 * The methods of this class do add some additional performance overhead to the
 * wait so it is advised to first check the condition to be waited for before
 * actually calling a method of this class.
 * <P>
 * <B>Warning</B>: Every methods in this class will clear the interrupted status
 * of the task before they throw a {@link OperationCanceledException}. If they
 * return normally, they will leave the interrupted status of thread as it was.
 *
 * <h3>Thread safety</h3>
 * Every method of this class can be called from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Every method of this class has the same synchronization transparency property
 * as the "await" method of the object passed to them.
 *
 * @see CancelableWaits#await(CancellationToken, InterruptibleWait)
 * @see CancelableWaits#await(CancellationToken, long, TimeUnit, InterruptibleLimitedWait)
 * @see CancellationToken
 *
 * @author Kelemen Attila
 */
public final class CancelableWaits {
    /**
     * Calls {@code lock.lockInterruptibly()} and waits until it returns or the
     * specified {@code CancellationToken} signals that the waiting must be
     * canceled. That is, if {@code lock.lockInterruptibly()} throws an
     * {@code InterruptedException}, the {@code lock.lockInterruptibly()} method
     * will be called again.
     * <P>
     * Whenever the specified {@code CancellationToken} signals a cancellation
     * request, the ongoing {@code lock.lockInterruptibly()} call will be
     * interrupted using thread interruption. The
     * {@code lock.lockInterruptibly()} call must return by throwing an
     * {@code InterruptedException} so that this method may clear the
     * interrupted status of the current thread and return by throwing a
     * {@link OperationCanceledException}.
     * <P>
     * Note that thread interruption may occur due to reasons uncontrolled by
     * this method, so {@code lock.lockInterruptibly()} calls may be interrupted
     * spuriously. In such spurious interrupt will the
     * {@code lock.lockInterruptibly()} method be called again.
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if this operation must be canceled and a {@link OperationCanceledException}
     *   should be thrown. This argument cannot be {@code null}.
     * @param lock the {@code Lock} object whose {@code lockInterruptibly()}
     *   method is to be invoked. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws OperationCanceledException thrown if the specified
     *   {@code CancellationToken} signals a cancellation request before
     *   {@code lock.lockInterruptibly()} returns
     */
    public static void lock(CancellationToken cancelToken, final Lock lock) {
        ExceptionHelper.checkNotNullArgument(lock, "lock");

        await(cancelToken, new InterruptibleWait() {
            @Override
            public void await() throws InterruptedException {
                lock.lockInterruptibly();
            }
        });
    }

    /**
     * Calls {@code lock.tryLock(long, TimeUnit)} with the specified timeout
     * value and waits until it returns or the specified
     * {@code CancellationToken} signals that the waiting must be canceled. That
     * is, if {@code lock.tryLock(long, TimeUnit)} throws an
     * {@code InterruptedException}, the {@code lock.tryLock(long, TimeUnit)}
     * method will be called again with an appropriately lowered timeout value.
     * <P>
     * Whenever the specified {@code CancellationToken} signals a cancellation
     * request, the ongoing {@code lock.tryLock(long, TimeUnit)} call will be
     * interrupted using thread interruption. The
     * {@code lock.tryLock(long, TimeUnit)} call must return by throwing an
     * {@code InterruptedException} so that this method may clear the
     * interrupted status of the current thread and return by throwing a
     * {@link OperationCanceledException}.
     * <P>
     * Note that thread interruption may occur due to reasons uncontrolled by
     * this method, so {@code lock.tryLock(long, TimeUnit)} calls may be
     * interrupted spuriously. In such spurious interrupt will the
     * {@code lock.tryLock(long, TimeUnit)} method be called again.
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if this operation must be canceled and a {@link OperationCanceledException}
     *   should be thrown. This argument cannot be {@code null}.
     * @param timeout the maximum time to wait in the given time unit. After
     *   this time elapses, this method returns by throwing a
     *   {@link OperationCanceledException}. This argument must be greater than or
     *   equal to zero.
     * @param timeUnit the time unit of the {@code timeout} argument. This
     *   argument cannot be {@code null}.
     * @param lock the {@code Lock} object whose
     *   {@code tryLock(long, TimeUnit)} method is to be invoked. This argument
     *   cannot be {@code null}.
     * @return the return value of the {@code lock.tryLock(long, TimeUnit)}
     *   method call. The return value of {@code false} means that the lock
     *   was not acquired.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws IllegalArgumentException thrown if the specified timeout value is
     *   lower than zero
     * @throws OperationCanceledException thrown if the specified
     *   {@code CancellationToken} signals a cancellation request before
     *   {@code lock.tryLock(long, TimeUnit)} returns
     */
    public static boolean tryLock(
            CancellationToken cancelToken,
            long timeout,
            TimeUnit timeUnit,
            final Lock lock) {
        ExceptionHelper.checkNotNullArgument(lock, "lock");

        return await(cancelToken, timeout, timeUnit, new InterruptibleLimitedWait() {
            @Override
            public boolean await(long nanosToWait) throws InterruptedException {
                return lock.tryLock(nanosToWait, TimeUnit.NANOSECONDS);
            }
        });
    }

    /**
     * Causes the current thread to sleep until the given time elapses or the
     * specified {@code CancellationToken} signals a cancellation request. If
     * the specified time elapses, this method returns by throwing a
     * {@link OperationCanceledException}.
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if this sleep must be canceled and a {@link OperationCanceledException}
     *   should be thrown. This argument cannot be {@code null}.
     * @param time the time to wait in the given time unit. This argument must
     *   be greater than or equal to zero.
     * @param timeUnit the time unit of the {@code time} argument. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws IllegalArgumentException thrown if the specified time is lower
     *   than zero
     * @throws OperationCanceledException thrown if the specified
     *   {@code CancellationToken} signals a cancellation request before the
     *   specified time elapses
     */
    public static void sleep(CancellationToken cancelToken,
            long time,
            TimeUnit timeUnit) {
        await(cancelToken, time, timeUnit, new InterruptibleLimitedWait() {
            @Override
            public boolean await(long nanosToWait) throws InterruptedException {
                TimeUnit.NANOSECONDS.sleep(nanosToWait);
                return true;
            }
        });
    }

    /**
     * Calls {@code executor.awaitTermination(long, TimeUnit)} with the
     * specified timeout value and waits until it returns or the specified
     * {@code CancellationToken} signals that the waiting must be canceled. That
     * is, if {@code executor.awaitTermination(long, TimeUnit)} throws an
     * {@code InterruptedException}, the
     * {@code executor.awaitTermination(long, TimeUnit)} method will be called
     * again with an appropriately lowered timeout value.
     * <P>
     * Whenever the specified {@code CancellationToken} signals a cancellation
     * request, the ongoing {@code executor.awaitTermination(long, TimeUnit)}
     * call will be interrupted using thread interruption. The
     * {@code executor.awaitTermination(long, TimeUnit)} call must return by
     * throwing an {@code InterruptedException} so that this method may clear
     * the interrupted status of the current thread and return by throwing a
     * {@link OperationCanceledException}.
     * <P>
     * Note that thread interruption may occur due to reasons uncontrolled by
     * this method, so {@code executor.awaitTermination(long, TimeUnit)} calls
     * may be interrupted spuriously. In such spurious interrupt will the
     * {@code executor.awaitTermination(long, TimeUnit)} method be called again.
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if this operation must be canceled and a {@link OperationCanceledException}
     *   should be thrown. This argument cannot be {@code null}.
     * @param timeout the maximum time to wait in the given time unit. After
     *   this time elapses, this method returns by throwing a
     *   {@link OperationCanceledException}. This argument must be greater than or
     *   equal to zero.
     * @param timeUnit the time unit of the {@code timeout} argument. This
     *   argument cannot be {@code null}.
     * @param executor the {@code ExecutorService} object whose
     *   {@code awaitTermination(long, TimeUnit)} method is to be invoked.
     *   This argument cannot be {@code null}.
     * @return the return value of the
     *   {@code executor.awaitTermination(long, TimeUnit)} method call. The
     *   return value of {@code false} means that the timeout elapsed without
     *   the executor terminating.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws IllegalArgumentException thrown if the specified timeout value is
     *   lower than zero
     * @throws OperationCanceledException thrown if the specified
     *   {@code CancellationToken} signals a cancellation request before
     *   {@code executor.awaitTermination(long, TimeUnit)} returns
     */
    public static boolean awaitTerminate(
            CancellationToken cancelToken,
            long timeout,
            TimeUnit timeUnit,
            final ExecutorService executor) {
        ExceptionHelper.checkNotNullArgument(executor, "condition");

        return await(cancelToken, timeout, timeUnit, new InterruptibleLimitedWait() {
            @Override
            public boolean await(long nanosToWait) throws InterruptedException {
                return executor.awaitTermination(nanosToWait, TimeUnit.NANOSECONDS);
            }
        });
    }

    /**
     * Calls {@code condition.await(long, TimeUnit)} with the specified timeout
     * value and waits until it returns or the specified
     * {@code CancellationToken} signals that the waiting must be canceled. That
     * is, if {@code condition.await(long, TimeUnit)} throws an
     * {@code InterruptedException}, the {@code condition.await(long, TimeUnit)}
     * method will be called again with an appropriately lowered timeout value.
     * <P>
     * Whenever the specified {@code CancellationToken} signals a cancellation
     * request, the ongoing {@code condition.await(long, TimeUnit)} call will be
     * interrupted using thread interruption. The
     * {@code condition.await(long, TimeUnit)} call must return by throwing an
     * {@code InterruptedException} so that this method may clear the
     * interrupted status of the current thread and return by throwing a
     * {@link OperationCanceledException}.
     * <P>
     * Note that thread interruption may occur due to reasons uncontrolled by
     * this method, so {@code condition.await(long, TimeUnit)} calls may be
     * interrupted spuriously. In such spurious interrupt will the
     * {@code condition.await(long, TimeUnit)} method be called again.
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if this operation must be canceled and a {@link OperationCanceledException}
     *   should be thrown. This argument cannot be {@code null}.
     * @param timeout the maximum time to wait in the given time unit. After
     *   this time elapses, this method returns by throwing a
     *   {@link OperationCanceledException}. This argument must be greater than or
     *   equal to zero.
     * @param timeUnit the time unit of the {@code timeout} argument. This
     *   argument cannot be {@code null}.
     * @param condition the {@code Condition} object whose
     *   {@code await(long, TimeUnit)} method is to be invoked. This argument
     *   cannot be {@code null}.
     * @return the return value of the {@code condition.await(long, TimeUnit)}
     *   method call. The return value of {@code false} means that the timeout
     *   elapsed without actually receiving the signal.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws IllegalArgumentException thrown if the specified timeout value is
     *   lower than zero
     * @throws OperationCanceledException thrown if the specified
     *   {@code CancellationToken} signals a cancellation request before
     *   {@code condition.await(long, TimeUnit)} returns
     */
    public static boolean await(
            CancellationToken cancelToken,
            long timeout,
            TimeUnit timeUnit,
            final Condition condition) {

        // timeUnit is checked by "await"
        // cancelToken is checked by "await"
        ExceptionHelper.checkNotNullArgument(condition, "condition");

        return await(cancelToken, timeout, timeUnit, new InterruptibleLimitedWait() {
            @Override
            public boolean await(long nanosToWait) throws InterruptedException {
                return condition.await(nanosToWait, TimeUnit.NANOSECONDS);
            }
        });
    }

    /**
     * Calls {@code condition.await()} and waits until it returns or the
     * specified {@code CancellationToken} signals that the waiting must be
     * canceled. That is, if {@code condition.await()} throws an
     * {@code InterruptedException}, the {@code condition.await()} method will
     * be called again.
     * <P>
     * Whenever the specified {@code CancellationToken} signals a cancellation
     * request, the ongoing {@code condition.await()} call will be interrupted
     * using thread interruption. The {@code condition.await()} call must return
     * by throwing an {@code InterruptedException} so that this method may clear
     * the interrupted status of the current thread and return by throwing a
     * {@link OperationCanceledException}.
     * <P>
     * Note that thread interruption may occur due to reasons uncontrolled by
     * this method, so {@code condition.await()} calls may be interrupted
     * spuriously. In such spurious interrupt will the {@code condition.await()}
     * method be called again.
     * <P>
     * <B>Warning</B>: Note that the {@code condition.await()} method may return
     * spuriously. That is, without any reason (as documented in its apidoc).
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if this operation must be canceled and a {@link OperationCanceledException}
     *   should be thrown. This argument cannot be {@code null}.
     * @param condition the {@code Condition} object whose {@code await()}
     *   method is to be invoked. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws OperationCanceledException thrown if the specified
     *   {@code CancellationToken} signals a cancellation request before
     *   {@code condition.await()}returns
     */
    public static void await(
            CancellationToken cancelToken,
            final Condition condition) {

        // cancelToken is checked by "await"
        ExceptionHelper.checkNotNullArgument(condition, "condition");

        await(cancelToken, new InterruptibleWait() {
            @Override
            public void await() throws InterruptedException {
                condition.await();
            }
        });
    }

    /**
     * Calls {@code wait.await} with the specified timeout value and waits until
     * it returns or the specified {@code CancellationToken} signals that the
     * waiting must be canceled. That is, if {@code wait.await} throws an
     * {@code InterruptedException}, the {@code wait.await} method will be
     * called again with an appropriately lowered timeout value.
     * <P>
     * Whenever the specified {@code CancellationToken} signals a cancellation
     * request, the ongoing {@code wait.await} call will be interrupted using
     * thread interruption. The {@code wait.await} call must return by throwing
     * an {@code InterruptedException} so that this method may clear the
     * interrupted status of the current thread and return by throwing a
     * {@link OperationCanceledException}.
     * <P>
     * Note that thread interruption may occur due to reasons uncontrolled by
     * this method, so {@code wait.await} calls may be interrupted spuriously.
     * In such spurious interrupt will the {@code wait.await} method be called
     * again.
     * <P>
     * This is a general purpose method for converting cancellation by thread
     * interruption to cancellation by a {@link CancellationToken} and waiting
     * for only a limited amount of time.
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if this operation must be canceled and a {@link OperationCanceledException}
     *   should be thrown. This argument cannot be {@code null}.
     * @param timeout the maximum time to wait in the given time unit. After
     *   this time elapses, this method returns by throwing a
     *   {@link OperationCanceledException}. This argument must be greater than or
     *   equal to zero.
     * @param timeUnit the time unit of the {@code timeout} argument. This
     *   argument cannot be {@code null}.
     * @param wait the {@code InterruptibleWait} object whose {@code await}
     *   method is to be called to wait for a particular event. This argument
     *   cannot be {@code null}.
     * @return the return value of the {@code wait.await} method call. Usually
     *   a return value of {@code false} means that the timeout elapsed without
     *   the event to be waited for actually occurred.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws IllegalArgumentException thrown if the specified timeout value is
     *   lower than zero
     * @throws OperationCanceledException thrown if the specified
     *   {@code CancellationToken} signals a cancellation request before
     *   {@code wait.await} returns
     */
    public static boolean await(
            CancellationToken cancelToken,
            long timeout,
            TimeUnit timeUnit,
            final InterruptibleLimitedWait wait) {
        // cancelToken is checked by "await/2"
        ExceptionHelper.checkArgumentInRange(timeout, 0, Long.MAX_VALUE, "timeout");
        ExceptionHelper.checkNotNullArgument(timeUnit, "timeUnit");
        ExceptionHelper.checkNotNullArgument(wait, "wait");

        final long startTime = System.nanoTime();
        final long timeoutNanos = TimeUnit.NANOSECONDS.convert(timeout, timeUnit);

        final BooleanRef signaled = new BooleanRef();
        await(cancelToken, new InterruptibleWait() {
            @Override
            public void await() throws InterruptedException {
                long toWaitNanos = Math.max(timeoutNanos - (System.nanoTime() - startTime), 0);
                signaled.value = wait.await(toWaitNanos);
            }
        });
        return signaled.value;
    }

    /**
     * Waits until {@code wait.await} returns or the specified
     * {@code CancellationToken} signals that the waiting must be canceled.
     * That is, if {@code wait.await} throws an {@code InterruptedException},
     * the {@code wait.await} method will be called again.
     * <P>
     * Whenever the specified {@code CancellationToken} signals a cancellation
     * request, the ongoing {@code wait.await} call will be interrupted using
     * thread interruption. The {@code wait.await} call must return by throwing
     * an {@code InterruptedException} so that this method may clear the
     * interrupted status of the current thread and return by throwing a
     * {@link OperationCanceledException}.
     * <P>
     * Note that thread interruption may occur due to reasons uncontrolled by
     * this method, so {@code wait.await} calls may be interrupted spuriously.
     * In such spurious interrupt will the {@code wait.await} method be called
     * again.
     * <P>
     * This is a general purpose method for converting cancellation by thread
     * interruption to cancellation by a {@link CancellationToken}.
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if this operation must be canceled and a {@link OperationCanceledException}
     *   should be thrown. This argument cannot be {@code null}.
     * @param wait the {@code InterruptibleWait} object whose {@code await}
     *   method is to be called to wait for a particular event. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws OperationCanceledException thrown if the specified
     *   {@code CancellationToken} signals a cancellation request before
     *   {@code wait.await} returns
     */
    public static void await(CancellationToken cancelToken, InterruptibleWait wait) {
        // cancelToken is always dereferenced
        ExceptionHelper.checkNotNullArgument(wait, "wait");

        ThreadInterrupter interrupter = new ThreadInterrupter(Thread.currentThread());
        ListenerRef listenerRef = cancelToken.addCancellationListener(interrupter);
        boolean interrupted = false;
        try {
            while (true) {
                if (cancelToken.isCanceled()) {
                    interrupted = false;
                    Thread.interrupted(); // clean interrupted status
                    throw new OperationCanceledException();
                }

                try {
                    wait.await();
                    return;
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

    private static class BooleanRef {
        public boolean value;
    }

    private CancelableWaits() {
        throw new AssertionError();
    }
}
