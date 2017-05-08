package org.jtrim2.property;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class NotNullVerifierTest {
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
        assertSame(value, NotNullVerifier.getInstance().storeValue(value));
    }

    @Test(expected = NullPointerException.class)
    public void testNull() {
        NotNullVerifier.getInstance().storeValue(null);
    }
}
