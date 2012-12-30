package org.jtrim.concurrent.async;

import java.util.concurrent.TimeUnit;
import org.jtrim.cache.GenericReference;
import org.jtrim.cache.ReferenceType;
import org.jtrim.cancel.Cancellation;
import org.jtrim.concurrent.WaitableSignal;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.jtrim.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class AsyncQueriesTest {

    public AsyncQueriesTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static <QueryArgType, DataType> void doTestSimpleSame(
            AsyncDataQuery<QueryArgType, DataType> query,
            QueryArgType input,
            final DataType expectedData) {

        doTestSimple(query, input, new ListenerVerifier<DataType>() {
            @Override
            public void verifyListener(AsyncDataListener<DataType> listener) {
                verify(listener).onDataArrive(same(expectedData));
            }
        });
    }

    private static <QueryArgType, DataType> void doTestSimple(
            AsyncDataQuery<QueryArgType, DataType> query,
            QueryArgType input,
            final ArgumentChecker<DataType> verifier) {
        doTestSimple(query, input, new ListenerVerifier<DataType>() {
            @Override
            public void verifyListener(AsyncDataListener<DataType> listener) {
                @SuppressWarnings("unchecked")
                ArgumentCaptor<DataType> arg = (ArgumentCaptor<DataType>)ArgumentCaptor.forClass(Object.class);

                verify(listener).onDataArrive(arg.capture());
                verifier.verifyArgument(arg.getValue());
            }
        });
    }

    private static <QueryArgType, DataType> void doTestSimple(
            AsyncDataQuery<QueryArgType, DataType> query,
            QueryArgType input,
            ListenerVerifier<DataType> verifier) {

        AsyncDataLink<DataType> link = query.createDataLink(input);

        AsyncDataListener<DataType> listener = mockListener();

        final WaitableSignal endSignal = new WaitableSignal();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                endSignal.signal();
                return null;
            }
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
        CachedLinkRequest<DataWithUid<Object>> input = new CachedLinkRequest<>(new DataWithUid<>(new Object(), new Object()));
        final DataWithUid<Object> output = new DataWithUid<>(new Object(), new Object());

        ConstQuery<Object, Object> wrappedQuery = new ConstQuery<>(
                input.getQueryArg().getData(),
                output.getData());

        CachedByIDAsyncDataQuery<Object, Object> query = AsyncQueries.cacheByID(
                wrappedQuery, ReferenceType.HardRefType, null);

        doTestSimple(query, input, new ArgumentChecker<DataWithUid<Object>>() {
            @Override
            public void verifyArgument(DataWithUid<Object> arg) {
                assertSame(output.getData(), arg.getData());
            }
        });
    }

    /**
     * Test of cacheByID method, of class AsyncQueries.
     */
    @Test
    public void testCacheByID_4args() {
        CachedLinkRequest<DataWithUid<Object>> input = new CachedLinkRequest<>(new DataWithUid<>(new Object(), new Object()));
        final DataWithUid<Object> output = new DataWithUid<>(new Object(), new Object());

        ConstQuery<Object, Object> wrappedQuery = new ConstQuery<>(
                input.getQueryArg().getData(),
                output.getData());

        CachedByIDAsyncDataQuery<Object, Object> query = AsyncQueries.cacheByID(
                wrappedQuery, ReferenceType.HardRefType, null, 1024);

        doTestSimple(query, input, new ArgumentChecker<DataWithUid<Object>>() {
            @Override
            public void verifyArgument(DataWithUid<Object> arg) {
                assertSame(output.getData(), arg.getData());
            }
        });
    }

    /**
     * Test of convertResults method, of class AsyncQueries.
     */
    @Test
    public void testConvertResults_AsyncDataQuery_DataConverter() {
        Object input = new Object();
        Object output = new Object();
        Object converted = new Object();

        ConstQuery<Object, Object> inputQuery = new ConstQuery<>(input, output);
        DataConverter<Object, Object> converter = mockConverter();

        stub(converter.convertData(same(output))).toReturn(converted);

        AsyncDataQuery<Object, Object> query = AsyncQueries.convertResults(inputQuery, converter);
        assertTrue(query instanceof AsyncDataQueryConverter);
        doTestSimpleSame(query, input, converted);
    }

    /**
     * Test of convertResults method, of class AsyncQueries.
     */
    @Test
    public void testConvertResults_AsyncDataQuery_AsyncDataQuery() {
        Object input = new Object();
        Object output = new Object();
        Object converted = new Object();

        ConstQuery<Object, Object> inputQuery = new ConstQuery<>(input, output);
        ConstQuery<Object, Object> converterQuery = new ConstQuery<>(output, converted);

        AsyncDataQuery<Object, Object> query = AsyncQueries.convertResults(inputQuery, converterQuery);
        assertTrue(query instanceof LinkedAsyncDataQuery);
        doTestSimpleSame(query, input, converted);
    }

    /**
     * Test of extractCachedResults method, of class AsyncQueries.
     */
    @Test
    public void testExtractCachedResults() {
        Object input = new Object();
        final RefCachedData<Object> output = new RefCachedData<>(new Object(), GenericReference.getNoReference());
        ConstQuery<Object, RefCachedData<Object>> wrappedQuery = new ConstQuery<>(input, output);

        AsyncDataQuery<Object, Object> query = AsyncQueries.extractCachedResults(wrappedQuery);
        doTestSimple(query, input, new ArgumentChecker<Object>() {
            @Override
            public void verifyArgument(Object arg) {
                assertSame(output.getData(), arg);
            }
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
        doTestSimple(query, input, new ArgumentChecker<Object>() {
            @Override
            public void verifyArgument(Object arg) {
                assertSame(output.getData(), arg);
            }
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
        doTestSimple(query, input, new ArgumentChecker<DataWithUid<Object>>() {
            @Override
            public void verifyArgument(DataWithUid<Object> arg) {
                assertSame(output, arg.getData());
            }
        });
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