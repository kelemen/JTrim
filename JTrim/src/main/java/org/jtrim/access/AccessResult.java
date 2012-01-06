package org.jtrim.access;

import java.util.*;
import java.util.concurrent.atomic.*;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.concurrent.ExecutorsEx;

/**
 * Defines the result of an {@link AccessToken} request from an
 * {@link AccessManager}. In case access was granted by the
 * {@code AccessManager} the {@link #getAccessToken() getAccessToken()} returns
 * a {@code non-null} {@code AccessToken}. Regardless if access was granted
 * the result may contain zero or more conflicting {@code AccessToken}s
 * which must be shutted down before access can be granted or in case of a
 * {@link AccessManager#getScheduledAccess(AccessRequest) scheduled token}
 * the returned {@code AccessToken} can execute tasks.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are immutable and as such are thread-safe even in
 * the face of unsynchronized concurrent access. Note that although instances
 * are immutable, the shared {@code AccessToken}s are not and they must be
 * safely published (with no race condition) to be safely used in multithreaded
 * environment.
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I> unless
 * otherwise noted.
 *
 * @param <IDType> the type of the access ID
 *   (see {@link AccessToken#getAccessID()})
 *
 * @see AccessManager
 * @see AccessToken
 * @author Kelemen Attila
 */
public final class AccessResult<IDType> {
    private final AccessToken<IDType> accessToken;
    private final Collection<AccessToken<IDType>> blockingTokens;

    private final AtomicReference<Set<IDType>> ids;

    /**
     * Creates a new {@code AccessResult} instance with no
     * {@link #getBlockingTokens() conflicting tokens}.
     * <P>
     * This constructor is intended to be used when access was granted
     * and there are no conflicting tokens.
     *
     * @param accessToken the {@link #getAccessToken() AccessToken} representing
     *   the requested rights. Although this argument can be {@code null} it is
     *   not recommended to call with {@code null} {@code AccessToken} because
     *   it means that access was not granted and the
     *   {@link #getBlockingTokens() conflicting tokens} are not known.
     */
    public AccessResult(AccessToken<IDType> accessToken) {
        this(accessToken, null);
    }

    /**
     * Creates a new {@code AccessResult} instance with no
     * {@link #getAccessToken() AccessToken}
     * ({@link #getAccessToken() getAccessToken()} will return {@code null})
     * and the list of the conflicting {@code AccessToken}s.
     * <P>
     * This constructor is intended to be used when access cannot be granted
     * and the supplied list of {@code AccessToken}s defines the conflicting
     * tokens needed to be shutted down before the requested rights can be
     * granted.
     *
     * @param blockingTokens the collection of {@code AccessToken}s needed
     *   to be shutted down before access can be granted. It is not recommended
     *   to call this constructor with an empty list of tokens because it means
     *   that access was not granted and the
     *   {@link #getBlockingTokens() conflicting tokens} are not known. This
     *   argument can be {@code null} which is equivalent to passing an empty
     *   collection. No reference will be retained by this access result to
     *   the passed {@code blockingTokens}, the collection will be copied.
     */
    public AccessResult(Collection<? extends AccessToken<IDType>> blockingTokens) {
        this(null, blockingTokens);
    }

    /**
     * Creates a new {@code AccessResult} instance with the requested
     * {@link #getAccessToken() AccessToken} and the list of the conflicting
     * {@code AccessToken}s.
     * <P>
     * Although this constructor can be used to define that access to the
     * requested rights was denied, it is intended to be used when access was
     * granted but there are {@link #getBlockingTokens() conflicting tokens}
     * which must be shutted down before tasks scheduled to the returned
     * {@code AccessToken} can execute. This is usually the case when a
     * {@link AccessManager#getScheduledAccess(AccessRequest) scheduled token}
     * was requested.
     *
     * @param accessToken he {@link #getAccessToken() AccessToken} representing
     *   the requested rights. This argument can be {@code null} in which case
     *   {@link #getAccessToken() getAccessToken()} will return {@code null}.
     *
     * @param blockingTokens the collection of {@code AccessToken}s needed
     *   to be shutted down before access can be granted. This argument can be
     *   {@code null} which is equivalent to passing an empty collection.
     *   No reference will be retained by this access result to the passed
     *   {@code blockingTokens}, the collection will be copied.
     */
    public AccessResult(AccessToken<IDType> accessToken,
            Collection<? extends AccessToken<IDType>> blockingTokens) {

        this.accessToken = accessToken;
        this.blockingTokens = blockingTokens == null
                ? Collections.<AccessToken<IDType>>emptySet()
                : CollectionsEx.readOnlyCopy(blockingTokens);

        this.ids = new AtomicReference<>(null);
    }

