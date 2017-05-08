package org.jtrim2.concurrent.async;

import org.junit.Test;

import static org.junit.Assert.*;

public class LinkedDataControlTest {
    private static LinkedDataControl create(Object mainControlData, Object secondaryControlData) {
        return new LinkedDataControl(mainControlData, secondaryControlData);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor1() {
        create(null, new Object());
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor2() {
        create(new Object(), null);
    }

    /**
     * Test of getMainControlData method, of class LinkedDataControl.
     */
    @Test
    public void testGetMainControlData() {
        Object mainArg = new Object();
        LinkedDataControl arg = new LinkedDataControl(mainArg, new Object());
        assertSame(mainArg, arg.getMainControlData());
    }

    /**
     * Test of getSecondaryControlData method, of class LinkedDataControl.
     */
    @Test
    public void testGetSecondaryControlData() {
        Object secArg = new Object();
        LinkedDataControl arg = new LinkedDataControl(new Object(), secArg);
        assertSame(secArg, arg.getSecondaryControlData());
    }

    /**
     * Test of toString method, of class LinkedDataControl.
     */
    @Test
    public void testToString() {
        LinkedDataControl arg = new LinkedDataControl(new Object(), new Object());
        assertNotNull(arg.toString());
    }
}
