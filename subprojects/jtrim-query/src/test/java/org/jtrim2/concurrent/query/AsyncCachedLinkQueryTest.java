package org.jtrim2.concurrent.query;

import org.jtrim2.cancel.Cancellation;
import org.junit.Test;

import static org.jtrim2.concurrent.query.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncCachedLinkQueryTest {
    private static <QueryArgType, DataType> AsyncCachedLinkQuery<QueryArgType, DataType> create(
            AsyncDataQuery<? super QueryArgType, ? extends DataType> wrappedQuery) {
        return new AsyncCachedLinkQuery<>(wrappedQuery);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor() {
        create(null);
    }

    @Test
    public void testQueryArg() {
        AsyncDataQuery<Object, Object> wrappedQuery = mockQuery();
        stub(wrappedQuery.createDataLink(any())).toReturn(mockLink());

        AsyncCachedLinkQuery<Object, Object> query = create(wrappedQuery);
        Object queryArg = new Object();

        query.createDataLink(new CachedDataRequest<>(queryArg));

        verify(wrappedQuery).createDataLink(same(queryArg));
        verifyNoMoreInteractions(wrappedQuery);
    }

    @Test
    public void testLinkIndependence() {
        ManualDataQuery<Object, Object> wrappedQuery = new ManualDataQuery<>(null);
        AsyncCachedLinkQuery<Object, Object> query = create(wrappedQuery);

        CachedDataRequest<Object> request = new CachedDataRequest<>(new Object());

        AsyncDataLink<Object> link1 = query.createDataLink(request);
        ManualDataLink<Object> wrappedLink1 = wrappedQuery.getLastDataLink();

        AsyncDataLink<Object> link2 = query.createDataLink(request);
        ManualDataLink<Object> wrappedLink2 = wrappedQuery.getLastDataLink();

        CollectListener<Object> listener1 = new CollectListener<>();
        CollectListener<Object> listener2 = new CollectListener<>();

        link1.getData(Cancellation.UNCANCELABLE_TOKEN, listener1);
        link2.getData(Cancellation.UNCANCELABLE_TOKEN, listener2);

        Object data1 = new Object();
        Object data2 = new Object();

        wrappedLink1.onDataArrive(data1);
        wrappedLink1.onDoneReceive(AsyncReport.SUCCESS);

        wrappedLink2.onDataArrive(data2);
        wrappedLink2.onDoneReceive(AsyncReport.SUCCESS);

        assertSame(AsyncReport.SUCCESS, listener1.getReport());
        assertSame(AsyncReport.SUCCESS, listener2.getReport());

        assertArrayEquals(new Object[]{data1}, listener1.getResults().toArray());
        assertArrayEquals(new Object[]{data2}, listener2.getResults().toArray());

        assertNull(listener1.getMiscError());
        assertNull(listener2.getMiscError());
    }

    /**
     * Test of toString method, of class AsyncCachedLinkQuery.
     */
    @Test
    public void testToString() {
        assertNotNull(create(mockQuery()).toString());
    }

    private static class ManualDataQuery<QueryArgType, DataType>
    implements
            AsyncDataQuery<QueryArgType, DataType> {

        private final AsyncDataState initialState;
        private volatile QueryArgType lastArgument;
        private volatile ManualDataLink<DataType> lastDataLink;

        public ManualDataQuery(AsyncDataState initialState) {
            this.initialState = initialState;
        }

        public ManualDataLink<DataType> getLastDataLink() {
            return lastDataLink;
        }

        public QueryArgType getLastArgument() {
            return lastArgument;
        }

        @Override
        public AsyncDataLink<DataType> createDataLink(QueryArgType arg) {
            ManualDataLink<DataType> result = new ManualDataLink<>(initialState);
            lastDataLink = result;
            lastArgument = arg;
            return result;
        }
    }
}
