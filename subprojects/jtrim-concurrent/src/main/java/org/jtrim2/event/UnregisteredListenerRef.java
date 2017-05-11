package org.jtrim2.event;

/**
 * A {@link ListenerRef} which defines a listener which is already unregistered
 * and will not be notified of events.
 * <P>
 * This class is a singleton and its one and only instance can be accessed by
 * {@link #INSTANCE}.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are immutable and as such, safe to be
 * accessed by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 */
public enum UnregisteredListenerRef implements ListenerRef {
    /**
     * The one and only instance of {@code UnregisteredListenerRef}.
     */
    INSTANCE;

    /**
     * This method does nothing and returns immediately to the caller.
     */
    @Override
    public void unregister() {
    }
}
