package org.jtrim.access;

/**
 * The listener interface for receiving notification of changes in the
 * accessibility of rights. This interface is intended to be used by
 * {@link AccessManager AccessManagers} to notify clients that a certain right
 * is now available or not.
 * <P>
 * It is not mandatory that an {@code AccessManager} implementation provides
 * such a notification service but highly recommended to do so. However if
 * an implementation provides this service it must fire events in the order they
 * occur.
 * <P>
 * A right can be in three states as stated in the documentation of
 * {@link AccessState}.
 *
 * <h3>Thread safety</h3>
 * Most implementation of this interface are not required to be thread-safe
 * but an event provider (e.g.: an {@code AccessManager}) may define otherwise.
 * <h4>Synchronization transparency</h4>
 * Implementations of this listener are not required to be
 * <I>synchronization transparent</I> however they must not wait for
 * asynchronous events.
 *
 * @param <RightType> the type of the right this listener can receive
 *   notifications of
 *
 * @author Kelemen Attila
 */
public interface AccessStateListener<RightType> {
    /**
     * Invoked when a right (or rights) becomes available or unavailable.
     * If a certain right implies other rights as well (such as with
     * {@link HierarchicalRight hierarchical rights}) the new state is true
     * for every such right.
     *
     * @param right the right which has changed state
     * @param state the new state which is true for the specified right
     *   (or rights)
     */
    public void onEnterState(RightType right, AccessState state);
}
