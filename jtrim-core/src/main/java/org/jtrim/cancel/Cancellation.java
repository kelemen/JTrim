package org.jtrim.cancel;

import org.jtrim.event.ListenerRef;
import org.jtrim.event.UnregisteredListenerRef;

/**
 * Contains static helper methods and fields related cancellation.
 *
 * @author Kelemen Attila
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
     * Creates a new {@code ChildCancellationSource} which will be notified
     * of the cancellation requests of the specified {@code CancellationToken}.
     * That is, the parent {@code CancellationToken} of the returned
     * {@code ChildCancellationSource} will be the specified
     * {@code CancellationToken}.
     * <P>
     * Note that this method registers a cancellation listener with the
     * specified with the specified {@code CancellationToken} to forward the
     * cancellation request, When forwarding the cancellation is no longer
     * required, it is recommended to detach the returned
     * {@code ChildCancellationSource} from its parent to allow the previously
     * mentioned cancellation listener to be unregistered.
     *
     * @param cancelToken the parent {@code CancellationToken} of the returned
     *   {@code ChildCancellationSource}. This argument cannot be {@code null}.
     * @return the new {@code ChildCancellationSource} which will be notified
     *   of the cancellation requests of the specified
     *   {@code CancellationToken}. This method never returns {@code null}.
     */
    public static ChildCancellationSource createChildCancellationSource(
            CancellationToken cancelToken) {

        SimpleChildCancellationSource result;
        result = new SimpleChildCancellationSource(cancelToken);
        result.attachToParent();
        return result;
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
