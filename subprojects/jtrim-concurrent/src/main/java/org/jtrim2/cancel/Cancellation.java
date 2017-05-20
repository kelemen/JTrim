package org.jtrim2.cancel;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.UnregisteredListenerRef;

/**
 * Contains static helper methods and fields related cancellation.
 */
public final class Cancellation {
    /**
     * A {@link CancellationToken} which can never be in the canceled state.
     * The {@link CancellationToken#isCanceled() isCanceled} method of
     * {@code UNCANCELABLE_TOKEN} will always return {@code false} when checked.
     * If a listener is registered with this token to be notified of
     * cancellation requests, this {@code CancellationToken} will do nothing but
     * return an already unregistered {@code ListenerRef}.
     */
    public static final CancellationToken UNCANCELABLE_TOKEN = UncancelableToken.INSTANCE;

    /**
     * A {@link CancellationToken} which is already in the canceled state.
     * The {@link CancellationToken#isCanceled() isCanceled} method of
     * {@code UNCANCELABLE_TOKEN} will always return {@code true} when checked.
     * If a listener is registered with this token to be notified of
     * cancellation requests, this {@code CancellationToken} will immediately
     * notify the listener and return an already unregistered
     * {@code ListenerRef}.
     * <P>
     * If you need a {@link CancellationController} as well, use the
     * {@link #DO_NOTHING_CONTROLLER}.
     *
     * @see #DO_NOTHING_CONTROLLER
     */
    public static final CancellationToken CANCELED_TOKEN = CanceledToken.INSTANCE;

    /**
     * A {@link CancellationController} which does nothing when calling
     * {@code cancel}.
     * <P>
     * This {@code CancellationController} is good for tasks, cannot be canceled
     * or tasks already canceled.
     *
     * @see #CANCELED_TOKEN
     */
    public static final CancellationController DO_NOTHING_CONTROLLER = DoNothingController.INSTANCE;

    /**
     * Creates a new {@code CancellationSource} whose {@link CancellationToken}
     * is not yet in the canceled state. The only possible way to make the
     * {@code CancellationToken} of the returned {@code CancellationSource}
     * signal cancellation request is to cancel the
     * {@link CancellationController} of the returned
     * {@code CancellationSource}.
     *
     * @return a new {@code CancellationSource} whose {@link CancellationToken}
     *   is not yet in the canceled state. This method never returns
     *   {@code null}.
     */
    public static CancellationSource createCancellationSource() {
        return new SimpleCancellationSource();
    }

    /**
     * Creates a new {@code CancellationSource} which will be notified of the
     * cancellation requests of the specified {@code CancellationToken}.
     * That is, if the {@code CancellationToken} specified in the argument is
     * canceled, the returned {@code CancellationToken} will be canceled as
     * well. Of course, the returned {@code CancellationSource} can also be
     * canceled by its own {@code CancellationController}
     *
     * @param cancelToken the {@code CancellationToken}, which when canceled,
     *   will cause the returned {@code CancellationSource} to be canceled. This
     *   argument cannot be {@code null}.
     * @return the new {@code ChildCancellationSource} which will be notified
     *   of the cancellation requests of the specified
     *   {@code CancellationToken}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public static CancellationSource createChildCancellationSource(
            CancellationToken cancelToken) {
        return new SimpleChildCancellationSource(cancelToken);
    }

    /**
     * Returns a {@code CancellationToken} which signals cancellation if and
     * only if at least one of the specified tokens are in canceled state.
     * <P>
     * If you do not specify any tokens, the returned token will never be in
     * the canceled state.
     *
     * @param tokens the array of {@code CancellationToken} checked for
     *   cancellation. This array might be empty but cannot contain {@code null}
     *   elements.
     * @return the {@code CancellationToken} which signals cancellation if and
     *   only if at least one of the specified tokens is in canceled state. This
     *   method never returns {@code null}.
     */
    public static CancellationToken anyToken(CancellationToken... tokens) {
        return new CombinedTokenAny(tokens);
    }

    /**
     * Returns a {@code CancellationToken} which signals cancellation if and
     * only if all of the specified tokens are in canceled state.
     * <P>
     * If you do not specify any tokens, the returned token will always be in
     * the canceled state.
     *
     * @param tokens the array of {@code CancellationToken} checked for
     *   cancellation. This array might be empty but cannot contain {@code null}
     *   elements.
     * @return the {@code CancellationToken} which signals cancellation if and
     *   only if all of the specified tokens is in canceled state. This
     *   method never returns {@code null}.
     */
    public static CancellationToken allTokens(CancellationToken... tokens) {
        return new CombinedTokenAll(tokens);
    }

