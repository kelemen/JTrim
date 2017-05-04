package org.jtrim2.collections;

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
public class ReferenceEqualityTest {
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
        assertEquals(1, ReferenceEquality.values().length);
        assertSame(
                ReferenceEquality.INSTANCE,
                ReferenceEquality.valueOf(ReferenceEquality.INSTANCE.name()));
    }

    /**
     * Test of equals method, of class ReferenceEquality.
     */
    @Test
    public void testEquals() {
        Object obj1 = new TestObject("OBJ1");

        assertFalse(ReferenceEquality.INSTANCE.equals(null, obj1));
        assertFalse(ReferenceEquality.INSTANCE.equals(obj1, null));
        assertFalse(ReferenceEquality.INSTANCE.equals(obj1, new Object()));
        assertFalse(ReferenceEquality.INSTANCE.equals(obj1, new TestObject("OBJ1")));

        assertTrue(ReferenceEquality.INSTANCE.equals(null, null));
        assertTrue(ReferenceEquality.INSTANCE.equals(obj1, obj1));
    }
}