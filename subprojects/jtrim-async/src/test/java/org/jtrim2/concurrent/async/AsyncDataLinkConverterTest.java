package org.jtrim2.concurrent.async;

import org.jtrim2.cancel.Cancellation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jtrim2.concurrent.async.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class AsyncDataLinkConverterTest {
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

    private static <OldDataType, NewDataType> AsyncDataLinkConverter<OldDataType, NewDataType> create(
            AsyncDataLink<? extends OldDataType> wrappedDataLink,
            DataConverter<? super OldDataType, ? extends NewDataType> converter) {
        return new AsyncDataLinkConverter<>(wrappedDataLink, converter);
    }

    @SuppressWarnings("unchecked")
    private static <OldDataType, NewDataType> DataConverter<OldDataType, NewDataType> mockConverter() {
        return mock(DataConverter.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSimpleConversion() {
        Object input = new Object();
        Object output = new Object();

        DataConverter<Object, Object> converter = mockConverter();

        AsyncDataController wrappedController = mock(AsyncDataController.class);
        AsyncDataState wrappedState = mock(AsyncDataState.class);
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>(wrappedState);

        stub(wrappedController.getDataState()).toReturn(wrappedState);
        stub(converter.convertData(any())).toReturn(output);

        AsyncDataListener<Object> listener = mockListener();

        AsyncDataController controller = create(wrappedLink, converter)
                .getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        verifyZeroInteractions(wrappedController);

        Object[] controlArgs = new Object[]{new Object(), new Object()};
        controller.controlData(controlArgs[0]);
        controller.controlData(controlArgs[1]);
        assertSame(wrappedState, controller.getDataState());

        assertArrayEquals(controlArgs, wrappedLink.getReceivedControlArgs().toArray());

        AsyncReport report = AsyncReport.getReport(new Exception(), false);

        wrappedLink.onDataArrive(input);
        verify(listener).onDataArrive(same(output));
        verifyNoMoreInteractions(listener);

        wrappedLink.onDoneReceive(report);
        verify(listener).onDoneReceive(same(report));
        verifyNoMoreInteractions(listener);
    }

    /**
     * Test of toString method, of class AsyncDataLinkConverter.
     */
    @Test
    public void testToString() {
        assertNotNull(create(mockLink(), mockConverter()).toString());
    }
}