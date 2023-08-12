package org.jtrim2.concurrent.query;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DelegatedAsyncDataControllerTest {
    /**
     * Test of controlData method, of class DelegatedAsyncDataController.
     */
    @Test
    public void testControlData() {
        AsyncDataController wrapped = mock(AsyncDataController.class);
        DelegatedAsyncDataController controller = new DelegatedAsyncDataController(wrapped);

        Object data = new Object();
        controller.controlData(data);

        verify(wrapped).controlData(same(data));
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of getDataState method, of class DelegatedAsyncDataController.
     */
    @Test
    public void testGetDataState() {
        AsyncDataController wrapped = mock(AsyncDataController.class);
        DelegatedAsyncDataController controller = new DelegatedAsyncDataController(wrapped);
        AsyncDataState state = mock(AsyncDataState.class);

        when(wrapped.getDataState()).thenReturn(state);

        assertSame(state, controller.getDataState());

        verify(wrapped).getDataState();
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of toString method, of class DelegatedAsyncDataController.
     */
    @Test
    public void testToString() {
        AsyncDataController wrapped = mock(AsyncDataController.class);
        DelegatedAsyncDataController controller = new DelegatedAsyncDataController(wrapped);

        String str = "TEST - DelegatedAsyncDataController.toString()";
        when(wrapped.toString()).thenReturn(str);

        assertSame(str, controller.toString());

        verifyNoInteractions(wrapped);
    }
}
