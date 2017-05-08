package org.jtrim2.collections;

import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class EqualityTest {
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
