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
import org.jtrim.cache.JavaRefObjectCache;
import org.jtrim.cache.ObjectCache;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cache.VolatileReference;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class CachedByIDAsyncDataQuery<QueryArgType, DataType>
implements
        AsyncDataQuery<CachedLinkRequest<DataWithUid<QueryArgType>>, DataWithUid<DataType>>,
        CachedQuery<Object> {

    // As of the current implementation, as long as a requested data link
    // does not return its final results, the in progress data link will not be
    // returned, instead a new data link will be created parallel with the
    // in progress data link. This is inefficient and a better implementation
    // would be preferable which makes use of in progress data link while the
    // final data is not available.
    //
    // Note that this is not simple to implement because the main reason for
    // this class is not to store reference to the input longer than necessary.
    // If optimized implementation is provided, this must be kept in mind.

    private static final AsyncDataState CACHED_STATE
            = new SimpleDataState("Result was cached.", 1.0);

    private final Lock mainLock;
    private final Map<Object, RefList.ElementRef<CachedResultRef<DataType>>> cachedResults;
    private final RefList<CachedResultRef<DataType>> cachedResultList;
    private final int maxCacheSize;
    private boolean consistent;

    private final ReferenceType refType;
    private final ObjectCache refCreator;

    private final AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery;
    private final OutputConverter<DataType> outputConverter;

    CachedByIDAsyncDataQuery(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery,
            ReferenceType refType,
            ObjectCache refCreator,
            int maxCacheSize) {

        ExceptionHelper.checkNotNullArgument(refType, "refType");
        ExceptionHelper.checkNotNullArgument(wrappedQuery, "wrappedQuery");

        this.wrappedQuery = wrappedQuery;
        this.cachedResults = CollectionsEx.newHashMap(maxCacheSize);
        this.cachedResultList = new RefLinkedList<>();
        this.maxCacheSize = maxCacheSize;
        this.mainLock = new ReentrantLock();
        this.consistent = true;
        this.outputConverter = new OutputConverter<>();

        this.refType = refType;
        this.refCreator = refCreator != null
                ? refCreator
                : JavaRefObjectCache.INSTANCE;
    }

    @Override
    public Collection<Object> clearCache() {
        List<Object> removedIDs = new ArrayList<>();

        mainLock.lock();
        try {
            removedIDs.addAll(cachedResults.keySet());
            cachedResults.clear();
            cachedResultList.clear();
            consistent = true;
        } finally {
            mainLock.unlock();
        }

        return removedIDs;
    }

    @Override
    public boolean removeFromCache(Object arg) {
        boolean wasRemoved = false;

        mainLock.lock();
        try {
            repairConsistency();

            consistent = false;

            RefList.ElementRef<CachedResultRef<DataType>> listRef;
            listRef = cachedResults.remove(arg);
            if (listRef != null) {
                listRef.remove();
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
    public AsyncDataLink<DataWithUid<DataType>> createDataLink(CachedLinkRequest<DataWithUid<QueryArgType>> arg) {
        CachedResultRef<DataType> cachedResult = null;

        Object queryID = arg.getQueryArg().getID();

        final long currentExpireTime = System.nanoTime()
                + arg.getCacheExpire(TimeUnit.NANOSECONDS);

        mainLock.lock();
        try {
            repairConsistency();

            consistent = false;

            RefList.ElementRef<CachedResultRef<DataType>> resultRef;
            resultRef = cachedResults.get(queryID);

            if (resultRef != null) {
                cachedResult = resultRef.getElement();

                if (cachedResult.isExpired()) {
                    // Remove from the data from the cache if it was in the
                    // cache for too long.
                    cachedResult = null;
                    resultRef.remove();
                    cachedResults.remove(queryID);
                }
                else {
                    // Set the time the data is allowed to be in the cache.
                    // Notice that updateExpireTime may only set the expire time
                    // to a lower value.
                    cachedResult.updateExpireTime(currentExpireTime);

                    // Since this data was the last data to be accessed, others
                    // should be removed first when necessary.
                    resultRef.moveLast();
                }
            }

            consistent = true;
        } finally {
            mainLock.unlock();
        }

        DataWithUid<DataType> result = cachedResult != null
                ? cachedResult.tryGetResult(queryID)
                : null;

        if (result != null) {
            // Note that only final (complete) data can be cached, so we can
            // safely return it if available.
            return AsyncDatas.createPreparedLink(result, CACHED_STATE);
        }
        else {
            QueryArgType queryArg = arg.getQueryArg().getData();

            AsyncDataLink<? extends DataType> wrappedLink;
            wrappedLink = wrappedQuery.createDataLink(queryArg);

            AsyncDataLink<RefCachedData<DataType>> cachedLink;
            cachedLink = AsyncDatas.refCacheResult(
                    wrappedLink, refType, refCreator, 0, TimeUnit.NANOSECONDS);

            final Object inputID = arg.getQueryArg().getID();
            final long expireTime = arg.getCacheExpire(TimeUnit.NANOSECONDS);

            AsyncDataLink<MarkedData<DataType>> markedLink;
            markedLink = AsyncDatas.convertResult(cachedLink,
                    new DataMarker<DataType>(inputID, expireTime));

            AsyncDataLink<MarkedData<DataType>> cacheStorer;
            cacheStorer = new CacheStorerLink(markedLink);

            return new AsyncDataLinkConverter<>(cacheStorer, outputConverter);
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(256);
        result.append("Use ");
        AsyncFormatHelper.appendIndented(wrappedQuery, result);
        result.append("\nCache results by ID. Max. cache size: ");
        result.append(maxCacheSize);
        result.append("\nCache: ");
        result.append(refType);
        result.append(" (");
        result.append(refCreator);
        result.append(")");

        return result.toString();
    }

    private void storeData(MarkedVolatileData<DataType> markedData) {
        final long currentExpireTime = System.nanoTime()
                + markedData.getExpireNanos();

        Object inputID = markedData.getInputID();

        CachedResultRef<DataType> newCachedResult;
        newCachedResult = new CachedResultRef<>(
                inputID,
                markedData.getExpireNanos(),
                markedData.getDataID(),
                markedData.getDataRef());

        mainLock.lock();
        try {
            repairConsistency();

            consistent = false;

            RefList.ElementRef<CachedResultRef<DataType>> lastRef;
            lastRef = cachedResults.get(inputID);

            if (lastRef != null) {
                // If the data was in the cache, decrease the expire time if
                // this request said a lower value.
                CachedResultRef<DataType> lastResult;
                lastResult = lastRef.getElement();
                lastResult.updateExpireTime(currentExpireTime);
            }
            else {
                // Add the new data to the cache and remove one from it if the
                // cache is full.
                // Note that newCachedResult is only used here but it is created
                // outside of the mainLock.lock() for slightly better
                // concurrency.
                RefList.ElementRef<CachedResultRef<DataType>> newRef;

                newRef = cachedResultList.addLastGetReference(newCachedResult);
                cachedResults.put(inputID, newRef);

                if (cachedResultList.size() > maxCacheSize) {
                    RefList.ElementRef<CachedResultRef<DataType>> oldRef;
                    oldRef = cachedResultList.getFirstReference();
                    if (oldRef != null) {
                        // Notice that remove must return "oldRef"
                        cachedResults.remove(oldRef.getElement().getInputID());
                        oldRef.remove();
                    }
                }
            }

            consistent = true;
        } finally {
            mainLock.unlock();
        }
    }

    private static class MarkedVolatileData<DataType> {
        private final Object inputID;
        private final Object dataID;
        private final VolatileReference<DataType> dataRef;
        private final long expireNanos;

        public MarkedVolatileData(MarkedData<DataType> data) {
            this.inputID = data.getInputID();
            this.dataID = data.getID();
            this.dataRef = data.getDataRef();
            this.expireNanos = data.getExpireNanos();
        }

        public Object getDataID() {
            return dataID;
        }

        public VolatileReference<DataType> getDataRef() {
            return dataRef;
        }

        public long getExpireNanos() {
            return expireNanos;
        }

        public Object getInputID() {
            return inputID;
        }
    }

    private static class MarkedData<DataType> {
        private final Object inputID;
        private final Object dataID;
        private final RefCachedData<DataType> data;
        private final long expireNanos;

        public MarkedData(
                Object inputID,
                DataWithUid<RefCachedData<DataType>> data,
                long expireNanos) {

            this.inputID = inputID;
            this.dataID = data.getID();
            this.data = data.getData();

            this.expireNanos = expireNanos;
        }

        public MarkedData(Object inputID, Object dataID,
                RefCachedData<DataType> data, long expireNanos) {
            this.inputID = inputID;
            this.dataID = dataID;
            this.data = data;
            this.expireNanos = expireNanos;
        }

        public long getExpireNanos() {
            return expireNanos;
        }

        public Object getInputID() {
            return inputID;
        }

        public Object getID() {
            return dataID;
        }

        public DataType getData() {
            return data.getData();
        }

        public VolatileReference<DataType> getDataRef() {
            return data.getDataRef();
        }
    }

    private static class DataMarker<DataType>
    implements
            DataConverter<RefCachedData<DataType>, MarkedData<DataType>> {

        private final Object inputID;
        private final long expireNanos;

        public DataMarker(Object inputID, long expireNanos) {
            this.inputID = inputID;
            this.expireNanos = expireNanos;
        }

        @Override
        public MarkedData<DataType> convertData(RefCachedData<DataType> data) {
            return new MarkedData<>(inputID,
                    new DataWithUid<>(data, new Object()), expireNanos);
        }
    }

    private static class OutputConverter<DataType>
    implements
            DataConverter<MarkedData<DataType>, DataWithUid<DataType>> {

        @Override
        public DataWithUid<DataType> convertData(MarkedData<DataType> data) {
            return new DataWithUid<>(data.getData(), data.getID());
        }
    }

    private class CacheStorerLink
    extends
            DataInterceptorLink<MarkedData<DataType>> {

        private volatile MarkedVolatileData<DataType> lastData;

        public CacheStorerLink(AsyncDataLink<? extends MarkedData<DataType>> wrappedLink) {
            super(wrappedLink);

            this.lastData = null;
        }

        @Override
        protected boolean onDataArrive(MarkedData<DataType> newData) {
            lastData = new MarkedVolatileData<>(newData);
            return true;
        }

        @Override
        protected boolean onDoneReceive(AsyncReport report) {
            MarkedVolatileData<DataType> data = lastData;

            if (data != null) {
                storeData(data);
                // Allow it to be garbage collected.
                lastData = null;
            }

            return true;
        }
    }

    private static class CachedResultRef<DataType> {
        private final Object inputID;
        private final Object resultID;
        private final VolatileReference<DataType> result;

        private final long createTime;
        private long expireTime;

        public CachedResultRef(
                CachedLinkRequest<DataWithUid<DataType>> arg,
                long resultID,
                VolatileReference<DataType> result) {

            this(arg.getQueryArg().getID(),
                    arg.getCacheExpire(TimeUnit.NANOSECONDS),
                    resultID,
                    result);
        }

        public CachedResultRef(
                Object inputID,
                long expireNanos,
                Object resultID,
                VolatileReference<DataType> result) {

            this.inputID = inputID;
            this.resultID = resultID;
            this.result = result;

            this.createTime = System.nanoTime();
            this.expireTime = expireNanos + this.createTime;
        }

        public Object getInputID() {
            return inputID;
        }

        public Object getResultID() {
            return resultID;
        }

        public DataWithUid<DataType> tryGetResult(Object senderID) {
            DataType data = senderID == inputID ? result.get() : null;

            return data != null
                    ? new DataWithUid<>(data, resultID)
                    : null;
        }

        public void updateExpireTime() {
            updateExpireTime(System.nanoTime());
        }

        public void updateExpireTime(long newExpireTime) {
            // To play safe we set the expire time to the lowest value.
            // So it will not remain cached forever.
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
