package org.jtrim.cache;

import org.jtrim.utils.TestUtils;
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
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class GenericReferenceTest {

    public GenericReferenceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(GenericReference.class);
    }

    private void checkRef(ReferenceType refType, Class<?> expectedClass) {
        System.gc();
        Object value = new Object();
        VolatileReference<Object> ref = GenericReference.createReference(value, refType);
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
        VolatileReference<Object> ref = GenericReference.createReference(value, ReferenceType.NoRefType);
        assertNull(ref.get());
        assertTrue(ref instanceof NoVolatileReference);
    }

    @Test
    public void testUserRef() {
        // UserRefType must return NoVolatileReference to be more efficient.
        System.gc();
        Object value = new Object();
        VolatileReference<Object> ref = GenericReference.createReference(value, ReferenceType.UserRefType);
        assertNull(ref.get());
        assertTrue(ref instanceof NoVolatileReference);
    }

    @Test
    public void testNullRef() {
        // For null object createReference must return NoVolatileReference to be
        // more efficient.
        for (ReferenceType refType: ReferenceType.values()) {
            System.gc();
            VolatileReference<Object> ref = GenericReference.createReference(null, refType);
            assertNull(ref.get());
            assertTrue(ref instanceof NoVolatileReference);
        }
    }

    @Test
    public void testWeak2() {
        System.gc();
        Object value = new Object();
        VolatileReference<Object> ref = GenericReference.createWeakReference(value);
        assertSame(value, ref.get());
        assertTrue(ref instanceof WeakVolatileReference);
    }

    @Test
    public void testSoft2() {
        System.gc();
        Object value = new Object();
        VolatileReference<Object> ref = GenericReference.createSoftReference(value);
        assertSame(value, ref.get());
        assertTrue(ref instanceof SoftVolatileReference);
    }

    @Test
    public void testHard2() {
        System.gc();
        Object value = new Object();
        VolatileReference<Object> ref = GenericReference.createHardReference(value);
        assertSame(value, ref.get());
        assertTrue(ref instanceof HardVolatileReference);
    }
}
