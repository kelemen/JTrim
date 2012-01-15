/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jtrim.cache;

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