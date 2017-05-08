package org.jtrim2.concurrent.async;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;

import static org.jtrim2.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncDataListenerConverterTest {
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

    private static <OldDataType, NewDataType> AsyncDataListenerConverter<OldDataType, NewDataType> create(
            AsyncDataListener<? super NewDataType> wrappedListener,
            DataConverter<? super OldDataType, ? extends NewDataType> converter) {
        return new AsyncDataListenerConverter<>(wrappedListener, converter);
    }

    @SuppressWarnings("unchecked")
    private static <OldDataType, NewDataType> DataConverter<OldDataType, NewDataType> mockConverter() {
        return mock(DataConverter.class);
    }

    @Test
    public void testForwardZero() {
        AsyncDataListener<Object> wrappedListener = mockListener();
        DataConverter<Object, Object> wrappedConverter = mockConverter();

        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        AsyncDataListenerConverter<Object, Object> listener = create(wrappedListener, wrappedConverter);

        listener.onDoneReceive(report);

        verify(wrappedListener).onDoneReceive(same(report));
        verifyNoMoreInteractions(wrappedListener);
    }

    @Test
    public void testForwardSingle() {
        AsyncDataListener<Object> wrappedListener = mockListener();
        DataConverter<Object, Object> wrappedConverter = mockConverter();

        Object input = new Object();
        Object output = new Object();
        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        stub(wrappedConverter.convertData(same(input))).toReturn(output);

        AsyncDataListenerConverter<Object, Object> listener = create(wrappedListener, wrappedConverter);

        listener.onDataArrive(input);
        listener.onDoneReceive(report);

        InOrder inOrder = inOrder(wrappedListener);
        inOrder.verify(wrappedListener).onDataArrive(same(output));
        inOrder.verify(wrappedListener).onDoneReceive(same(report));
        inOrder.verifyNoMoreInteractions();
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor1() {
        create(null, mockConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(mockListener(), null);
    }

    /**
     * Test of toString method, of class AsyncDataListenerConverter.
     */
    @Test
    public void testToString() {
        assertNotNull(create(mockListener(), mockConverter()).toString());
    }
}
