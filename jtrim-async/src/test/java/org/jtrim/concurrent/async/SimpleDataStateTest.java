package org.jtrim.concurrent.async;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class SimpleDataStateTest {

    public SimpleDataStateTest() {
    }

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
    public void testProperties() {
        String stateStr = "SimpleDataStateTest - STATE";
        double progress = 6547.43654;

        SimpleDataState state = new SimpleDataState(stateStr, progress);

        assertEquals(progress, state.getProgress(), 0.0);
        assertEquals(stateStr, state.getState());
        assertNotNull(state.toString());
    }
}