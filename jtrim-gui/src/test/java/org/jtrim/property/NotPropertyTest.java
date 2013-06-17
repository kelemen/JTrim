package org.jtrim.property;

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
public class NotPropertyTest {
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
    public void testGetValue() {
        MutableProperty<Boolean> wrapped = memProperty(true, true);

        NotProperty property = new NotProperty(wrapped);
        assertFalse(property.getValue());
        assertNotNull(property.toString());

        wrapped.setValue(false);
        assertTrue(property.getValue());
        assertNotNull(property.toString());

        wrapped.setValue(null);
        assertNull(property.getValue());
        assertNotNull(property.toString());
    }

    @Test
    public void testChangeListener() {
        MutableProperty<Boolean> wrapped = memProperty(true);

        NotProperty property = new NotProperty(wrapped);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = property.addChangeListener(listener);

        wrapped.setValue(false);
        verify(listener).run();

        listenerRef.unregister();
        wrapped.setValue(true);
        verifyNoMoreInteractions(listener);
    }
}
