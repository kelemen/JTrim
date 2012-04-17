package org.jtrim.cache;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.*;

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

    /**
     * Test of get method, of class GenericReference.
     */
    @Test
    public void testGet() {
        VolatileReference<Object> weakRef = GenericReference.createReference(
                new Object(), ReferenceType.WeakRefType);

        System.gc();
        assertNull(weakRef.get());

        VolatileReference<Object> hardRef = GenericReference.createReference(
                new Object(), ReferenceType.HardRefType);

        System.gc();
        assertNotNull(hardRef.get());

        VolatileReference<Object> softRef = GenericReference.createReference(
                hardRef.get(), ReferenceType.SoftRefType);

        System.gc();
        assertNotNull(softRef.get());

        VolatileReference<Object> noRef = GenericReference.createReference(
                new Object(), ReferenceType.NoRefType);
        assertNull(noRef.get());
    }
}