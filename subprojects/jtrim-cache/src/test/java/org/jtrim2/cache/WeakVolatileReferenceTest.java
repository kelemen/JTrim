package org.jtrim2.cache;

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
public class WeakVolatileReferenceTest {

    public WeakVolatileReferenceTest() {
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

    /**
     * Test of get method, of class WeakVolatileReference.
     */
    @Test
    public void testGet() {
        VolatileReference<Object> weakRef = new WeakVolatileReference<>(new Object());

        System.gc();
        assertNull(weakRef.get());
    }

    /**
     * Test of clear method, of class WeakVolatileReference.
     */
    @Test
    public void testClear() {
        System.gc();
        VolatileReference<Object> weakRef = new WeakVolatileReference<>(new Object());
        assertNotNull(weakRef.get());
        weakRef.clear();
        assertNull(weakRef.get());
    }

    /**
     * Test of toString method, of class WeakVolatileReference.
     */
    @Test
    public void testToString() {
        System.gc();
        assertNotNull(new WeakVolatileReference<>(new Object()).toString());
        assertNotNull(new WeakVolatileReference<>(null).toString());
    }
}
