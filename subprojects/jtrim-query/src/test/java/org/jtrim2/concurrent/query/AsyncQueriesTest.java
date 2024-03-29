package org.jtrim2.concurrent.query;

import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import org.jtrim2.cache.GenericReference;
import org.jtrim2.cache.ReferenceType;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.jtrim2.concurrent.query.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncQueriesTest {
    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(AsyncQueries.class);
    }

    private static <QueryArgType, DataType> void doTestSimpleSame(
            AsyncDataQuery<QueryArgType, DataType> query,
            QueryArgType input,
            final DataType expectedData) {

        doTestSimpleVerifyListener(query, input, (AsyncDataListener<DataType> listener) -> {
            verify(listener).onDataArrive(same(expectedData));
        });
    }

    private static <QueryArgType, DataType> void doTestSimple(
            AsyncDataQuery<QueryArgType, DataType> query,
            QueryArgType input,
            final ArgumentChecker<DataType> verifier) {
        doTestSimpleVerifyListener(query, input, (AsyncDataListener<DataType> listener) -> {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<DataType> arg = (ArgumentCaptor<DataType>) ArgumentCaptor.forClass(Object.class);

            verify(listener).onDataArrive(arg.capture());
            verifier.verifyArgument(arg.getValue());
        });
    }

    private static <QueryArgType, DataType> void doTestSimpleVerifyListener(
            AsyncDataQuery<QueryArgType, DataType> query,
            QueryArgType input,
            ListenerVerifier<DataType> verifier) {

        AsyncDataLink<DataType> link = query.createDataLink(input);

        AsyncDataListener<DataType> listener = mockListener();

        final WaitableSignal endSignal = new WaitableSignal();
        doAnswer((InvocationOnMock invocation) -> {
            endSignal.signal();
            return null;
        }).when(listener).onDoneReceive(any(AsyncReport.class));

        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        if (!endSignal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, 10, TimeUnit.SECONDS)) {
            throw new RuntimeException("onDoneReceive has never been called.");
        }

        verifier.verifyListener(listener);
    }

    @SuppressWarnings("unchecked")
    private static <OldDataType, NewDataType> DataConverter<OldDataType, NewDataType> mockConverter() {
        return mock(DataConverter.class);
    }

    /**
     * Test of cacheResults method, of class AsyncQueries.
     */
    @Test
    public void testCacheResults() {
        CachedDataRequest<Object> input = new CachedDataRequest<>(new Object());
        Object output = new Object();

        ConstQuery<Object, Object> wrappedQuery = new ConstQuery<>(input.getQueryArg(), output);
        AsyncDataQuery<CachedDataRequest<Object>, Object> query = AsyncQueries.cacheResults(wrappedQuery);

        assertTrue(query instanceof AsyncCachedLinkQuery);
        doTestSimpleSame(query, input, output);
    }

    /**
     * Test of cacheLinks method, of class AsyncQueries.
     */
    @Test
    public void testCacheLinks_AsyncDataQuery() {
        CachedLinkRequest<Object> input = new CachedLinkRequest<>(new Object());
        Object output = new Object();

        ConstQuery<Object, Object> wrappedQuery = new ConstQuery<>(input.getQueryArg(), output);
        CachedAsyncDataQuery<Object, Object> query = AsyncQueries.cacheLinks(wrappedQuery);
        doTestSimpleSame(query, input, output);
    }

    /**
     * Test of cacheLinks method, of class AsyncQueries.
     */
    @Test
    public void testCacheLinks_AsyncDataQuery_int() {
        CachedLinkRequest<Object> input = new CachedLinkRequest<>(new Object());
        Object output = new Object();

        ConstQuery<Object, Object> wrappedQuery = new ConstQuery<>(input.getQueryArg(), output);
        CachedAsyncDataQuery<Object, Object> query = AsyncQueries.cacheLinks(wrappedQuery, 128);
        doTestSimpleSame(query, input, output);
    }

    /**
     * Test of cacheByID method, of class AsyncQueries.
     */
    @Test
    public void testCacheByID_3args() {
        CachedLinkRequest<DataWithUid<Object>> input
                = new CachedLinkRequest<>(new DataWithUid<>(new Object(), new Object()));
        final DataWithUid<Object> output = new DataWithUid<>(new Object(), new Object());

        ConstQuery<Object, Object> wrappedQuery = new ConstQuery<>(
                input.getQueryArg().getData(),
                output.getData());

        CachedByIDAsyncDataQuery<Object, Object> query = AsyncQueries.cacheByID(
                wrappedQuery, ReferenceType.HardRefType, null);

        doTestSimple(query, input, (DataWithUid<Object> arg) -> {
            assertSame(output.getData(), arg.getData());
        });
    }

    /**
     * Test of cacheByID method, of class AsyncQueries.
     */
    @Test
    public void testCacheByID_4args() {
        CachedLinkRequest<DataWithUid<Object>> input
                = new CachedLinkRequest<>(new DataWithUid<>(new Object(), new Object()));
        final DataWithUid<Object> output = new DataWithUid<>(new Object(), new Object());

        ConstQuery<Object, Object> wrappedQuery = new ConstQuery<>(
                input.getQueryArg().getData(),
                output.getData());

        CachedByIDAsyncDataQuery<Object, Object> query = AsyncQueries.cacheByID(
                wrappedQuery, ReferenceType.HardRefType, null, 1024);

        doTestSimple(query, input, (DataWithUid<Object> arg) -> {
            assertSame(output.getData(), arg.getData());
        });
    }

    private void testConvertResults_AsyncDataQuery_DataConverter(
            BiFunction<
                AsyncDataQuery<Object, Object>,
                DataConverter<Object, Object>,
                AsyncDataQuery<Object, Object>
            > queryFactory) {
        Object input = new Object();
        Object output = new Object();
        Object converted = new Object();

        ConstQuery<Object, Object> inputQuery = new ConstQuery<>(input, output);
        DataConverter<Object, Object> converter = mockConverter();

        when(converter.convertData(same(output))).thenReturn(converted);

        AsyncDataQuery<Object, Object> query = queryFactory.apply(inputQuery, converter);
        assertTrue(query instanceof AsyncDataQueryConverter);
        doTestSimpleSame(query, input, converted);
    }


    @Test
    public void testConvertResults_AsyncDataQuery_DataConverter2() {
        testConvertResults_AsyncDataQuery_DataConverter(AsyncQueries::convertResultsSync);
    }

    private void testConvertResults_AsyncDataQuery_AsyncDataQuery(
            BiFunction<
                AsyncDataQuery<Object, Object>,
                AsyncDataQuery<Object, Object>,
                AsyncDataQuery<Object, Object>
            > queryFactory) {
        Object input = new Object();
        Object output = new Object();
        Object converted = new Object();

        ConstQuery<Object, Object> inputQuery = new ConstQuery<>(input, output);
        ConstQuery<Object, Object> converterQuery = new ConstQuery<>(output, converted);

        AsyncDataQuery<Object, Object> query = queryFactory.apply(inputQuery, converterQuery);
        assertTrue(query instanceof LinkedAsyncDataQuery);
        doTestSimpleSame(query, input, converted);
    }

    /**
     * Test of convertResults method, of class AsyncQueries.
     */
    @Test
    public void testConvertResults_AsyncDataQuery_AsyncDataQuery1() {
        testConvertResults_AsyncDataQuery_AsyncDataQuery(AsyncQueries::convertResultsAsync);
    }

    /**
     * Test of extractCachedResults method, of class AsyncQueries.
     */
    @Test
    public void testExtractCachedResults() {
        Object input = new Object();
        final RefCachedData<Object> output
                = new RefCachedData<>(new Object(), GenericReference.getNoReference());
        ConstQuery<Object, RefCachedData<Object>> wrappedQuery = new ConstQuery<>(input, output);

        AsyncDataQuery<Object, Object> query = AsyncQueries.extractCachedResults(wrappedQuery);
        doTestSimple(query, input, (Object arg) -> {
            assertSame(output.getData(), arg);
        });
    }

    /**
     * Test of removeUidFromResults method, of class AsyncQueries.
     */
    @Test
    public void testRemoveUidFromResults() {
        Object input = new Object();
        final DataWithUid<Object> output = new DataWithUid<>(new Object());
        ConstQuery<Object, DataWithUid<Object>> wrappedQuery = new ConstQuery<>(input, output);

        AsyncDataQuery<Object, Object> query = AsyncQueries.removeUidFromResults(wrappedQuery);
        doTestSimple(query, input, (Object arg) -> {
            assertSame(output.getData(), arg);
        });
    }

    /**
     * Test of markResultsWithUid method, of class AsyncQueries.
     */
    @Test
    public void testMarkResultsWithUid() {
        Object input = new Object();
        final Object output = new Object();
        ConstQuery<Object, Object> wrappedQuery = new ConstQuery<>(input, output);

        AsyncDataQuery<Object, DataWithUid<Object>> query = AsyncQueries.markResultsWithUid(wrappedQuery);
        doTestSimple(query, input, (DataWithUid<Object> arg) -> {
            assertSame(output, arg.getData());
        });
    }

    @Test
    public void testCacheLinksOfCachedResults() {
        AsyncDataQuery<Object, Object> wrappedQuery = mockQuery();
        ManualDataLink<Object> wrappedLink1 = new ManualDataLink<>();
        ManualDataLink<Object> wrappedLink2 = new ManualDataLink<>();

        when(wrappedQuery.createDataLink(any()))
                .thenReturn(wrappedLink1)
                .thenReturn(wrappedLink2);

        CachedAsyncDataQuery<CachedDataRequest<Object>, Object> query
                = AsyncQueries.cacheLinks(AsyncQueries.cacheResults(wrappedQuery));

        Object arg = new Object();
        Object output1 = new Object();
        Object output2 = new Object();

        AsyncDataLink<Object> link1 = query.createDataLink(new CachedLinkRequest<>(new CachedDataRequest<>(arg)));

        AsyncDataListener<Object> listener1 = mockListener();
        link1.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);

        wrappedLink1.onDataArrive(output1);
        wrappedLink1.onDataArrive(output2);
        wrappedLink1.onDoneReceive(AsyncReport.SUCCESS);

        AsyncDataLink<Object> link2 = query.createDataLink(new CachedLinkRequest<>(new CachedDataRequest<>(arg)));

        AsyncDataListener<Object> listener2 = mockListener();
        link2.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        // The following two lines should have no effect if the test is to
        // succeed.
        wrappedLink2.onDataArrive(new Object());
        wrappedLink2.onDoneReceive(AsyncReport.SUCCESS);

        ArgumentCaptor<Object> listener1Args = ArgumentCaptor.forClass(Object.class);
        InOrder inOrder = inOrder(listener1, listener2);
        inOrder.verify(listener1, times(2)).onDataArrive(listener1Args.capture());
        inOrder.verify(listener1).onDoneReceive(any(AsyncReport.class));
        inOrder.verify(listener2).onDataArrive(same(output2));
        inOrder.verify(listener2).onDoneReceive(any(AsyncReport.class));
        inOrder.verifyNoMoreInteractions();

        assertArrayEquals(new Object[]{output1, output2}, listener1Args.getAllValues().toArray());
    }

    private static interface ListenerVerifier<DataType> {
        public void verifyListener(AsyncDataListener<DataType> listener);
    }

    private static interface ArgumentChecker<ArgType> {
        public void verifyArgument(ArgType arg);
    }

    private static class ConstQuery<QueryArgType, DataType> implements AsyncDataQuery<QueryArgType, DataType> {
        private final QueryArgType expectedInput;
        private final DataType result;

        public ConstQuery(QueryArgType expectedInput, DataType result) {
            this.expectedInput = expectedInput;
            this.result = result;
        }

        @Override
        public AsyncDataLink<DataType> createDataLink(Object arg) {
            assertSame(expectedInput, arg);
            return AsyncLinks.createPreparedLink(result, new SimpleDataState("TEST", 0.5));
        }
    }
}
