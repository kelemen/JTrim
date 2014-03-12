package org.jtrim.collections;

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
public class EqualityTest {
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
    public void testUtilityClass() {
        TestUtils.testUtilityClass(Equality.class);
    }

    /**
     * Test of naturalEquality method, of class Equality.
     */
    @Test
    public void testNaturalEquality() {
        assertSame(NaturalEquality.INSTANCE, Equality.naturalEquality());
    }

    /**
     * Test of referenceEquality method, of class Equality.
     */
    @Test
    public void testReferenceEquality() {
        assertSame(ReferenceEquality.INSTANCE, Equality.referenceEquality());
    }
}
