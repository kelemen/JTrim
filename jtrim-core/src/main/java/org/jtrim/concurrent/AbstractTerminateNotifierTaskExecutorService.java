package org.jtrim.concurrent;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.event.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * An abstract base class for {@link TaskExecutorService} implementations which
 * implements the {@link #addTerminateListener(Runnable)} method as well as the
 * ones implemented by {@link AbstractTaskExecutorService}.
 * <P>
 * Rather than implementing the {@code addTerminateListener} method,
 * implementations need to call the {@code #notifyTerminateListeners()} method
 * after the {@code TaskExecutorService} terminated. This abstract class
 * provides a safe and robust handling of terminate listeners. The provided
 * implementation will guarantee that listeners will not be notified multiple
 * times and will be automatically unregistered after they have been notified
 * (allowing the listeners to be garbage collected, even if not unregistered
 * manually).
 *
 * @author Kelemen Attila
 */
public abstract class AbstractTerminateNotifierTaskExecutorService
extends
        AbstractTaskExecutorService {

    private final ListenerManager<Runnable, Void> listeners;

    /**
     * Initializes the {@code AbstractTerminateNotifierTaskExecutorService} with
     * no listeners currently registered.
     */
    public AbstractTerminateNotifierTaskExecutorService() {
        this.listeners = new CopyOnTriggerListenerManager<>();
    }

    /**
     * Notifies the currently registered terminate listeners. This method may
     * only be called if this {@code TaskExecutorService} is already in the
     * terminated state (i.e.: {@link #isTerminated() isTerminated()} returns
     * {@code true}. Note that once a {@code TaskExecutorService} is in the
     * terminated state it must remain in the terminated state forever after.
     * The {@code AbstractTerminateNotifierTaskExecutorService} actually relies
     * on this property for correctness.
     */
    protected final void notifyTerminateListeners() {
        if (!isTerminated()) {
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
    public ListenerRef addTerminateListener(Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");
        // A quick check for the already terminate case.
        if (isTerminated()) {
            listener.run();
            return UnregisteredListenerRef.INSTANCE;
        }

        AutoUnregisterListener autoListener = new AutoUnregisterListener(listener);
        ListenerRef result = autoListener.registerWith(listeners);
        // If the executor terminated before the registration and also
        // notifyTerminateListeners() was called, there is a chance that this
        // currently added listener may not have been notified. To avoid this
        // we have to double check isTerminated() and notify the listener.
        // Note that AutoUnregisterListener.run() is idempotent, so there is no
        // problem if it actually got called.
        if (isTerminated()) {
            autoListener.run();
        }
        return result;
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
