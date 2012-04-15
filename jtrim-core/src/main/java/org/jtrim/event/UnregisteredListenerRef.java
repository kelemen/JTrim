package org.jtrim.event;

import org.jtrim.utils.ExceptionHelper;

/**
 * A {@link ListenerRef} which defines a listener which is already unregistered
 * and will not be notified of events.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are immutable and as such, safe to be
 * accessed by multiple threads concurrently. Note however, that the referenced
 * listener may not be an immutable object.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @param <ListenerType> the type of the listener already unregistered listener
 *
 * @author Kelemen Attila
 */
public final class UnregisteredListenerRef<ListenerType>
implements
        ListenerRef<ListenerType> {

    private final ListenerType listener;

    /**
     * Initializes the {@code UnregisteredListenerRef} with the specified
     * listener to be returned by the {@link #getListener() getListener()}
     * method.
     *
     * @param listener the listener to be returned by the {@code #getListener()}
     *   method. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public UnregisteredListenerRef(ListenerType listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        this.listener = listener;
    }

    /**
     * Returns {@code false}, since this listener is considered to be always
     * unregistered.
     *
     * @return {@code false} always
     */
    @Override
    public boolean isRegistered() {
        return false;
    }

    /**
     * This method does nothing and returns immediately to the caller.
     */
    @Override
    public void unregister() {
    }

    /**
     * Returns the listener specified at construction time.
     *
     * @return the listener specified at construction time. This method never
     *   returns {@code null}.
     */
    @Override
    public ListenerType getListener() {
        return listener;
    }
}