    /**
     * Shuts down the returned {@link #getAccessToken() AccessToken} if there
     * was one returned. In case there was no {@code AccessToken} returned this
     * method does nothing.
     * <P>
     * This method call is effectively equivalent to the following code:
     * <P>
     * {@code if (result.isAvailable()) result.getAccessToken().shutdown();}
     *
     * @see AccessToken#shutdown()
     */
    public void shutdown() {
        if (accessToken != null) {
            accessToken.shutdown();
        }
    }

    /**
     * Returns {@code true} if the access was granted.
     * <P>
     * If this method returns {@code true}
     * {@link #getAccessToken() getAccessToken()} will return a {@code non-null}
     * {@link AccessToken}.
     *
     * <h6>Synchronization transparency</h6>
     * This method is <I>synchronization transparent</I>.
     *
     * @return {@code true} if the access was granted, {@code false} otherwise.
     */
    public boolean isAvailable() {
        return accessToken != null;
    }

    /**
     * Returns the set of IDs of the conflicting
     * {@link AccessToken AccessTokens}. Notice that the size of the returned
     * set can be smaller (but not larger) than the number of conflicting
     * tokens.
     *
     * <h6>Synchronization transparency</h6>
     * This method is <I>synchronization transparent</I>.
     *
     * @return the set of IDs of the
     *   {@link #getBlockingTokens() conflicting tokens}. This method never
     *   returns {@code null}. In case there are no conflicting tokens this
     *   method returns an empty set.
     */
    public Set<IDType> getBlockingIDs() {
        Set<IDType> result = ids.get();
        if (result != null) {
            return result;
        }

        if (blockingTokens.isEmpty()) {
            result = Collections.emptySet();
        }
        else {
            result = CollectionsEx.newHashSet(blockingTokens.size());
            for (AccessToken<IDType> token: blockingTokens) {
                result.add(token.getAccessID());
            }

            result = Collections.unmodifiableSet(result);
        }

        if (!ids.compareAndSet(null, result)) {
            result = ids.get();
            assert result != null;
        }

        return result;
    }

    /**
     * Returns the {@link AccessToken} representing the requested rights or
     * {@code null} if the request was denied.
     * <P>
     * Note that in case of a
     * {@link AccessManager#getScheduledAccess(AccessRequest) scheduled token}
     * it is possible that there are
     * {@link #getBlockingTokens() conflicting tokens}.
     *
     * <h6>Synchronization transparency</h6>
     * This method is <I>synchronization transparent</I>.
     *
     * @return the {@link AccessToken} representing the requested rights or
     *   {@code null} if the request was denied
     */
    public AccessToken<IDType> getAccessToken() {
        return accessToken;
    }

    /**
     * Returns the conflicting tokens needed to be shutted down before the
     * requested {@link #getAccessToken() AccessToken} can execute tasks. If
     * there are no conflicting tokens an empty collection is returned.
     *
     * <h6>Synchronization transparency</h6>
     * This method is <I>synchronization transparent</I>.
     *
     * @return the conflicting tokens needed to be shutted down before the
     *   requested {@link #getAccessToken() AccessToken} can execute tasks.
     *   This method never returns {@code null}. In case there are no
     *   conflicting tokens an empty collection is returned.
     */
    public Collection<AccessToken<IDType>> getBlockingTokens() {
        return blockingTokens;
    }

    /**
     * Calls the {@link AccessToken#shutdownNow() shutdownNow()} method of
     * the {@link #getBlockingTokens() conflicting tokens}.
     * <P>
     * This method is equivalent to:
     * <P>
     * {@code ExecutorsEx.shutdownTokensNow(result.getBlockingTokens());}
     *
     * @see org.jtrim.concurrent.ExecutorsEx#shutdownExecutorsNow(java.util.Collection)
     */
    public void shutdownBlockingTokensNow() {
        ExecutorsEx.shutdownExecutorsNow(blockingTokens);
    }

    /**
     * Shuts down the {@link #getBlockingTokens() conflicting tokens} and
     * waits until they terminate uninterruptibly.
     * <P>
     * This method uses the {@link AccessToken#shutdownNow() shutdownNow()}
     * method of the conflicting tokens.
     */
    public void releaseBlockingTokens() {
        // shutdownNow() is not a blocking operation, so
        // calling them before release() reduces the latency
        shutdownBlockingTokensNow();
        AccessTokens.releaseTokens(blockingTokens);
    }

    /**
     * Returns the string representation of this right request result in no
     * particular format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        return "AccessResult{" + "accessToken=" + accessToken
                + "blockingTokens=" + blockingTokens + '}';
    }
}
