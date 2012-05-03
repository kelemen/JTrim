package org.jtrim.concurrent.executor;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.event.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a default implementation for creating {@link CancellationController}
 * and {@link CancellationToken} objects. These two objects returned by the
 * {@code CancellationSource} are linked in way, that requesting
 * {@link CancellationController#cancel() cancellation} through the
 * {@code CancellationController} will cause the {@code CancellationToken} to
 * move to a canceled state.
 * <P>
 * The two objects can be returned by the {@link #getController() getController}
 * and the {@link #getToken() getToken} methods.
 *
 * <h3>Thread safety</h3>
 * Methods of this class can be safely accessed by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I> but note that
 * the methods of the {@code CancellationController} and the
 * {@code CancellationSource} are not.
 *
 * @see CancellationController
 * @see CancellationToken
 *
 * @author Kelemen Attila
 */
public final class CancellationSource {
    /**
     * A {@code CancellationToken} which can never be in the canceled state.
     * The {@link CancellationToken#isCanceled() isCanceled} method of
     * {@code UNCANCELABLE_TOKEN} will always return {@code false} when checked.
     * If a listener is registered with this token to be notified of
     * cancellation requests, this {@code CancellationToken} will do nothing but
     * return an already unregistered {@code ListenerRef}.
     */
    public static final CancellationToken UNCANCELABLE_TOKEN = UncancelableToken.INSTANCE;

    /**
     * A {@code CancellationToken} which is already in the canceled state.
     * The {@link CancellationToken#isCanceled() isCanceled} method of
     * {@code UNCANCELABLE_TOKEN} will always return {@code true} when checked.
     * If a listener is registered with this token to be notified of
     * cancellation requests, this {@code CancellationToken} will immediately
     * notify the listener and return an already unregistered
     * {@code ListenerRef}.
     */
    public static final CancellationToken CANCELED_TOKEN = CanceledToken.INSTANCE;

    private final CancellationControllerImpl impl;

    /**
     * Creates a new {@code CancellationSource} of which
     * {@code CancellationToken} was not yet canceled.
     */
    public CancellationSource() {
        this.impl = new CancellationControllerImpl();
    }

    /**
     * Returns the {@code CancellationController} which can be used to signal
     * cancellation to the {@link CancellationToken} returned by the
     * {@link #getToken() getToken()} method. That is, after
     * the {@code getController().cancel()} invocation, the
     * {@code getToken().isCanceled()} invocation will return {@code true}.
     *
     * @return the {@code CancellationController} which can be used to signal
     *   cancellation to the {@link CancellationToken} returned by the
     *   {@link #getToken() getToken()} method. This method never returns
     *   {@code null} and every invocation of this method will return the same
     *   object.
     */
    public CancellationController getController() {
        return impl;

    }

    /**
     * Returns the {@code CancellationToken} which detects cancellation
     * requests made through the {@link CancellationController} returned by the
     * {@link #getController() getController()} method. That is, after
     * the {@code getController().cancel()} invocation, the
     * {@code getToken().isCanceled()} invocation will return {@code true}.
     *
     * @return the {@code CancellationToken} which detects cancellation
     *   requests made through the {@link CancellationController} returned by
     *   the {@link #getController() getController()} method. This method never
     *   returns {@code null} and every invocation of this method will return
     *   the same object.
     */
    public CancellationToken getToken() {
        return impl.getToken();
    }

    private static class CancellationControllerImpl
    implements
            CancellationController {

        private final CancellationTokenImpl impl;

        public CancellationControllerImpl() {
            this.impl = new CancellationTokenImpl();
        }

        public CancellationToken getToken() {
            return impl;
        }

        @Override
        public void cancel() {
            impl.cancel();
        }
    }

    private static class CancellationTokenImpl
    implements
            CancellationToken {

        private volatile boolean canceled;
        private final ListenerManager<Runnable, Void> listeners;

        public CancellationTokenImpl() {
            this.listeners = new CopyOnTriggerListenerManager<>();
            this.canceled = false;
        }

        public void cancel() {
            canceled = true;
            listeners.onEvent(RunnableDispatcher.INSTANCE, null);
        }

        @Override
        public ListenerRef addCancellationListener(final Runnable task) {
            ExceptionHelper.checkNotNullArgument(task, "task");

            SingleShotEvent idempotentTask = new SingleShotEvent(task);
            final ListenerRef result = listeners.registerListener(idempotentTask);
            idempotentTask.setListenerRef(result);

            if (isCanceled()) {
                result.unregister();
                idempotentTask.run();
                return UnregisteredListenerRef.INSTANCE;
            }
            else {
                return result;
            }
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void checkCanceled() {
            if (isCanceled()) {
                throw new OperationCanceledException();
            }
        }

        private static class SingleShotEvent implements Runnable {
            private final AtomicReference<Runnable> taskRef;
            private volatile ListenerRef listenerRef;

            public SingleShotEvent(Runnable task) {
                this.taskRef = new AtomicReference<>(task);
                this.listenerRef = null;
            }

            public void setListenerRef(ListenerRef listenerRef) {
                this.listenerRef = listenerRef;
                if (taskRef.get() == null) {
                    listenerRef.unregister();
                    this.listenerRef = null;
                }
            }

            @Override
            public void run() {
                Runnable task = taskRef.getAndSet(null);
                if (task != null) {
                    try {
                        task.run();
                    } finally {
                        ListenerRef currentListenerRef = listenerRef;
                        if (currentListenerRef != null) {
                            currentListenerRef.unregister();
                            listenerRef = null;
                        }
                    }
                }
            }
        }
    }

    private enum RunnableDispatcher implements EventDispatcher<Runnable, Void> {
        INSTANCE;

        @Override
        public void onEvent(Runnable eventListener, Void arg) {
            eventListener.run();
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
}
