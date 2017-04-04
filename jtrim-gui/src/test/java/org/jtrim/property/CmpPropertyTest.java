package org.jtrim.property;

import org.jtrim.collections.Equality;
import org.jtrim.collections.EqualityComparator;
import org.jtrim.event.ListenerRef;
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
public class CmpPropertyTest {
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
    public void testEqual() {
        MutableProperty<TestObjWithIdentity> property1 = memProperty(new TestObjWithIdentity("OBJ1"));
        MutableProperty<TestObjWithIdentity> property2 = memProperty(new TestObjWithIdentity("OBJ1"));

        CmpProperty cmpProperty = new CmpProperty(property1, property2, testObjCmp());
        assertTrue(cmpProperty.getValue());
        assertNotNull(cmpProperty.toString());
    }

    @Test
    public void testNotEqual() {
        MutableProperty<TestObjWithIdentity> property1 = memProperty(new TestObjWithIdentity("OBJ1"));
        MutableProperty<TestObjWithIdentity> property2 = memProperty(new TestObjWithIdentity("OBJ2"));

        CmpProperty cmpProperty = new CmpProperty(property1, property2, testObjCmp());
        assertFalse(cmpProperty.getValue());
        assertNotNull(cmpProperty.toString());
    }

    @Test
    public void testChange1() {
        MutableProperty<TestObjWithIdentity> property1 = memProperty(new TestObjWithIdentity("OBJ"));
        MutableProperty<TestObjWithIdentity> property2 = memProperty(new TestObjWithIdentity("OBJ"));

        CmpProperty cmpProperty = new CmpProperty(property1, property2, testObjCmp());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = cmpProperty.addChangeListener(listener);

        property1.setValue(new TestObjWithIdentity("OBJ_A"));
        verify(listener).run();

        listenerRef.unregister();

        property1.setValue(new TestObjWithIdentity("OBJ_B"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testChange2() {
        MutableProperty<TestObjWithIdentity> property1 = memProperty(new TestObjWithIdentity("OBJ"));
        MutableProperty<TestObjWithIdentity> property2 = memProperty(new TestObjWithIdentity("OBJ"));

        CmpProperty cmpProperty = new CmpProperty(property1, property2, testObjCmp());

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = cmpProperty.addChangeListener(listener);

        property2.setValue(new TestObjWithIdentity("OBJ_A"));
        verify(listener).run();

        listenerRef.unregister();

        property2.setValue(new TestObjWithIdentity("OBJ_B"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testAddChangeListenerRobustness() {
        MultiPropertyFactory<Object, Boolean> factory = (property1, property2) -> {
            return new CmpProperty(property1, property2, Equality.naturalEquality());
        };

        ChangeListenerRobustnessTests<?> tests = new ChangeListenerRobustnessTests<>(factory);
        tests.runTests();
    }
}
