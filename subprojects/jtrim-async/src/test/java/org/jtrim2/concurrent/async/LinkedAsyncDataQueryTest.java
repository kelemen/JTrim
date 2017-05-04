package org.jtrim2.concurrent.async;

import org.jtrim2.cancel.Cancellation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;

import static org.jtrim2.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class LinkedAsyncDataQueryTest {
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

        stub(input.createDataLink(any())).toReturn(AsyncLinks.createPreparedLink(inputData, null));
        stub(converter.createDataLink(any())).toReturn(AsyncLinks.createPreparedLink(outputData, null));

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