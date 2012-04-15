package org.jtrim.access;

/**
 * The listener interface for receiving notification of the terminate event
 * of an {@link AccessToken}. The terminate event cannot occur more than once
 * per one {@code AccessToken}.
 * <P>
 * Note that method notified of this event does not receive a reference
 * to the {@code AccessToken} which terminated, so an {@code AccessListener}
 * should not be registered to multiple {@code AccessToken}s (although
 * it is possible to do so).
 *
 * <h3>Thread safety</h3>
 * Instances of this interface does not need to be thread-safe.
 * <h4>Synchronization transparency</h4>
 * This listener is not required to be <I>synchronization transparent</I> but
 * they must expect to be notified on any thread.
 *
 * @see AccessToken#addAccessListener(AccessListener)
 * @author Kelemen Attila
 */
public interface AccessListener {

    /**
     * Invoked when the {@link AccessToken} terminates and no longer executes
     * tasks. This method must expect to be called on any thread and therefore
     * must not wait for other tasks to complete to avoid deadlocks.
     * It is especially more important to be aware that this method can be
     * called from the methods of {@code AccessToken}, {@link AccessManager} or
     * from the threads tasks scheduled to the {@code AccessToken} executes.
     * <P>
     * Although this method can only be called once per one {@code AccessToken}
     * but it is recommended to be written to be idempotent to allow the
     * following idiom to be used:
     * <pre>
     * void registerListener(AccessToken<?> token, AccessListener listener) {
     *   token.addAccessListener(listener);
     *   if (token.isTerminated()) {
     *     listener.onLostAccess();
     *   }
     * }
     * </pre>
     * This method will always notify the listener of terminate events
     * even if it was terminated before the listener was registered and can
     * be safely used if the {@code onLostAccess()} method is idempotent.
     *
     * @see AccessTokens#idempotentAccessListener(AccessListener)
     */
    public void onLostAccess();
}
