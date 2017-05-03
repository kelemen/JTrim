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
public class NoVolatileReferenceTest {

    public NoVolatileReferenceTest() {
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
     * Test of getInstance method, of class NoVolatileReference.
     */
    @Test
    public void testGetInstance() {
        assertSame(NoVolatileReference.getInstance(), NoVolatileReference.getInstance());
    }

    /**
     * Test of get method, of class NoVolatileReference.
     */
    @Test
    public void testGet() {
        assertNull(NoVolatileReference.getInstance().get());
    }

    /**
     * Test of clear method, of class NoVolatileReference.
     */
    @Test
    public void testClear() {
        NoVolatileReference<Object> ref = NoVolatileReference.getInstance();
        ref.clear();
        assertNull(ref.get());
    }

    /**
     * Test of toString method, of class NoVolatileReference.
     */
    @Test
    public void testToString() {
        assertNotNull(NoVolatileReference.getInstance().toString());
    }
}
