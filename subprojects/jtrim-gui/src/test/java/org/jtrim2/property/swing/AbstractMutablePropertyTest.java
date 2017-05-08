package org.jtrim2.property.swing;

import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.PropertySource;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AbstractMutablePropertyTest {
    @SuppressWarnings("unchecked")
    private static PropertySource<Object> mockSource() {
        return mock(PropertySource.class);
    }

    /**
     * Test of getValue method, of class AbstractMutableProperty.
     */
    @Test
    public void testGetValue() {
        PropertySource<Object> source = mockSource();
        AbstractMutablePropertyImpl property = new AbstractMutablePropertyImpl(source);

        Object value = new Object();
        stub(source.getValue()).toReturn(value);

        verifyZeroInteractions(source);

        assertSame(value, property.getValue());
        verify(source).getValue();
        verifyNoMoreInteractions(source);
    }

    /**
     * Test of addChangeListener method, of class AbstractMutableProperty.
     */
    @Test
    public void testAddChangeListener() {
        PropertySource<Object> source = mockSource();
        AbstractMutablePropertyImpl property = new AbstractMutablePropertyImpl(source);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = mock(ListenerRef.class);
        stub(source.addChangeListener(any(Runnable.class))).toReturn(listenerRef);

        verifyZeroInteractions(source);

        assertSame(listenerRef, property.addChangeListener(listener));
        verify(source).addChangeListener(same(listener));
        verifyNoMoreInteractions(source);
    }

    public class AbstractMutablePropertyImpl extends AbstractMutableProperty<Object> {
        public AbstractMutablePropertyImpl(PropertySource<Object> source) {
            super(source);
        }

        @Override
        public void setValue(Object value) {
        }
    }
}
