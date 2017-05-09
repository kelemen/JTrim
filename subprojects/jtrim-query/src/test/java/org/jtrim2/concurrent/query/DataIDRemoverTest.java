package org.jtrim2.concurrent.query;

import org.junit.Test;

import static org.junit.Assert.*;

public class DataIDRemoverTest {
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
