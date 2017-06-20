package org.jtrim2.collections;

import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class EqualityTest {
    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(Equality.class);
    }

    @Test
    public void testReferenceEquality() {
        assertSame(ReferenceEquality.INSTANCE, Equality.referenceEquality());
    }

    @Test
    public void testNaturalEqualityEquals() {
        TestObject obj1 = new TestObject("OBJ1");

        assertFalse(Equality.naturalEquality().equals(null, obj1));
        assertFalse(Equality.naturalEquality().equals(obj1, null));
        assertFalse(Equality.naturalEquality().equals(obj1, new TestObject("OBJ2")));

        assertTrue(Equality.naturalEquality().equals(null, null));
        assertTrue(Equality.naturalEquality().equals(obj1, obj1));
        assertTrue(Equality.naturalEquality().equals(obj1, new TestObject("OBJ1")));
    }
}
