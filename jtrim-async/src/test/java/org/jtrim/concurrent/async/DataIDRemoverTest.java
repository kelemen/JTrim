package org.jtrim.concurrent.async;

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
public class DataIDRemoverTest {

    public DataIDRemoverTest() {
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
     * Test of convertData method, of class DataIDRemover.
     */
    @Test
    public void testConvertData() {
        DataIDRemover<Object> remover = new DataIDRemover<>();

        Object data = new Object();
        assertSame(data, remover.convertData(new DataWithUid<>(data, new Object())));
    }

    /**
     * Test of toString method, of class DataIDRemover.
     */
    @Test
    public void testToString() {
        assertNotNull(new DataIDRemover<>().toString());
    }
}