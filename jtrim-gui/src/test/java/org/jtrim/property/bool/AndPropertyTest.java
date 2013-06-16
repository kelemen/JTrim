package org.jtrim.property.bool;

import org.jtrim.event.ListenerRef;
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
public class AndPropertyTest {
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

        AndProperty andProperty = new AndProperty(property1, property2);
        assertEquals(expected, andProperty.getValue());
    }

    @Test
    public void testGetValue() {
        testGetValue(false, false, false);
        testGetValue(null, false, false);
        testGetValue(false, null, false);
        testGetValue(false, true, false);
        testGetValue(true, false, false);

        testGetValue(true, true, true);
        testGetValue(null, null, true);
        testGetValue(null, true, true);
        testGetValue(true, null, true);
    }

    private void testGetValueForSingle(Boolean input, boolean expected) {
        PropertySource<Boolean> property = constSource(input);

        AndProperty andProperty = new AndProperty(property);
        assertEquals(expected, andProperty.getValue());
    }

    @Test
    public void testGetValueForSingle() {
        testGetValueForSingle(false, false);
        testGetValueForSingle(null, true);
        testGetValueForSingle(true, true);
    }

    @Test
    public void testGetValueForZero() {
        AndProperty andProperty = new AndProperty();
        assertEquals(true, andProperty.getValue());
    }

    @Test
    public void testListenerChangeFrom1() {
        MutableProperty<Boolean> property1 = memProperty(true);
        MutableProperty<Boolean> property2 = memProperty(true);

        AndProperty andProperty = new AndProperty(property1, property2);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = andProperty.addChangeListener(listener);

        property1.setValue(false);
        verify(listener).run();

        listenerRef.unregister();
        property1.setValue(true);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testListenerChangeFrom2() {
        MutableProperty<Boolean> property1 = memProperty(true);
        MutableProperty<Boolean> property2 = memProperty(true);

        AndProperty andProperty = new AndProperty(property1, property2);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = andProperty.addChangeListener(listener);

        property2.setValue(false);
        verify(listener).run();

        listenerRef.unregister();
        property2.setValue(true);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testAddChangeListenerRobustness() {
        MultiPropertyFactory<?, ?> factory = new MultiPropertyFactory<Boolean, Boolean>() {
            @Override
            public PropertySource<Boolean> create(
                    PropertySource<Boolean> property1,
                    PropertySource<Boolean> property2) {
                return new AndProperty(property1, property2);
            }
        };

        ChangeListenerRobustnessTests<?> tests = new ChangeListenerRobustnessTests<>(factory);
        tests.runTests();
    }
}
