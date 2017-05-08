package org.jtrim2.concurrent.async;

import org.jtrim2.cancel.Cancellation;
import org.junit.Test;
import org.mockito.InOrder;

import static org.jtrim2.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncDataQueryConverterTest {
    private static <NewDataType, QueryArgType, OldDataType>
            AsyncDataQueryConverter<NewDataType, QueryArgType, OldDataType> create(
            AsyncDataQuery<? super QueryArgType, ? extends OldDataType> wrappedQuery,
            DataConverter<? super OldDataType, ? extends NewDataType> converter) {
        return new AsyncDataQueryConverter<>(wrappedQuery, converter);
    }

    @SuppressWarnings("unchecked")
    private static <OldDataType, NewDataType> DataConverter<OldDataType, NewDataType> mockConverter() {
        return mock(DataConverter.class);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor1() {
        create(null, mockConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(mockQuery(), null);
    }

    /**
     * Test of createDataLink method, of class AsyncDataQueryConverter.
     */
    @Test
    public void testCreateDataLink() {
        Object input = new Object();
        Object output = new Object();
        Object converted = new Object();

        ConstQuery<Object, Object> wrappedQuery = new ConstQuery<>(input, output);
        DataConverter<Object, Object> converter = mockConverter();

        stub(converter.convertData(same(output))).toReturn(converted);

        AsyncDataLink<Object> link = create(wrappedQuery, converter).createDataLink(input);
        assertTrue(link instanceof AsyncDataLinkConverter);

        AsyncDataListener<Object> listener = mockListener();
        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        InOrder inOrder = inOrder(listener);
        inOrder.verify(listener).onDataArrive(same(converted));
        inOrder.verify(listener).onDoneReceive(any(AsyncReport.class));
        inOrder.verifyNoMoreInteractions();
    }

    /**
     * Test of toString method, of class AsyncDataQueryConverter.
     */
    @Test
    public void testToString() {
        assertNotNull(create(mockQuery(), mockConverter()).toString());
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
