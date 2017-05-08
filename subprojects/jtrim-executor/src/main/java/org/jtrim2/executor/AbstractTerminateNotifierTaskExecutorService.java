package org.jtrim2.executor;

import java.util.Objects;
import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.OneShotListenerManager;

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

    private final OneShotListenerManager<Runnable, Void> listeners;

    /**
     * Initializes the {@code AbstractTerminateNotifierTaskExecutorService} with
     * no listeners currently registered.
     */
    public AbstractTerminateNotifierTaskExecutorService() {
        this.listeners = new OneShotListenerManager<>();
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
        EventListeners.dispatchRunnable(listeners);
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: Listeners added by this method will be
     * automatically unregistered after they have been notified.
     */
    @Override
    public ListenerRef addTerminateListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        return listeners.registerOrNotifyListener(listener);
    }
}
