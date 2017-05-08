package org.jtrim2.cache;

import org.junit.Test;

import static org.junit.Assert.*;

public class WeakVolatileReferenceTest {
    /**
     * Test of get method, of class WeakVolatileReference.
     */
    @Test
    public void testGet() {
        VolatileReference<Object> weakRef = new WeakVolatileReference<>(new Object());

        System.gc();
        assertNull(weakRef.get());
    }

    /**
     * Test of clear method, of class WeakVolatileReference.
     */
    @Test
    public void testClear() {
        System.gc();
        VolatileReference<Object> weakRef = new WeakVolatileReference<>(new Object());
        assertNotNull(weakRef.get());
        weakRef.clear();
        assertNull(weakRef.get());
    }

    /**
     * Test of toString method, of class WeakVolatileReference.
     */
    @Test
    public void testToString() {
        System.gc();
        assertNotNull(new WeakVolatileReference<>(new Object()).toString());
        assertNotNull(new WeakVolatileReference<>(null).toString());
    }
}
