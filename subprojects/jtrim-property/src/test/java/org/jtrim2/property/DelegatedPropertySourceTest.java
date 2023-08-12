package org.jtrim2.property;

import org.jtrim2.event.ListenerRef;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DelegatedPropertySourceTest {
    @SuppressWarnings("unchecked")
    private static PropertySource<Object> mockProperty() {
        return mock(PropertySource.class);
    }

    /**
     * Test of getValue method, of class DelegatedPropertySource.
     */
    @Test
    public void testGetValue() {
        PropertySource<Object> wrapped = mockProperty();
        DelegatedPropertySource<Object> delegated = new DelegatedPropertySource<>(wrapped);

        Object value = new Object();
        when(wrapped.getValue()).thenReturn(value);

        assertSame(value, delegated.getValue());
        verify(wrapped).getValue();
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of addChangeListener method, of class DelegatedPropertySource.
     */
    @Test
    public void testAddChangeListener() {
        PropertySource<Object> wrapped = mockProperty();
        DelegatedPropertySource<Object> delegated = new DelegatedPropertySource<>(wrapped);

        ListenerRef listenerRef = mock(ListenerRef.class);
        when(wrapped.addChangeListener(any(Runnable.class))).thenReturn(listenerRef);

        Runnable listener = mock(Runnable.class);
        assertSame(listenerRef, delegated.addChangeListener(listener));
        verify(wrapped).addChangeListener(same(listener));
        verifyNoMoreInteractions(wrapped);
    }
}
