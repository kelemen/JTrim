package org.jtrim2.concurrent.query;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim2.cache.JavaRefObjectCache;
import org.jtrim2.cache.ObjectCache;
import org.jtrim2.cache.ReferenceType;
import org.jtrim2.cancel.Cancellation;
import org.junit.Test;

import static org.jtrim2.concurrent.query.AsyncMocks.*;
import static org.jtrim2.concurrent.query.TestQueryHelper.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CachedByIDAsyncDataQueryTest {
    public <QueryArgType, DataType> CachedByIDAsyncDataQuery<QueryArgType, DataType> create(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery,
            ReferenceType refType,
            ObjectCache refCreator,
            int maxCacheSize) {
        return new CachedByIDAsyncDataQuery<>(wrappedQuery,
                refType, refCreator, maxCacheSize);
    }

    @Test
    public void testSyncHardRef1() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        MarkWithIDConverter<DummyData> marker = new MarkWithIDConverter<>();
        IdentityQuery<DummyData> query = new IdentityQuery<>(callCount);

        final CachedLinkRequest<DataWithUid<DummyData>> request1;
        final CachedLinkRequest<DataWithUid<DummyData>> request2;

        request1 = new CachedLinkRequest<>(marker.convertData(new DummyData()));
        request2 = new CachedLinkRequest<>(marker.convertData(new DummyData()));

        CachedByIDAsyncDataQuery<DummyData, DummyData> testedQuery;
        testedQuery = create(
                query,
                ReferenceType.HardRefType,
                JavaRefObjectCache.INSTANCE,
                128);

        DataWithUid<DummyData> result;

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 1);

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 1);

        testedQuery.clearCache();

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 2);

        result = queryAndWaitResult(testedQuery, request2);
        assertSame(result.getData(), request2.getQueryArg().getData());
        assertEquals(callCount.get(), 3);

        result = queryAndWaitResult(testedQuery, request2);
        assertSame(result.getData(), request2.getQueryArg().getData());
        assertEquals(callCount.get(), 3);

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 3);
    }

    @Test
    public void testSyncHardRef2() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        MarkWithIDConverter<DummyData> marker = new MarkWithIDConverter<>();
        IdentityQuery<DummyData> query = new IdentityQuery<>(callCount);

        final CachedLinkRequest<DataWithUid<DummyData>> request1;
        final CachedLinkRequest<DataWithUid<DummyData>> request2;

        request1 = new CachedLinkRequest<>(marker.convertData(new DummyData()));
        request2 = new CachedLinkRequest<>(marker.convertData(new DummyData()));

        CachedByIDAsyncDataQuery<DummyData, DummyData> testedQuery;
        testedQuery = new CachedByIDAsyncDataQuery<>(
                query,
                ReferenceType.HardRefType,
                JavaRefObjectCache.INSTANCE,
                1);

        DataWithUid<DummyData> result;

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 1);

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 1);

        testedQuery.clearCache();

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 2);

        result = queryAndWaitResult(testedQuery, request2);
        assertSame(result.getData(), request2.getQueryArg().getData());
        assertEquals(callCount.get(), 3);

        result = queryAndWaitResult(testedQuery, request2);
        assertSame(result.getData(), request2.getQueryArg().getData());
        assertEquals(callCount.get(), 3);

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 4);
    }

    @Test
    public void testSyncNoRef1() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);
        MarkWithIDConverter<DummyData> marker = new MarkWithIDConverter<>();
        IdentityQuery<DummyData> query = new IdentityQuery<>(callCount);

        final CachedLinkRequest<DataWithUid<DummyData>> request1;
        final CachedLinkRequest<DataWithUid<DummyData>> request2;

        request1 = new CachedLinkRequest<>(marker.convertData(new DummyData()));
        request2 = new CachedLinkRequest<>(marker.convertData(new DummyData()));

        CachedByIDAsyncDataQuery<DummyData, DummyData> testedQuery;
        testedQuery = create(
                query,
                ReferenceType.NoRefType,
                JavaRefObjectCache.INSTANCE,
                128);

        DataWithUid<DummyData> result;

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 1);

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 2);

        testedQuery.clearCache();

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 3);

        result = queryAndWaitResult(testedQuery, request2);
        assertSame(result.getData(), request2.getQueryArg().getData());
        assertEquals(callCount.get(), 4);

        result = queryAndWaitResult(testedQuery, request2);
        assertSame(result.getData(), request2.getQueryArg().getData());
        assertEquals(callCount.get(), 5);

        result = queryAndWaitResult(testedQuery, request1);
        assertSame(result.getData(), request1.getQueryArg().getData());
        assertEquals(callCount.get(), 6);
    }

    @Test
    public void testSubsequentCallsDoesNotRecreateWrappedLink() {
        AsyncDataQuery<DummyData, DummyData> query = mockQuery();
        ManualDataLink<DummyData> wrappedLink = new ManualDataLink<>();

        stub(query.createDataLink(any(DummyData.class))).toReturn(wrappedLink);

        CachedByIDAsyncDataQuery<DummyData, DummyData> testedQuery;
        testedQuery = create(query, ReferenceType.HardRefType,
                JavaRefObjectCache.INSTANCE, 128);

        DummyData data = new DummyData();
        Object dataID = new Object();
        DataWithUid<DummyData> markedData = new DataWithUid<>(data, dataID);

        final CachedLinkRequest<DataWithUid<DummyData>> request
                = new CachedLinkRequest<>(markedData);

        AsyncDataListener<DataWithUid<DummyData>> listener = mockListener();
        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        wrappedLink.onDataArrive(new DummyData());
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        verify(query).createDataLink(any(DummyData.class));
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testSubsequentCallsDoesNotRecreateWrappedLinkVeryLargeTimeout() {
        AsyncDataQuery<DummyData, DummyData> query = mockQuery();
        ManualDataLink<DummyData> wrappedLink = new ManualDataLink<>();

        stub(query.createDataLink(any(DummyData.class))).toReturn(wrappedLink);

        CachedByIDAsyncDataQuery<DummyData, DummyData> testedQuery;
        testedQuery = create(query, ReferenceType.HardRefType,
                JavaRefObjectCache.INSTANCE, 128);

        DummyData data = new DummyData();
        Object dataID = new Object();
        DataWithUid<DummyData> markedData = new DataWithUid<>(data, dataID);

        final CachedLinkRequest<DataWithUid<DummyData>> request
                = new CachedLinkRequest<>(markedData, Long.MAX_VALUE, TimeUnit.DAYS);

        AsyncDataListener<DataWithUid<DummyData>> listener = mockListener();
        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        wrappedLink.onDataArrive(new DummyData());
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        verify(query).createDataLink(any(DummyData.class));
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testRemoveFromCacheNotCached() {
        AsyncDataQuery<DummyData, DummyData> query = mockQuery();
        ManualDataLink<DummyData> wrappedLink = new ManualDataLink<>();

        stub(query.createDataLink(any(DummyData.class))).toReturn(wrappedLink);

        CachedByIDAsyncDataQuery<DummyData, DummyData> testedQuery;
        testedQuery = create(query, ReferenceType.HardRefType,
                JavaRefObjectCache.INSTANCE, 128);

        DummyData data = new DummyData();
        Object dataID = new Object();
        DataWithUid<DummyData> markedData = new DataWithUid<>(data, dataID);

        final CachedLinkRequest<DataWithUid<DummyData>> request
                = new CachedLinkRequest<>(markedData);

        AsyncDataListener<DataWithUid<DummyData>> listener = mockListener();
        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        wrappedLink.onDataArrive(new DummyData());
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        testedQuery.removeFromCache(new Object());
        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        verify(query).createDataLink(any(DummyData.class));
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testRemoveFromCache() {
        AsyncDataQuery<DummyData, DummyData> query = mockQuery();
        ManualDataLink<DummyData> wrappedLink = new ManualDataLink<>();

        stub(query.createDataLink(any(DummyData.class))).toReturn(wrappedLink);

        CachedByIDAsyncDataQuery<DummyData, DummyData> testedQuery;
        testedQuery = create(query, ReferenceType.HardRefType,
                JavaRefObjectCache.INSTANCE, 128);

        DummyData data = new DummyData();
        Object dataID = new Object();
        DataWithUid<DummyData> markedData = new DataWithUid<>(data, dataID);

        final CachedLinkRequest<DataWithUid<DummyData>> request
                = new CachedLinkRequest<>(markedData);

        AsyncDataListener<DataWithUid<DummyData>> listener = mockListener();
        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        wrappedLink.onDataArrive(new DummyData());
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        testedQuery.removeFromCache(dataID);
        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        verify(query, times(2)).createDataLink(any(DummyData.class));
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testCacheExpire() {
        AsyncDataQuery<DummyData, DummyData> query = mockQuery();
        ManualDataLink<DummyData> wrappedLink = new ManualDataLink<>();

        stub(query.createDataLink(any(DummyData.class))).toReturn(wrappedLink);

        CachedByIDAsyncDataQuery<DummyData, DummyData> testedQuery;
        testedQuery = create(query, ReferenceType.HardRefType,
                JavaRefObjectCache.INSTANCE, 128);

        DummyData data = new DummyData();
        Object dataID = new Object();
        DataWithUid<DummyData> markedData = new DataWithUid<>(data, dataID);

        final CachedLinkRequest<DataWithUid<DummyData>> request
                = new CachedLinkRequest<>(markedData, 0L, TimeUnit.NANOSECONDS);

        AsyncDataListener<DataWithUid<DummyData>> listener = mockListener();
        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        wrappedLink.onDataArrive(new DummyData());
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        verify(query, times(2)).createDataLink(any(DummyData.class));
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testDecreaseCacheExpire() {
        AsyncDataQuery<DummyData, DummyData> query = mockQuery();
        ManualDataLink<DummyData> wrappedLink = new ManualDataLink<>();

        stub(query.createDataLink(any(DummyData.class))).toReturn(wrappedLink);

        CachedByIDAsyncDataQuery<DummyData, DummyData> testedQuery;
        testedQuery = create(query, ReferenceType.HardRefType,
                JavaRefObjectCache.INSTANCE, 128);

        DummyData data = new DummyData();
        Object dataID = new Object();
        DataWithUid<DummyData> markedData = new DataWithUid<>(data, dataID);

        final CachedLinkRequest<DataWithUid<DummyData>> request
                = new CachedLinkRequest<>(markedData, 0L, TimeUnit.NANOSECONDS);

        final CachedLinkRequest<DataWithUid<DummyData>> requestNoTimeout
                = new CachedLinkRequest<>(markedData, 1L, TimeUnit.DAYS);

        AsyncDataListener<DataWithUid<DummyData>> listener = mockListener();

        testedQuery.createDataLink(requestNoTimeout).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        wrappedLink.onDataArrive(new DummyData());
        wrappedLink.onDoneReceive(AsyncReport.SUCCESS);

        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);
        verify(query).createDataLink(any(DummyData.class));

        testedQuery.createDataLink(request).getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        verify(query, times(2)).createDataLink(any(DummyData.class));
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testToString() {
        assertNotNull(create(mockQuery(),
                ReferenceType.NoRefType,
                JavaRefObjectCache.INSTANCE,
                128).toString());
    }

    private static class DummyData {
    }

    private static class IdentityQuery<DataType>
    implements
            AsyncDataQuery<DataType, DataType> {

        private final AtomicInteger callCount;

        public IdentityQuery(AtomicInteger callCount) {
            this.callCount = callCount;
        }

        @Override
        public AsyncDataLink<DataType> createDataLink(DataType arg) {
            callCount.getAndIncrement();
            return AsyncLinks.createPreparedLink(arg,
                    new SimpleDataState("TestState", 1.0));
        }
    }
}
