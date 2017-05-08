package org.jtrim2.concurrent.async;

import org.junit.Test;

import static org.junit.Assert.*;

public class DoNothingDataControllerTest {
    @Test
    public void testAutoGenerated() {
        assertEquals(1, DoNothingDataController.values().length);
        assertSame(DoNothingDataController.INSTANCE, DoNothingDataController.valueOf("INSTANCE"));
    }

    /**
     * Test of controlData method, of class DoNothingDataController.
     */
    @Test
    public void testControlData() {
        DoNothingDataController.INSTANCE.controlData(null);
        DoNothingDataController.INSTANCE.controlData(new Object());
    }

    /**
     * Test of getDataState method, of class DoNothingDataController.
     */
    @Test
    public void testGetDataState() {
        assertNull(DoNothingDataController.INSTANCE.getDataState());
    }

    /**
     * Test of toString method, of class DoNothingDataController.
     */
    @Test
    public void testToString() {
        assertNotNull(DoNothingDataController.INSTANCE.toString());
    }
}
