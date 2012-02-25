/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
 *
 * @author Kelemen Attila
 */
public final class CachedAsyncDataQuery<QueryArgType, DataType>
implements
        AsyncDataQuery<CachedLinkRequest<QueryArgType>, DataType>,
        CachedQuery<QueryArgType> {

    private final Lock mainLock;
    private final Map<QueryArgType, RefList.ElementRef<CachedLink<QueryArgType, DataType>>> cachedLinks;
    private final RefList<CachedLink<QueryArgType, DataType>> cachedLinkList;
    private final int maxCacheSize;
    private boolean consistent;

    private final AsyncDataQuery<? super QueryArgType, DataType> wrappedQuery;

    CachedAsyncDataQuery(
            AsyncDataQuery<? super QueryArgType, DataType> wrappedQuery,
            int maxCacheSize) {
        this.wrappedQuery = wrappedQuery;
        this.cachedLinks = CollectionsEx.newHashMap(maxCacheSize);
        this.cachedLinkList = new RefLinkedList<>();
        this.maxCacheSize = maxCacheSize;
        this.consistent = true;
        this.mainLock = new ReentrantLock();
    }

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
