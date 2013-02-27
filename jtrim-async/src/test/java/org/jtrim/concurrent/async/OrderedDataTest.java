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
public class OrderedDataTest {
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
    public void testSimple() {
        long index = 436544354656775L;
        Object data = new Object();

        OrderedData<Object> orderedData = new OrderedData<>(index, data);
        assertSame(data, orderedData.getRawData());
        assertEquals(index, orderedData.getIndex());
        assertNotNull(orderedData.toString());
    }

    @Test
    public void testNullData() {
        long index = 436544354656775L;

        OrderedData<Object> orderedData = new OrderedData<>(index, null);
        assertNull(orderedData.getRawData());
        assertEquals(index, orderedData.getIndex());
        assertNotNull(orderedData.toString());
    }
}
