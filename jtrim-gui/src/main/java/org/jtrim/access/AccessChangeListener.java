package org.jtrim.access;

/**
 * The listener interface for receiving notification of acquiring and releasing
 * {@link AccessToken access tokens}. This interface is intended to be used by
 * {@link AccessManager access managers} to notify clients that a certain token
 * has been acquired or released.
 *
 * <h3>Thread safety</h3>
 * Most implementation of this interface are not required to be thread-safe
 * but an event provider (e.g.: an {@code AccessManager}) may define otherwise.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this listener are not required to be
 * <I>synchronization transparent</I> however they must not wait for
 * asynchronous events.
 *
 * @param <IDType> the type of the
 *   {@link AccessRequest#getRequestID() request id}
 * @param <RightType> the type of the right this listener can receive
 *   notifications of
 *
 * @see AccessAvailabilityNotifier
 *
 * @author Kelemen Attila
 */
public interface AccessChangeListener<IDType, RightType> {
    /**
     * Invoked when an {@link AccessToken} gets acquired or released. Note that,
     * even if this event reports that an {@code AccessToken} got released, it
     * does not imply that this request is now available. You have to verify
     * it manually, that the rights you need are available or not.
     *
     * @param request the request used to acquire the {@code AccessToken} which
     *   has been acquired or released. This argument cannot be {@code null}.
     * @param acquired {@code true} if this notification is due to the
     *   {@code AccessToken} got acquired, {@code false} if it was just
     *   released.
     */
    public void onChangeAccess(
            AccessRequest<? extends IDType, ? extends RightType> request,
            boolean acquired);
}
