package org.jtrim.property.bool;

import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jtrim.property.PropertyFactory.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class BoolPropertiesTest {
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

    @SuppressWarnings("unchecked")
    public static <ValueType> PropertySource<ValueType> mockProperty() {
        return mock(PropertySource.class);
    }

    @Test
    public void testSame() {
        TestObjWithEquals value = new TestObjWithEquals("OBJ1");
        MutableProperty<TestObjWithEquals> property1 = memProperty(value);
        MutableProperty<TestObjWithEquals> property2 = memProperty(value);
        PropertySource<Boolean> cmp = BoolProperties.same(property1, property2);
        assertTrue(cmp instanceof CmpProperty);

        assertTrue(cmp.getValue());

        property1.setValue(new TestObjWithEquals("OBJ1"));
        assertFalse(cmp.getValue());
    }

    /**
     * Test of equals method, of class BoolProperties.
     */
    @Test
    public void testEquals_PropertySource_PropertySource() {
        MutableProperty<TestObjWithEquals> property1 = memProperty(new TestObjWithEquals("OBJ1"));
        MutableProperty<TestObjWithEquals> property2 = memProperty(new TestObjWithEquals("OBJ2"));
        PropertySource<Boolean> cmp = BoolProperties.equals(property1, property2);
        assertTrue(cmp instanceof CmpProperty);

        assertFalse(cmp.getValue());

        property2.setValue(new TestObjWithEquals("OBJ1"));
        assertTrue(cmp.getValue());
    }

    /**
     * Test of equals method, of class BoolProperties.
     */
    @Test
    public void testEquals_3args() {
        MutableProperty<TestObjWithIdentity> property1 = memProperty(new TestObjWithIdentity("OBJ1"));
        MutableProperty<TestObjWithIdentity> property2 = memProperty(new TestObjWithIdentity("OBJ2"));
        PropertySource<Boolean> cmp = BoolProperties.equals(property1, property2, TestObjWithIdentity.STR_CMP);
        assertTrue(cmp instanceof CmpProperty);

        assertFalse(cmp.getValue());

        property2.setValue(new TestObjWithIdentity("OBJ1"));
        assertTrue(cmp.getValue());
    }

    @Test
    public void testSameWithConst() {
        TestObjWithEquals constValue = new TestObjWithEquals("OBJ1");
        MutableProperty<TestObjWithEquals> property = memProperty(constValue);
        PropertySource<Boolean> cmp = BoolProperties.sameWithConst(property, constValue);
        assertTrue(cmp instanceof CmpToConstProperty);

        assertTrue(cmp.getValue());

        property.setValue(new TestObjWithEquals("OBJ1"));
        assertFalse(cmp.getValue());
    }

    @Test
    public void testEqualsWithConstNaturalEquals() {
        MutableProperty<TestObjWithEquals> property = memProperty(new TestObjWithEquals("OBJ1"));
        TestObjWithEquals constValue = new TestObjWithEquals("OBJ2");
        PropertySource<Boolean> cmp = BoolProperties.equalsWithConst(property, constValue);
        assertTrue(cmp instanceof CmpToConstProperty);

        assertFalse(cmp.getValue());

        property.setValue(new TestObjWithEquals("OBJ2"));
        assertTrue(cmp.getValue());
    }

    @Test
    public void testEqualsWithConst() {
        MutableProperty<TestObjWithIdentity> property = memProperty(new TestObjWithIdentity("OBJ1"));
        TestObjWithIdentity constValue = new TestObjWithIdentity("OBJ2");
        PropertySource<Boolean> cmp
                = BoolProperties.equalsWithConst(property, constValue, TestObjWithIdentity.STR_CMP);
        assertTrue(cmp instanceof CmpToConstProperty);

        assertFalse(cmp.getValue());

        property.setValue(new TestObjWithIdentity("OBJ2"));
        assertTrue(cmp.getValue());
    }

    @Test
    public void testNot() {
        MutableProperty<Boolean> wrapped = memProperty(true, true);

        PropertySource<Boolean> property = BoolProperties.not(wrapped);
        assertTrue(property instanceof NotProperty);
        assertFalse(property.getValue());
    }
}
