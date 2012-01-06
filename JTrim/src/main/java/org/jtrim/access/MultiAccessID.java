package org.jtrim.access;

import org.jtrim.utils.ExceptionHelper;

/**
 * Stores IDs of two {@link AccessToken AccessTokens}. This class is used as an
 * {@link AccessToken#getAccessID() access ID} by
 * {@link AccessTokens#combineTokens(java.util.concurrent.Executor, org.jtrim.access.AccessToken, org.jtrim.access.AccessToken) combined tokens}.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are immutable and as such are thread-safe even in
 * the face of unsynchronized concurrent access. Note that although instances
 * are immutable, the IDs instances store might not necessarily immutable
 * themselves. Note however that <B>it is strongly recommended to use immutable
 * access IDs</B>.
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @param <IDType1> the type of the {@link AccessToken#getAccessID() access ID}
 *   of the first token
 * @param <IDType2> the type of the {@link AccessToken#getAccessID() access ID}
 *   of the second token
 * @author Kelemen Attila
 */
public final class MultiAccessID<IDType1, IDType2> {
    private final IDType1 id1;
    private final IDType2 id2;

    /**
     * Initializes this instance with the specified ID references.
     *
     * @param id1 the access ID of the first
     *   {@link AccessToken AccessToken} of a
     *   {@link AccessTokens#combineTokens(java.util.concurrent.Executor, org.jtrim.access.AccessToken, org.jtrim.access.AccessToken) combined token}.
     *   This argument cannot be {@code null}.
     * @param id2 the access ID of the second
     *   {@link AccessToken AccessToken} of a
     *   {@link AccessTokens#combineTokens(java.util.concurrent.Executor, org.jtrim.access.AccessToken, org.jtrim.access.AccessToken) combined token}.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the specified IDs are
     *   {@code null}
     */
    public MultiAccessID(IDType1 id1, IDType2 id2) {
        ExceptionHelper.checkNotNullArgument(id1, "id1");
        ExceptionHelper.checkNotNullArgument(id2, "id2");

        this.id1 = id1;
        this.id2 = id2;
    }

    /**
     * Returns the access ID of the first {@link AccessToken AccessToken} of a
     * {@link AccessTokens#combineTokens(java.util.concurrent.Executor, org.jtrim.access.AccessToken, org.jtrim.access.AccessToken) combined token}.
     *
     * @return the access ID of the first {@link AccessToken AccessToken} of a
     *   combined token. This method never returns {@code null}.
     */
    public IDType1 getAccessID1() {
        return id1;
    }

    /**
     * Returns the access ID of the second {@link AccessToken AccessToken} of a
     * {@link AccessTokens#combineTokens(java.util.concurrent.Executor, org.jtrim.access.AccessToken, org.jtrim.access.AccessToken) combined token}.
     *
     * @return the access ID of the second {@link AccessToken AccessToken} of a
     *   combined token. This method never returns {@code null}.
     */
    public IDType2 getAccessID2() {
        return id2;
    }

    /**
     * Returns the string representation of this access ID in no particular
     * format. The returned string will contain the string representation
     * of both IDs.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "MultiAccessID{" + "id1=" + id1 + ", id2=" + id2 + '}';
    }
}
