package org.jtrim.property.swing;

import org.jtrim.event.EventListeners;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class SwingPropertiesTest {
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
     * Test of fromSwingSource method, of class SwingProperties.
     */
    @Test
    public void testFromSwingSource() {
        Object initialValue = new Object();
        TestSwingProperty wrapped = new TestSwingProperty(initialValue);
        PropertySource<Object> property
                = SwingProperties.fromSwingSource(wrapped, TestSwingProperty.listenerForwarder());

        assertTrue(property instanceof SwingBasedPropertySource);
        assertEquals(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        wrapped.setValue(new Object());
        verify(listener).run();

    }

    /**
     * Test of toSwingSource method, of class SwingProperties.
     */
    @Test
    public void testToSwingSource() {
        Object initialValue = new Object();
        MutableProperty<Object> wrapped = PropertyFactory.memProperty(initialValue);
        SwingPropertySource<Object, Runnable> property
                = SwingProperties.toSwingSource(wrapped, EventListeners.runnableDispatcher());

        assertTrue(property instanceof StandardBasedSwingPropertySource);
        assertEquals(initialValue, property.getValue());

        Runnable listener = mock(Runnable.class);
        property.addChangeListener(listener);
        wrapped.setValue(new Object());
        verify(listener).run();
    }
}
