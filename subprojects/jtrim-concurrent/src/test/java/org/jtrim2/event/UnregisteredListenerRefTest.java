package org.jtrim2.event;

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
public class UnregisteredListenerRefTest {

    public UnregisteredListenerRefTest() {
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
    public void testAutoGenerated() {
        assertEquals(1, UnregisteredListenerRef.values().length);
        assertSame(
                UnregisteredListenerRef.INSTANCE,
                UnregisteredListenerRef.valueOf(UnregisteredListenerRef.INSTANCE.toString()));
    }

    /**
     * Test of isRegistered method, of class UnregisteredListenerRef.
     */
    @Test
    public void testIsRegistered() {
        assertFalse(UnregisteredListenerRef.INSTANCE.isRegistered());
    }

    /**
     * Test of unregister method, of class UnregisteredListenerRef.
     */
    @Test
    public void testUnregister() {
        UnregisteredListenerRef.INSTANCE.unregister();
    }
}