package org.jtrim.access;

import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.event.*;

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

    private final OneShotListenerManager<Runnable, Void> listeners;

    /**
     * Initializes the {@code AbstractAccessToken}.
     */
    public AbstractAccessToken() {
        this.listeners = new OneShotListenerManager<>();
    }

    /**
     * Notifies the currently registered release listeners. This method may
     * only be called if this {@code AccessToken} is already in the released
     * state (i.e.: {@link #isReleased() isReleased()} returns {@code true}.
     * Note that once a {@code TaskExecutorService} is in the released state it
     * must remain in the terminated state forever after. The
     * {@code AbstractAccessToken} actually relies on this property for
     * correctness.
     * <P>
     * If called after this token is already in released state, this method is
     * idempotent.
     */
    protected final void notifyReleaseListeners() {
        if (!isReleased()) {
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
    public ListenerRef addReleaseListener(Runnable listener) {
        return listeners.registerOrNotifyListener(listener);
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method simply repeatedly calls the
     * {@link #tryAwaitRelease(CancellationToken, long, TimeUnit) tryAwaitRelease(CancellationToken, long, TimeUnit)}
     * method until it returns {@code true}.
     */
    @Override
    public void awaitRelease(CancellationToken cancelToken) {
        while (!tryAwaitRelease(cancelToken, Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
            // Repeat until it has been released, or throws an exception.
        }
    }
}
