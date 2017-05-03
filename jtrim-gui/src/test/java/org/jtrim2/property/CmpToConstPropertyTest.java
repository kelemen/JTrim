package org.jtrim2.property;

import org.jtrim2.collections.Equality;
import org.jtrim2.collections.EqualityComparator;
import org.jtrim2.event.ListenerRef;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.jtrim2.property.PropertyFactory.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class CmpToConstPropertyTest {
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

    private static EqualityComparator<TestObjWithIdentity> testObjCmp() {
        return TestObjWithIdentity.STR_CMP;
    }

    @Test
    public void testEqualWithNull() {
        MutableProperty<TestObjWithEquals> property = memProperty(new TestObjWithEquals("OBJ1"), true);

        CmpToConstProperty cmpProperty = new CmpToConstProperty(property, null, Equality.naturalEquality());
        assertFalse(cmpProperty.getValue());

        property.setValue(new TestObjWithEquals("OBJ2"));
        assertFalse(cmpProperty.getValue());

        property.setValue(null);
        assertTrue(cmpProperty.getValue());
    }

    @Test
    public void testEqual() {
        MutableProperty<TestObjWithIdentity> property = memProperty(new TestObjWithIdentity("OBJ1"));
        TestObjWithIdentity constValue = new TestObjWithIdentity("OBJ1");

        CmpToConstProperty cmpProperty = new CmpToConstProperty(property, constValue, testObjCmp());
        assertTrue(cmpProperty.getValue());
        assertNotNull(cmpProperty.toString());
    }

    @Test
    public void testNotEqual() {
        MutableProperty<TestObjWithIdentity> property = memProperty(new TestObjWithIdentity("OBJ1"));
        TestObjWithIdentity constValue = new TestObjWithIdentity("OBJ2");

        CmpToConstProperty cmpProperty = new CmpToConstProperty(property, constValue, testObjCmp());
        assertFalse(cmpProperty.getValue());
        assertNotNull(cmpProperty.toString());
    }

    @Test
    public void testChangeListener() {
        MutableProperty<TestObjWithIdentity> property = memProperty(new TestObjWithIdentity("OBJ1"));
        TestObjWithIdentity constValue = new TestObjWithIdentity("OBJ1");

        CmpToConstProperty cmpProperty = new CmpToConstProperty(property, constValue, testObjCmp());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = cmpProperty.addChangeListener(listener);

        property.setValue(new TestObjWithIdentity("OBJ2"));
        verify(listener).run();
        assertFalse(cmpProperty.getValue());

        listenerRef.unregister();
        property.setValue(new TestObjWithIdentity("OBJ3"));

        verifyNoMoreInteractions(listener);
    }
}
