package org.jtrim2.event;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Defines a {@link ListenerRef} forwarding its calls to another
 * {@code ListenerRef} which is specified after construction time. This class
 * is useful if you need to unregister a listener in the listener itself (or in
 * any code which is defined before actually registering the listener).
 * <P>
 * This is the recommended usage pattern:
 * <pre>{@code
 * ListenerRegistry<Runnable> registry = ...;
 *
 * final InitLaterListenerRef listenerRef = new InitLaterListenerRef();
 * // From this point, you may use "listenerRef" to unregister the listener.
 * // The listener will be unregistered as soon as the "init" method has been
 * // called
 *
 * listenerRef.init(registry.registerListener(() -> {
 *   // You may use "listenerRef" here to unregister this listener.
 * }));
 * }</pre>
 * <P>
 * Note that the actual unregistering of the listener will not happen until you
 * call the {@link #init(ListenerRef)} method. Therefore, if you unregister the
 * listener prior calling {@code init}, it may be possible, that the listener
 * will be notified even if you have called {@code unregister()} on the
 * {@code InitLaterListenerRef} which is a violation of the contract of the
 * {@code ListenerRef} interface. Therefore, it is not recommended not
 * unregister the listener before calling the {@code init} method. If you do
 * unregister it, don't forget that it is possible that you may receive event
 * notifications.
 *
 * <h2>Thread safety</h2>
 * Methods of this class are safe to use by multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this class are <I>synchronization transparent</I>, assuming
 * that the underlying listener is <I>synchronization transparent</I>.
 */
public final class InitLaterListenerRef implements ListenerRef {
    private final AtomicReference<ListenerRef> currentRef;

    /**
     * Creates a new {@code InitLaterListenerRef} with no underlying
     * {@code ListenerRef}. Call the {@link #init(ListenerRef) init} method,
     * to set the {@code ListenerRef} to which calls are to be forwarded.
     */
    public InitLaterListenerRef() {
        this.currentRef = new AtomicReference<>(null);
    }

    /**
     * Sets to {@code ListenerRef} to which calls are forwarded to. This method
     * may not be called more than once.
     * <P>
     * If {@link #unregister()} has been called on this
     * {@code InitLaterListenerRef}, this method will call the
     * {@code unregister()} method of the passed {@code ListenerRef}.
     *
     * @param listenerRef the {@code ListenerRef} to which class are forwarded
     *   to. This argument cannot be {@code null}.
     *
     * @throws IllegalStateException thrown if this method has already been
     *   called
     */
    public void init(ListenerRef listenerRef) {
        Objects.requireNonNull(listenerRef, "listenerRef");

        do {
            ListenerRef oldRef = currentRef.get();
            if (oldRef != null) {
                if (oldRef == PoisonListenerRef.LISTENER_REF_POISON) {
                    listenerRef.unregister();
                    return;
                }

                throw new IllegalStateException("Already initialized.");
            }
        } while (!currentRef.compareAndSet(null, listenerRef));
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: If you call this method before calling the
     * {@link #init(ListenerRef) init} method, this method will simply cause the
     * subsequent {@code init} method to unregister the underlying
     * {@code ListenerRef}.
     * <P>
     * This method is allowed to be called concurrently with the {@code init}
     * method.
     */
    @Override
    public void unregister() {
        ListenerRef oldRef = currentRef.getAndSet(PoisonListenerRef.LISTENER_REF_POISON);
        if (oldRef != null) {
            oldRef.unregister();
        }
    }

    private enum PoisonListenerRef implements ListenerRef {
        LISTENER_REF_POISON;

        @Override
        public void unregister() {
        }
    }
}
