package org.jtrim2.property;

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
public class NoOpVerifierTest {
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
    public void testNotNull() {
        Object value = new Object();
        assertSame(value, NoOpVerifier.getInstance().storeValue(value));
    }

    @Test
    public void testNull() {
        assertNull(NoOpVerifier.getInstance().storeValue(null));
    }
}
