package org.jtrim2.concurrent.async;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class SimpleDataControllerTest {
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

    @Test
    public void testInitialNull() {
        SimpleDataController controller = new SimpleDataController();
        assertNull(controller.getDataState());
    }

    @Test
    public void testInitialNonNull() {
        AsyncDataState state = mock(AsyncDataState.class);
        SimpleDataController controller = new SimpleDataController(state);
        assertSame(state, controller.getDataState());
    }

    @Test
    public void testSetAndGetDataState() {
        SimpleDataController controller = new SimpleDataController();

        AsyncDataState newState = mock(AsyncDataState.class);
        controller.setDataState(newState);
        assertSame(newState, controller.getDataState());
    }

    /**
     * Test of controlData method, of class SimpleDataController.
     */
    @Test
    public void testControlData() {
        SimpleDataController controller = new SimpleDataController();
        controller.controlData(new Object());
        assertNull(controller.getDataState());
    }
}
