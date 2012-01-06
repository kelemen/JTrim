/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.async;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import org.jtrim.cache.*;
import org.jtrim.collections.*;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class CachedByIDAsyncDataQuery<QueryArgType, DataType>
implements
        AsyncDataQuery<CachedLinkRequest<DataWithUid<QueryArgType>>, DataWithUid<DataType>>,
        CachedQuery<Object> {

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
        this.refCreator = refCreator;
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
                    cachedResult = null;
                    resultRef.remove();
                    cachedResults.remove(queryID);
                }
                else {
                    cachedResult.updateExpireTime(currentExpireTime);
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
                CachedResultRef<DataType> lastResult;
                lastResult = lastRef.getElement();
                lastResult.updateExpireTime(currentExpireTime);
            }
            else {
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
