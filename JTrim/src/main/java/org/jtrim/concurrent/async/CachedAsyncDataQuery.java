package org.jtrim.concurrent.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an {@code AsyncDataQuery} which caches {@code AsyncDataLink}
 * instances based on the input of the query. That is, this query may return the
 * same {@code AsyncDataLink} for the same input (based on {@code equals}).
 * The data provided by the  {@code AsyncDataLink} instances are not cached by
 * this query. Those can be cached by using the
 * {@link AsyncDatas#cacheResults(AsyncDataQuery) AsyncDatas.cacheResults}
 * method.
 * <P>
 * This query caches only at most a specified number of {@code AsyncDataLink}
 * instances and when this limit is exceeded, the one used least recently
 * is discarded from the cache. Also a lease time can be specified when
 * requesting an {@code AsyncDataLink} when it must be removed from the cache
 * and the {@code AsyncDataLink} needs to be recreated if requested again.
 * <P>
 * Note that it is possible to manually remove cached items from the
 * returned {@code AsyncDataQuery} using its
 * {@link CachedAsyncDataQuery#clearCache() clearCache} and
 * {@link CachedAsyncDataQuery#removeFromCache(Object) removeFromCache}
 * methods.
 *
 * <h3>Creating instances</h3>
 * It is not possible to directly instantiate this class, to create instances
 * of this class use the {@link AsyncDatas#cacheLinks(AsyncDataQuery, int)}
 * method.
 *
 * <h3>Thread safety</h3>
 * This class is safe to be used by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * This class is not <I>synchronization transparent</I> but methods of this
 * class return reasonably fast. That is, they can be executed by methods need
 * to be responsive (e.g.: listeners, methods called on the AWT event dispatch
 * thread).
 *
 * @param <QueryArgType> the type of the input actually used to query the data.
 *   Note however, that this query uses
 *   {@link CachedLinkRequest CachedLinkRequest&lt;QueryArgType&gt;} as input.
 * @param <DataType> the type of the data to be retrieved. As with every
 *   {@code AsyncDataQuery}, this type is strongly recommended to be immutable
 *   or effectively immutable.
 *
 * @see AsyncDatas#cacheLinks(AsyncDataQuery)
 * @see AsyncDatas#cacheLinks(AsyncDataQuery, int)
 *
 * @author Kelemen Attila
 */
public final class CachedAsyncDataQuery<QueryArgType, DataType>
implements
        AsyncDataQuery<CachedLinkRequest<QueryArgType>, DataType>,
        CachedLinkContainer<QueryArgType> {

    private final Lock mainLock;
    private final Map<QueryArgType, RefList.ElementRef<CachedLink<QueryArgType, DataType>>> cachedLinks;
    private final RefList<CachedLink<QueryArgType, DataType>> cachedLinkList;
    private final int maxCacheSize;
    private boolean consistent;

    private final AsyncDataQuery<? super QueryArgType, DataType> wrappedQuery;

    CachedAsyncDataQuery(
            AsyncDataQuery<? super QueryArgType, DataType> wrappedQuery,
            int maxCacheSize) {

        ExceptionHelper.checkNotNullArgument(wrappedQuery, "wrappedQuery");
        ExceptionHelper.checkArgumentInRange(maxCacheSize, 0, Integer.MAX_VALUE, "maxCacheSize");

        this.wrappedQuery = wrappedQuery;
        this.cachedLinks = CollectionsEx.newHashMap(maxCacheSize);
        this.cachedLinkList = new RefLinkedList<>();
        this.maxCacheSize = maxCacheSize;
        this.consistent = true;
        this.mainLock = new ReentrantLock();
    }

    /**
     * {@inheritDoc}
     * <P>
     * Note that this method takes linear time in the number of cached
     * {@code AsyncDataLink} instances.
     */
    @Override
    public Collection<QueryArgType> clearCache() {
        List<QueryArgType> removed = new ArrayList<>();

        mainLock.lock();
        try {
            removed.addAll(cachedLinks.keySet());

            cachedLinks.clear();
            cachedLinkList.clear();
            consistent = true;
        } finally {
            mainLock.unlock();
        }

        return removed;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean removeFromCache(QueryArgType arg) {
        boolean wasRemoved = false;
        mainLock.lock();
        try {
            consistent = false;

            RefList.ElementRef<CachedLink<QueryArgType, DataType>> linkRef;

            linkRef = cachedLinks.remove(arg);
            if (linkRef != null) {
                linkRef.remove();
                wasRemoved = true;
            }

            consistent = true;
        } finally {
            mainLock.unlock();
        }

        return wasRemoved;
    }

    private void repairConsistency() {
        if (!consistent) {
            clearCache();
        }
    }

    /**
     * Returns an {@code AsyncDataLink} which will provide data based on the
     * specified input. The returned {@code AsyncDataLink} may be retrieved from
     * the cache rather than actually requesting it from the underlying
     * {@code AsyncDataLink}.
     * <P>
     * Regardless if the requested {@code AsyncDataLink} was cached or not, this
     * method returns immediately without blocking.
     *
     * @param arg the input argument which is to be used to retrieve the data.
     *   This argument contains information about how the returned
     *   {@code AsyncDataLink} may be cached and the actual
     *   {@link CachedLinkRequest#getQueryArg() input} which determines what
     *   data is to be retrieved by the returned {@code AsyncDataLink}. This
     *   argument cannot be {@code null}.
     * @return the {@code AsyncDataLink} which will provide data based on the
     *   specified input. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    @Override
    public AsyncDataLink<DataType> createDataLink(
            CachedLinkRequest<QueryArgType> arg) {

        ExceptionHelper.checkNotNullArgument(arg, "arg");

        CachedLink<QueryArgType, DataType> cachedLink = null;
        QueryArgType queryArg = arg.getQueryArg();

        final long currentExpireTime = System.nanoTime()
                + arg.getCacheExpire(TimeUnit.NANOSECONDS);

        mainLock.lock();
        try {
            repairConsistency();

            consistent = false;

            RefList.ElementRef<CachedLink<QueryArgType, DataType>> linkRef;
            linkRef = cachedLinks.get(queryArg);

            if (linkRef != null) {
                cachedLink = linkRef.getElement();

                if (cachedLink.isExpired()) {
                    cachedLink = null;
                    linkRef.remove();
                    cachedLinks.remove(queryArg);
                }
                else {
                    //cachedLink.setLastAccessTime(System.nanoTime());
                    cachedLink.updateExpireTime(currentExpireTime);
                    linkRef.moveLast();
                }
            }

            consistent = true;
        } finally {
            mainLock.unlock();
        }

        if (cachedLink != null) {
            return cachedLink.getCachedLink();
        }
        else {
            AsyncDataLink<DataType> newLink;
            newLink = wrappedQuery.createDataLink(queryArg);
            CachedLink<QueryArgType, DataType> newCachedLink;
            newCachedLink = new CachedLink<>(arg, newLink);

            mainLock.lock();
            try {
                repairConsistency();

                consistent = false;

                RefList.ElementRef<CachedLink<QueryArgType, DataType>> lastRef;
                lastRef = cachedLinks.get(queryArg);

                if (lastRef != null) {
                    CachedLink<QueryArgType, DataType> lastLink;
                    lastLink = lastRef.getElement();
                    lastLink.updateExpireTime(currentExpireTime);
                    newLink = lastLink.getCachedLink();
                }
                else {
                    RefList.ElementRef<CachedLink<QueryArgType, DataType>> newRef;

                    newRef = cachedLinkList.addGetReference(cachedLinkList.size(), newCachedLink);
                    cachedLinks.put(queryArg, newRef);

                    if (cachedLinkList.size() > maxCacheSize) {
                        RefList.ElementRef<CachedLink<QueryArgType, DataType>> oldRef;
                        oldRef = cachedLinkList.getFirstReference();
                        if (oldRef != null) {
                            // Notice that remove must return "oldRef"
                            cachedLinks.remove(oldRef.getElement().getQueryArg());
                            oldRef.remove();
                        }
                    }
                }

                consistent = true;
            } finally {
                mainLock.unlock();
            }

            return newLink;
        }
    }

    /**
     * Returns the string representation of this {@code AsyncDataQuery} in no
     * particular format.
     * <P>
     * This method is intended to be used for debugging only.
     *
     * @return the string representation of this object in no particular format.
     *   This method never returns {@code null}.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("Use ");
        AsyncFormatHelper.appendIndented(wrappedQuery, result);
        result.append("\nCache links. Max. cache size: ");
        result.append(maxCacheSize);

        return result.toString();
    }

    private static class CachedLink<QueryArgType, DataType> {
        private final QueryArgType arg;
        private final AsyncDataLink<DataType> cachedLink;
        private final long createTime;
        private long expireTime;

        public CachedLink(
                CachedLinkRequest<QueryArgType> arg,
                AsyncDataLink<DataType> cachedLink) {

            this.arg = arg.getQueryArg();
            this.cachedLink = cachedLink;
            this.createTime = System.nanoTime();
            this.expireTime = arg.getCacheExpire(TimeUnit.NANOSECONDS)
                    + this.createTime;
        }

        public QueryArgType getQueryArg() {
            return arg;
        }

        public AsyncDataLink<DataType> getCachedLink() {
            return cachedLink;
        }

        public void updateExpireTime(long newExpireTime) {
            // To play safe we set the expire time to the lowest value.
            if (newExpireTime < expireTime) {
                expireTime = newExpireTime;
            }
        }

        public long getExpireTime() {
            return expireTime;
        }

        public boolean isExpired() {
            return System.nanoTime() >= getExpireTime();
        }
    }
}
