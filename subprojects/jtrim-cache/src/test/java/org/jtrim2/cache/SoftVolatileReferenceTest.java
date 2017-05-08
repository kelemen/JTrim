package org.jtrim2.cache;

import org.junit.Test;

import static org.junit.Assert.*;

public class SoftVolatileReferenceTest {
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
