package org.jtrim.access;

/**
 * The listener interface to be invoked when the availability of a right group
 * changes. That is, when a group of rights become available or unavailable
 * in an {@link AccessManager}.
 * <P>
 * {@code AccessChangeAction} implementations are intended to be added to a
 * {@link RightGroupHandler} (which is an {@link AccessStateListener} of an
 * {@code AccessManager}).
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
 * @see RightGroupHandler
 *
 * @author Kelemen Attila
 */
public interface AccessChangeAction {
    /**
     * Invoked when a specific group of right availability changes. The group of
     * rights are normally defined when the {@code AccessChangeAction} is added
     * to a {@link RightGroupHandler}.
     * <P>
     * The {@code AccessManager} managing the group of rights is specified for
     * this method. The {@code AccessManager} maybe used to determine what
     * {@link AccessToken} instances block access to the right. Note however,
     * that by the time this method gets called, the current rights available
     * may change, and so this must be considered when the tokens are needed for
     * some purpose. Regardless of this possible inconsistency when the
     * {@code available} argument is {@code true}, it can be assumed that there
     * are no blocking tokens.
     *
     * @param accessManager the {@code AccessManager} managing the rights in the
     *   group of rights. This argument cannot be {@code null}.
     * @param available {@code true} if the group of right, this
     *   {@code AccessChangeAction} listens is available, {@code false} if not
     */
    public void onChangeAccess(AccessManager<?, ?> accessManager, boolean available);
}
