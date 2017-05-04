package org.jtrim2.concurrent.async;

import java.util.Collection;

/**
 * Defines a container for cached {@link AsyncDataLink} instances.
 * <P>
 * How {@link AsyncDataLink} instances are added to the cache is completely
 * implementation dependent, this interface only defines methods for removing
 * {@link AsyncDataLink} instances from the cache.
 * <P>
 * This interface is intended to be used by {@link AsyncDataQuery}
 * implementations caching {@link AsyncDataLink} instances.
 *
 * <h3>Thread safety</h3>
 * Implementations of this class must be safe to be used by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @param <CacheRefType> the type of the key used to access
 *   {@link AsyncDataLink} instances in the cache. In case the
 *   {@code CachedLinkContainer} is being used by an {@code AsyncDataQuery},
 *   it can typically be the input used to
 *   {@link AsyncDataQuery#createDataLink(Object) create} the
 *   {@link AsyncDataLink}.
 *
 * @see AsyncQueries#cacheByID(AsyncDataQuery, ReferenceType, ObjectCache, int)
 * @see AsyncQueries#cacheLinks(AsyncDataQuery, int)
 *
 * @author Kelemen Attila
 */
public interface CachedLinkContainer<CacheRefType> {
    /**
     * Removes every {@code AsyncDataLink} instances from the cache currently
     * cached by this query. A subsequent retrieval request from the cache will
     * fail to find any required {@code AsyncDataLink}, therefore causing the
     * implementation to fallback to recreate the required {@code AsyncDataLink}
     * instance.
     *
     * @return the collection of inputs whose {@code AsyncDataLink} instances
     *   were cached before calling this method. This method never returns
     *   {@code null}.
     */
    public Collection<CacheRefType> clearCache();

    /**
     * Removes a cached {@code AsyncDataLink} which was cached by the specified
     * input. A subsequent retrieval request from the cache requiring the
     * {@code AsyncDataLink} associated with the specified input will fail,
     * therefore causing the implementation to fallback to recreate the required
     * {@code AsyncDataLink} instance.
     *
     * @param arg the input for which the associated {@code AsyncDataLink} is to
     *   be removed from the cache. This argument can be {@code null}.
     * @return {@code true} if an {@code AsyncDataLink} instance was cached for
     *   the specified input, {@code false} otherwise. If this method returns
     *   {@code false}, it did nothing because there was no
     *   {@code AsyncDataLink} associated with the given input in the cache.
     */
    public boolean removeFromCache(CacheRefType arg);
}
