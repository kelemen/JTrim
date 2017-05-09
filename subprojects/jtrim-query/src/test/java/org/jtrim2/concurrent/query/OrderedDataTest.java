package org.jtrim2.concurrent.query;

import org.junit.Test;

import static org.junit.Assert.*;

public class OrderedDataTest {
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
