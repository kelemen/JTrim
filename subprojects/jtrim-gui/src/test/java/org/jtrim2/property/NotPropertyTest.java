package org.jtrim2.property;

import org.jtrim2.event.ListenerRef;
import org.junit.Test;

import static org.jtrim2.property.PropertyFactory.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NotPropertyTest {
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
