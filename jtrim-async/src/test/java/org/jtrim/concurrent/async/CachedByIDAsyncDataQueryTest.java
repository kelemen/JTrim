package org.jtrim.concurrent.async;

import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.cache.JavaRefObjectCache;
import org.jtrim.cache.ReferenceType;
import static org.jtrim.concurrent.async.TestQueryHelper.queryAndWaitResult;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.*;

/**
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class CachedByIDAsyncDataQueryTest {

    public CachedByIDAsyncDataQueryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
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
        testedQuery = AsyncQueries.cacheByID(
                query,
                ReferenceType.HardRefType,
                JavaRefObjectCache.INSTANCE);

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
        testedQuery = AsyncQueries.cacheByID(
                query,
                ReferenceType.NoRefType,
                JavaRefObjectCache.INSTANCE);

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
