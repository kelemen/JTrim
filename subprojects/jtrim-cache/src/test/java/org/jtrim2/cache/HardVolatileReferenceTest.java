package org.jtrim2.cache;

import org.junit.Test;

import static org.junit.Assert.*;

public class HardVolatileReferenceTest {
    /**
     * Test of get method, of class HardVolatileReference.
     */
    @Test
    public void testGet() {
        System.gc();

        VolatileReference<Object> hardRef = new HardVolatileReference<>(new Object());

        System.gc();
        assertNotNull(hardRef.get());
    }

    @Test
    public void testGet2() {
        System.gc();

        Object value = new Object();
        VolatileReference<Object> hardRef = new HardVolatileReference<>(value);

        System.gc();
        assertSame(value, hardRef.get());
    }

    /**
     * Test of clear method, of class HardVolatileReference.
     */
    @Test
    public void testClear() {
        System.gc();
        VolatileReference<Object> hardRef = new HardVolatileReference<>(new Object());
        assertNotNull(hardRef.get());
        hardRef.clear();
        assertNull(hardRef.get());
    }

    /**
     * Test of toString method, of class HardVolatileReference.
     */
    @Test
    public void testToString() {
        System.gc();
        assertNotNull(new HardVolatileReference<>(new Object()).toString());
        assertNotNull(new HardVolatileReference<>(null).toString());
    }
}
