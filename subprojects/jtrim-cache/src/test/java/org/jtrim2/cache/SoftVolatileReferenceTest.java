package org.jtrim2.cache;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class SoftVolatileReferenceTest {

    public SoftVolatileReferenceTest() {
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
     * Test of get method, of class SoftVolatileReference.
     */
    @Test
    public void testGet() {
        System.gc();
        VolatileReference<Object> softRef = new SoftVolatileReference<>(new Object());

        System.gc();
        assertNotNull(softRef.get());
    }

    /**
     * Test of clear method, of class SoftVolatileReference.
     */
    @Test
    public void testClear() {
        System.gc();
        VolatileReference<Object> softRef = new SoftVolatileReference<>(new Object());
        assertNotNull(softRef.get());
        softRef.clear();
        assertNull(softRef.get());
    }

    /**
     * Test of toString method, of class SoftVolatileReference.
     */
    @Test
    public void testToString() {
        System.gc();
        assertNotNull(new SoftVolatileReference<>(new Object()).toString());
        assertNotNull(new SoftVolatileReference<>(null).toString());
    }
}
