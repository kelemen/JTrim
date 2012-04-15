package org.jtrim.access;

/**
 * Defines the possible states an access right can be in.
 * These states can be used by {@link AccessManager AccessManagers} which
 * can notify users of possible changes in the accessibility of rights.
 *
 * @see AccessStateListener
 * @author Kelemen Attila
 */
public enum AccessState {
    /**
     * This state implies that the right can be acquired by both read
     * and write requests.
     */
    AVAILABLE,

    /**
     * This state implies that the right can only be acquired by both read
     * requests but write requests will be denied while in this state.
     */
    READONLY,

    /**
     * This state implies that the right cannot be acquired by any kind of
     * request and such requests will be denied if tried anyway.
     */
    UNAVAILABLE
}