    /**
     * Adds a {@link CancellationToken#addCancellationListener(Runnable) cancellation listener}
     * to the specified {@code CancellationToken} and returns reference which
     * can be used to remove the listener and wait until it has been removed.
     * <P>
     * <B>Warning</B>: It is forbidden to call any of the {@code close} methods
     * from within the added listener. Attempting to do so will result in an
     * {@code IllegalStateException} to be thrown by the {@code close} method.
     * <P>
     * Calling the {@code unregisterAndWait} method of the returned reference
     * ensures the following:
     * <ul>
     *  <li>
     *   After the {@code unregisterAndWait} methods returns normally (without
     *   throwing an exception), the listener is guaranteed not to be executed
     *   anymore.
     *  </li>
     *  <li>
     *   Calling the {@code unregisterAndWait} method (with valid argument)
     *   ensures that the added listener will be unregistered. This is
     *   {@code true} even if the {@code unregisterAndWait} method gets canceled.
     *  </li>
     *  <li>
     *   If the passed listener is <I>synchronization transparent</I>, then
     *   the {@code unregisterAndWait} method is
     *   <I>synchronization transparent</I> as well.
     *  </li>
     * </ul>
     *
     * Here is an example usage:
     * <pre>{@code
     * CancellationToken cancelToken = ...;
     * WaitableListenerRef ref = listenerForCancellation(cancelToken, () -> {
     *   System.out.println("CANCELED")
     * });
     * try {
     *   // ...
     * } finally {
     *   ref.unregisterAndWait(Cancellation.UNCANCELABLE_TOKEN);
     * }
     * // When execution reaches this line, it is ensured that if "CANCELED"
     * // has not been printed yet, it will never be printed.
     * }</pre>
     *
     * @param cancelToken the {@code CancellationToken} to which the listener
     *   is to be added. That is, the listener is registered to be notified of
     *   the cancellation requests of this token. This argument cannot be
     *   {@code null}.
     * @param listener the listener whose {@code run} method is to be passed
     *   as a cancellation listener to the specified {@code CancellationToken}.
     *   This argument cannot be {@code null}.
     * @return a {@code CancelableCloseable} whose {@code close} methods can
     *   be used to unregister the added listener and wait until it can be
     *   ensured that the listener will never be executed. This method never
     *   returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static WaitableListenerRef listenForCancellation(
            final CancellationToken cancelToken, final Runnable listener) {
        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(listener, "listener");

        final AtomicReference<WaitableSignal> doneSignalRef = new AtomicReference<>(null);
        final ThreadLocal<Object> inListener = new ThreadLocal<>();

        final ListenerRef listenerRef = cancelToken.addCancellationListener(() -> {
            WaitableSignal endRunSignal = new WaitableSignal();
            if (!doneSignalRef.compareAndSet(null, endRunSignal)) {
                return;
            }

            Object prevValue = inListener.get();
            try {
                inListener.set(Boolean.TRUE);
                listener.run();
            } finally {
                if (prevValue == null) inListener.remove();
                endRunSignal.signal();
            }
        });
        return new WaitableListenerRef() {
            private boolean isInListener() {
                Object value = inListener.get();
                if (value == null) inListener.remove();
                return value != null;
            }

            @Override
            public void unregisterAndWait(CancellationToken cancelToken) {
                Objects.requireNonNull(cancelToken, "cancelToken");
                if (isInListener()) {
                    throw new IllegalStateException(
                            "This method cannot be called from the"
                            + " registered cancellation listener.");
                }

                WaitableSignal signal = doneSignalRef.getAndSet(WaitableSignal.SIGNALING_SIGNAL);
                try {
                    if (signal != null) {
                        signal.waitSignal(cancelToken);
                    }
                } finally {
                    listenerRef.unregister();
                }
            }

            @Override
            public void unregister() {
                listenerRef.unregister();
            }
        };
    }

    /**
     * Executes the specified task on the current thread synchronously and
     * interrupts the current thread if the specified {@link CancellationToken}
     * signals a cancellation request. The {@code InterruptedException} thrown
     * by the specified task is treated as if the task has been canceled. That
     * is, {@code InterruptedException} thrown by the task is converted to
     * {@link OperationCanceledException}; any other exception is simply
     * propagated to the caller as it was thrown by the task.
     * <P>
     * Note that this may cause the current thread to be interrupted without
     * throwing any exception if the underlying task does not detect the
     * thread interruption. The thread interrupted status will not be cleared
     * even if it was caused by a cancellation request because there is no way
     * to detect if outside code has also interrupted the current thread.
     * <P>
     * This method is intended to be used to convert code which uses thread
     * interruption for cancellation to the more robust cancellation mechanism
     * provided by <I>JTrim</I>.
     *
     * @param <ResultType> the type of the result of the task to be executed
     * @param cancelToken the {@code CancellationToken} which when signals
     *   causes the current thread to be interrupted. This argument will also be
     *   passed to the task to be executed. This argument cannot be {@code null}.
     * @param task the task to be executed. This argument cannot be
     *   {@code null}.
     * @return the return value of the specified task. This value is exactly the
     *   same object as the one returned by the task and so if the task returns
     *   {@code null}, this method may also returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws OperationCanceledException thrown if the underlying task thrown
     *   an {@code OperationCanceledException} or an
     *   {@code InterruptedException}. Note that the current thread will no be
     *   re-interrupted if the specified task throws an
     *   {@code InterruptedException}.
     */
    public static <ResultType> ResultType doAsCancelable(
            CancellationToken cancelToken, InterruptibleTask<ResultType> task) {
        ThreadInterrupter threadInterrupter = new ThreadInterrupter(Thread.currentThread());
        ListenerRef listenerRef = cancelToken.addCancellationListener(threadInterrupter);

        try {
            return task.execute(cancelToken);
        } catch (InterruptedException ex) {
            throw new OperationCanceledException(ex);
        } finally {
            threadInterrupter.stopInterrupt();
            listenerRef.unregister();
        }
    }

    private enum DoNothingController implements CancellationController {
        INSTANCE;

        @Override
        public void cancel() {
        }
    }

    private enum UncancelableToken implements CancellationToken {
        INSTANCE;

        @Override
        public ListenerRef addCancellationListener(Runnable task) {
            return UnregisteredListenerRef.INSTANCE;
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Override
        public void checkCanceled() {
        }
    }

    private enum CanceledToken implements CancellationToken {
        INSTANCE;

        @Override
        public ListenerRef addCancellationListener(Runnable task) {
            task.run();
            return UnregisteredListenerRef.INSTANCE;
        }

        @Override
        public boolean isCanceled() {
            return true;
        }

        @Override
        public void checkCanceled() {
            throw new OperationCanceledException();
        }
    }

    private Cancellation() {
        throw new AssertionError();
    }
}
