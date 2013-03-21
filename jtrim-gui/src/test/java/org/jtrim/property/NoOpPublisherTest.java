package org.jtrim.property;

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
public class NoOpPublisherTest {
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
        assertSame(value, NoOpPublisher.getInstance().returnValue(value));
    }

    @Test
    public void testNull() {
        assertNull(NoOpPublisher.getInstance().returnValue(null));
    }
}
