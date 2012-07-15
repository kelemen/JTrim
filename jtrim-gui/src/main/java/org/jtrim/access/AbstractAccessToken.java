package org.jtrim.access;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.event.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * A convenient base class for {@link AccessToken} implementations.
 * <P>
 * This class implements the {@code addTerminateListener} listener method but
 * subclasses need to call the {@code #notifyReleaseListeners()} method
 * after the {@code AccessToken} was released. This abstract class
 * provides a safe and robust handling of release listeners. The provided
 * implementation will guarantee that listeners will not be notified multiple
 * times and will be automatically unregistered after they have been notified
 * (allowing the listeners to be garbage collected, even if not unregistered
 * manually).
 * <P>
 * This class also adds a simple implementation for the
 * {@link #awaitRelease(CancellationToken) awaitRelease(CancellationToken)}
 * method which relies on the other {@code awaitRelease} method (the one with
 * the timeout argument).
 *
 * @param <IDType> the type of the access ID (see
 *   {@link AccessToken#getAccessID() getAccessID()})
 *
 * @author Kelemen Attila
 */
public abstract class AbstractAccessToken<IDType>
implements
        AccessToken<IDType> {

    private final ListenerManager<Runnable, Void> listeners;

    /**
     * Initializes the {@code AbstractAccessToken}.
     */
    public AbstractAccessToken() {
        this.listeners = new CopyOnTriggerListenerManager<>();
    }

    /**
     * Notifies the currently registered release listeners. This method may
     * only be called if this {@code AccessToken} is already in the released
     * state (i.e.: {@link #isReleased() isReleased()} returns {@code true}.
     * Note that once a {@code TaskExecutorService} is in the released state it
     * must remain in the terminated state forever after. The
     * {@code AbstractAccessToken} actually relies on this property for
     * correctness.
     */
    protected final void notifyReleaseListeners() {
        if (!isReleased()) {
            throw new IllegalStateException(
                    "May only be called in the terminated state.");
        }
        listeners.onEvent(RunnableDispatcher.INSTANCE, null);
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: Listeners added by this method will be
     * automatically unregistered after they have been notified.
     */
    @Override
    public ListenerRef addReleaseListener(Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");
        // A quick check for the already terminate case.
        if (isReleased()) {
            listener.run();
            return UnregisteredListenerRef.INSTANCE;
        }

        AutoUnregisterListener autoListener = new AutoUnregisterListener(listener);
        ListenerRef result = autoListener.registerWith(listeners);
        // If the access token was released before the registration and also
        // notifyReleaseListeners() was called, there is a chance that this
        // currently added listener may not have been notified. To avoid this
        // we have to double check isReleased() and notify the listener.
        // Note that AutoUnregisterListener.run() is idempotent, so there is no
        // problem if it actually got called.
        if (isReleased()) {
            autoListener.run();
        }
        return result;
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method simply repeatedly calls the
     * {@link #awaitRelease(CancellationToken, long, TimeUnit) awaitRelease(CancellationToken, long, TimeUnit)}
     * method until it returns {@code true}.
     */
    @Override
    public void awaitRelease(CancellationToken cancelToken) {
        while (!awaitRelease(cancelToken, Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
            // Repeat until it has been released, or throws an exception.
        }
    }

    private enum RunnableDispatcher implements EventDispatcher<Runnable, Void> {
        INSTANCE;

        @Override
        public void onEvent(Runnable eventListener, Void arg) {
            eventListener.run();
        }
    }

    // Note that apart from automatic unregistering, this class
    // takes care that the listener may not be run multiple times.
    private static class AutoUnregisterListener implements Runnable {
        private final AtomicReference<Runnable> listener;
        private volatile ListenerRef listenerRef;

        public AutoUnregisterListener(Runnable listener) {
            this.listener = new AtomicReference<>(listener);
            this.listenerRef = null;
        }

        public ListenerRef registerWith(ListenerManager<Runnable, ?> manager) {
            ListenerRef currentRef = manager.registerListener(this);
            this.listenerRef = currentRef;
            if (listener.get() == null) {
                this.listenerRef = null;
                currentRef.unregister();
            }
            return currentRef;
        }

        @Override
        public void run() {
            Runnable currentListener = listener.getAndSet(null);
            ListenerRef currentRef = listenerRef;
            if (currentRef != null) {
                currentRef.unregister();
            }
            if (currentListener != null) {
                currentListener.run();
            }
        }
    }
}
