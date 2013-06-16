package org.jtrim.collections;

import java.util.Objects;
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
public class NaturalEqualityTest {
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
        assertEquals(1, NaturalEquality.values().length);
        assertSame(
                NaturalEquality.INSTANCE,
                NaturalEquality.valueOf(NaturalEquality.INSTANCE.name()));
    }

    /**
     * Test of equals method, of class NaturalEquality.
     */
    @Test
    public void testEquals() {
        TestObject obj1 = new TestObject("OBJ1");

        assertFalse(NaturalEquality.INSTANCE.equals(null, obj1));
        assertFalse(NaturalEquality.INSTANCE.equals(obj1, null));
        assertFalse(NaturalEquality.INSTANCE.equals(obj1, new TestObject("OBJ2")));

        assertTrue(NaturalEquality.INSTANCE.equals(null, null));
        assertTrue(NaturalEquality.INSTANCE.equals(obj1, obj1));
        assertTrue(NaturalEquality.INSTANCE.equals(obj1, new TestObject("OBJ1")));
    }
}