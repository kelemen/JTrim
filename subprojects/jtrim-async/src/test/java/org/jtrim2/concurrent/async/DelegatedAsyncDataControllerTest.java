package org.jtrim2.concurrent.async;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DelegatedAsyncDataControllerTest {
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

        stub(wrapped.getDataState()).toReturn(state);

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
        stub(wrapped.toString()).toReturn(str);

        assertSame(str, controller.toString());

        verifyZeroInteractions(wrapped);
    }
}
