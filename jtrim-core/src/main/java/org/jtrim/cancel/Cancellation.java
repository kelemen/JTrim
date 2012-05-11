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

    public static ChildCancellationSource createChildCancellationSource(
            CancellationToken cancelToken) {

        SimpleChildCancellationSource result;
        result = new SimpleChildCancellationSource(CANCELED_TOKEN);
        result.attachToParent();
        return result;
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
