package org.jtrim2.property;

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
public class OrPropertyTest {
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

    private void testGetValue(Boolean input1, Boolean input2, boolean expected) {
        PropertySource<Boolean> property1 = constSource(input1);
        PropertySource<Boolean> property2 = constSource(input2);

        OrProperty orProperty = new OrProperty(property1, property2);
        assertEquals(expected, orProperty.getValue());
    }

    @Test
    public void testGetValue() {
        testGetValue(false, false, false);
        testGetValue(null, false, false);
        testGetValue(false, null, false);
        testGetValue(null, null, false);

        testGetValue(true, true, true);
        testGetValue(false, true, true);
        testGetValue(true, false, true);
        testGetValue(null, true, true);
        testGetValue(true, null, true);
    }

    private void testGetValueForSingle(Boolean input, boolean expected) {
        PropertySource<Boolean> property = constSource(input);

        OrProperty orProperty = new OrProperty(property);
        assertEquals(expected, orProperty.getValue());
    }

    @Test
    public void testGetValueForSingle() {
        testGetValueForSingle(false, false);
        testGetValueForSingle(null, false);
        testGetValueForSingle(true, true);
    }

    @Test
    public void testGetValueForZero() {
        OrProperty orProperty = new OrProperty();
        assertEquals(false, orProperty.getValue());
    }

    @Test
    public void testListenerChangeFrom1() {
        MutableProperty<Boolean> property1 = memProperty(false);
        MutableProperty<Boolean> property2 = memProperty(false);

        OrProperty orProperty = new OrProperty(property1, property2);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = orProperty.addChangeListener(listener);

        property1.setValue(true);
        verify(listener).run();

        listenerRef.unregister();
        property1.setValue(false);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testListenerChangeFrom2() {
        MutableProperty<Boolean> property1 = memProperty(false);
        MutableProperty<Boolean> property2 = memProperty(false);

        OrProperty orProperty = new OrProperty(property1, property2);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = orProperty.addChangeListener(listener);

        property2.setValue(true);
        verify(listener).run();

        listenerRef.unregister();
        property2.setValue(false);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testAddChangeListenerRobustness() {
        MultiPropertyFactory<Boolean, Boolean> factory = OrProperty::new;

        ChangeListenerRobustnessTests<?> tests = new ChangeListenerRobustnessTests<>(factory);
        tests.runTests();
    }
}
