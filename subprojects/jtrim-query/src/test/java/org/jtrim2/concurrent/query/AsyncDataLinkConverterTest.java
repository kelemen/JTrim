package org.jtrim2.concurrent.query;

import org.jtrim2.cancel.Cancellation;
import org.junit.Test;

import static org.jtrim2.concurrent.query.AsyncMocks.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncDataLinkConverterTest {
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
    public void testSimpleConversion() {
        Object input = new Object();
        Object output = new Object();

        DataConverter<Object, Object> converter = mockConverter();

        AsyncDataController wrappedController = mock(AsyncDataController.class);
        AsyncDataState wrappedState = mock(AsyncDataState.class);
        ManualDataLink<Object> wrappedLink = new ManualDataLink<>(wrappedState);

        when(wrappedController.getDataState()).thenReturn(wrappedState);
        when(converter.convertData(any())).thenReturn(output);

        AsyncDataListener<Object> listener = mockListener();

        AsyncDataController controller = create(wrappedLink, converter)
                .getData(Cancellation.UNCANCELABLE_TOKEN, listener);

        verifyNoInteractions(wrappedController);

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
