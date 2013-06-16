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

    /**
     * Test of equals method, of class BoolProperties.
     */
    @Test
    public void testEquals_PropertySource_PropertySource() {
        MutableProperty<TestObjWithEquals> property1 = memProperty(new TestObjWithEquals("OBJ1"));
        MutableProperty<TestObjWithEquals> property2 = memProperty(new TestObjWithEquals("OBJ2"));
        PropertySource<Boolean> cmp = BoolProperties.equals(property1, property2);
        assertTrue(cmp instanceof CmpProperties);

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
        assertTrue(cmp instanceof CmpProperties);

        assertFalse(cmp.getValue());

        property2.setValue(new TestObjWithIdentity("OBJ1"));
        assertTrue(cmp.getValue());
    }

}
