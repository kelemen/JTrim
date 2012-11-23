package org.jtrim.cache;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class JavaRefObjectCacheTest {

    public JavaRefObjectCacheTest() {
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

    @Test
    public void testAutoGenerated() {
        assertEquals(1, JavaRefObjectCache.values().length);
        assertSame(JavaRefObjectCache.INSTANCE, JavaRefObjectCache.valueOf(JavaRefObjectCache.INSTANCE.toString()));
    }

    private void checkRef(ReferenceType refType, Class<?> expectedClass) {
        System.gc();
        Object value = new Object();
        VolatileReference<Object> ref = JavaRefObjectCache.INSTANCE.getReference(value, refType);
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
        VolatileReference<Object> ref = JavaRefObjectCache.INSTANCE.getReference(value, ReferenceType.NoRefType);
        assertNull(ref.get());
        assertTrue(ref instanceof NoVolatileReference);
    }

    @Test
    public void testUserRef() {
        // UserRefType must return NoVolatileReference to be more efficient.
        System.gc();
        Object value = new Object();
        VolatileReference<Object> ref = JavaRefObjectCache.INSTANCE.getReference(value, ReferenceType.UserRefType);
        assertNull(ref.get());
        assertTrue(ref instanceof NoVolatileReference);
    }

    @Test
    public void testNullRef() {
        // For null object createReference must return NoVolatileReference to be
        // more efficient.
        for (ReferenceType refType: ReferenceType.values()) {
            System.gc();
            VolatileReference<Object> ref = JavaRefObjectCache.INSTANCE.getReference(null, refType);
            assertNull(ref.get());
            assertTrue(ref instanceof NoVolatileReference);
        }
    }
}