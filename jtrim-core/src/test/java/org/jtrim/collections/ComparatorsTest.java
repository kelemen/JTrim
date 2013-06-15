package org.jtrim.collections;

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
public class ComparatorsTest {
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

    /**
     * Test of naturalEquality method, of class Comparators.
     */
    @Test
    public void testNaturalEquality() {
        assertSame(NaturalEquality.INSTANCE, Comparators.naturalEquality());
    }

    /**
     * Test of referenceEquality method, of class Comparators.
     */
    @Test
    public void testReferenceEquality() {
        assertSame(ReferenceEquality.INSTANCE, Comparators.referenceEquality());
    }
}
