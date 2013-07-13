package org.jtrim.access;

/**
 * @deprecated This interface was designed for {@link AccessAvailabilityNotifier}
 *   which is deprecated.
 * <P>
 * The listener interface to be invoked when the availability of a right group
 * changes. That is, when a group of rights become available or unavailable
 * in an {@link AccessManager}.
 * <P>
 * {@code AccessChangeAction} implementations are intended to be added to an
 * {@link AccessAvailabilityNotifier}.
 *
 * <h3>Thread safety</h3>
 * Most implementation of this interface are not required to be thread-safe
 * but {@code AccessManager} managing the rights, may define otherwise.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this listener are not required to be
 * <I>synchronization transparent</I> however they must not wait for
 * asynchronous events.
 *
 * @see AccessAvailabilityNotifier
 *
 * @author Kelemen Attila
 */
@Deprecated
public interface AccessChangeAction {
    /**
     * Invoked when a specific group of right availability changes. The group of
     * rights are normally defined when the {@code AccessChangeAction} is added
     * to an {@link AccessAvailabilityNotifier}.
     *
     * @param available {@code true} if the group of right, this
     *   {@code AccessChangeAction} listens is available, {@code false} if not
     */
    public void onChangeAccess(boolean available);
}
