package org.jtrim2.cache;

import org.junit.Test;

import static org.junit.Assert.*;

public class JavaRefObjectCacheTest {
    private void checkRef(ReferenceType refType, Class<?> expectedClass) {
        System.gc();
        Object value = new Object();
        VolatileReference<Object> ref = ObjectCache.javaRefCache().getReference(value, refType);
        assertSame(value, ref.get());
        assertTrue(expectedClass == ref.getClass());
    }

    @Test
    public void testWeak() {
        checkRef(ReferenceType.WeakRefType, WeakVolatileReference.class);
    }

    @Test
    public void testSoft() {
        checkRef(ReferenceType.SoftRefType, SoftVolatileReference.class);
    }

    @Test
    public void testHard() {
        checkRef(ReferenceType.HardRefType, HardVolatileReference.class);
    }

    @Test
    public void testNoRef() {
        System.gc();
        Object value = new Object();
        VolatileReference<Object> ref = ObjectCache.javaRefCache().getReference(value, ReferenceType.NoRefType);
        assertNull(ref.get());
        assertTrue(ref instanceof NoVolatileReference);
    }

    @Test
    public void testUserRef() {
        // UserRefType must return NoVolatileReference to be more efficient.
        System.gc();
        Object value = new Object();
        VolatileReference<Object> ref = ObjectCache.javaRefCache().getReference(value, ReferenceType.UserRefType);
        assertNull(ref.get());
        assertTrue(ref instanceof NoVolatileReference);
    }

    @Test
    public void testNullRef() {
        // For null object createReference must return NoVolatileReference to be
        // more efficient.
        for (ReferenceType refType: ReferenceType.values()) {
            System.gc();
            VolatileReference<Object> ref = ObjectCache.javaRefCache().getReference(null, refType);
            assertNull(ref.get());
            assertTrue(ref instanceof NoVolatileReference);
        }
    }
}
