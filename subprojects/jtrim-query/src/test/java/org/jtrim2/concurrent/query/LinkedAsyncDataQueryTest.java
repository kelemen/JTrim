package org.jtrim2.concurrent.query;

import org.jtrim2.cancel.Cancellation;
import org.junit.Test;
import org.mockito.InOrder;

import static org.jtrim2.concurrent.query.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LinkedAsyncDataQueryTest {
    private static <QueryArgType, SecArgType, DataType> LinkedAsyncDataQuery<QueryArgType, DataType> create(
            AsyncDataQuery<? super QueryArgType, ? extends SecArgType> input,
            AsyncDataQuery<? super SecArgType, ? extends DataType> converter) {
        return new LinkedAsyncDataQuery<>(input, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor1() {
        create(null, mockQuery());
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(mockQuery(), null);
    }

    @Test
    public void testSimpleTest() {
        AsyncDataQuery<Object, Object> input = mockQuery();
        AsyncDataQuery<Object, Object> converter = mockQuery();

        Object inputData = new Object();
        Object outputData = new Object();

        when(input.createDataLink(any())).thenReturn(AsyncLinks.createPreparedLink(inputData, null));
        when(converter.createDataLink(any())).thenReturn(AsyncLinks.createPreparedLink(outputData, null));

        LinkedAsyncDataQuery<Object, Object> query = create(input, converter);
        Object queryInput = new Object();

        AsyncDataLink<Object> link = query.createDataLink(queryInput);
        assertTrue(link instanceof LinkedAsyncDataLink);

        AsyncDataListener<Object> listener = mockListener();
        link.getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        InOrder inOrder = inOrder(input, converter, listener);
        inOrder.verify(input).createDataLink(same(queryInput));
        inOrder.verify(converter).createDataLink(same(inputData));
        inOrder.verify(listener).onDataArrive(same(outputData));
        inOrder.verify(listener).onDoneReceive(any(AsyncReport.class));
    }

    /**
     * Test of toString method, of class LinkedAsyncDataQuery.
     */
    @Test
    public void testToString() {
        LinkedAsyncDataQuery<Object, Object> query = create(mockQuery(), mockQuery());
        assertNotNull(query.toString());
    }
}
