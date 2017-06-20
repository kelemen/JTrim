package org.jtrim2.property;

import org.jtrim2.event.ListenerRef;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DefaultPropertySourceProxyTest {
    private static MemProperty<Object> createMemProperty(Object initialValue) {
        return new MemProperty<>(initialValue, NoOpVerifier.getInstance(), PropertyFactory.noOpPublisher());
    }

    private static DefaultPropertySourceProxy<Object> create(PropertySource<Object> initialProperty) {
        return new DefaultPropertySourceProxy<>(initialProperty);
    }

    @Test
    public void testGetValueWithInitialProperty() {
        Object value1 = new Object();
        MemProperty<Object> backingProperty = createMemProperty(value1);

        DefaultPropertySourceProxy<Object> proxy = create(backingProperty);
        assertSame(value1, proxy.getValue());

        Object value2 = new Object();
        backingProperty.setValue(value2);
        assertSame(value2, proxy.getValue());

        backingProperty.setValue(null);
        assertNull(proxy.getValue());
    }

    @Test
    public void testListenerWithInitialPropertyChangingBackingProperty() {
        MemProperty<Object> backingProperty = createMemProperty(new Object());

        DefaultPropertySourceProxy<Object> proxy = create(backingProperty);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.addChangeListener(listener);

        backingProperty.setValue(new Object());
        verify(listener).run();

        listenerRef.unregister();
        backingProperty.setValue(new Object());
        verify(listener).run();
    }

    @Test
    public void testGetValueWithReplacedProperty() {
        Object value1 = new Object();
        MemProperty<Object> origBackingProperty = createMemProperty(new Object());
        MemProperty<Object> backingProperty = createMemProperty(value1);

        DefaultPropertySourceProxy<Object> proxy = create(origBackingProperty);
        proxy.replaceSource(backingProperty);

        assertSame(value1, proxy.getValue());

        Object value2 = new Object();
        backingProperty.setValue(value2);
        assertSame(value2, proxy.getValue());

        backingProperty.setValue(null);
        assertNull(proxy.getValue());
    }

    @Test
    public void testGetValueWithReplacedPropertyOldPropertyIsIgnored() {
        Object value1 = new Object();
        MemProperty<Object> origBackingProperty = createMemProperty(new Object());
        MemProperty<Object> backingProperty = createMemProperty(value1);

        DefaultPropertySourceProxy<Object> proxy = create(origBackingProperty);
        proxy.replaceSource(backingProperty);

        assertSame(value1, proxy.getValue());

        origBackingProperty.setValue(new Object());
        assertSame(value1, proxy.getValue());
    }

    @Test
    public void testListenerWithReplacedPropertyChangingBackingProperty() {
        MemProperty<Object> origBackingProperty = createMemProperty(new Object());
        MemProperty<Object> backingProperty = createMemProperty(new Object());

        DefaultPropertySourceProxy<Object> proxy = create(origBackingProperty);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.addChangeListener(listener);

        proxy.replaceSource(backingProperty);
        verify(listener).run();

        backingProperty.setValue(new Object());
        verify(listener, times(2)).run();

        listenerRef.unregister();
        backingProperty.setValue(new Object());
        verify(listener, times(2)).run();
    }

    @Test
    public void testListenerWithReplacedPropertyOldBackingHasNoEffect() {
        MemProperty<Object> origBackingProperty = createMemProperty(new Object());
        MemProperty<Object> backingProperty = createMemProperty(new Object());

        DefaultPropertySourceProxy<Object> proxy = create(origBackingProperty);
        proxy.replaceSource(backingProperty);

        Runnable listener = mock(Runnable.class);
        proxy.addChangeListener(listener);

        origBackingProperty.setValue(new Object());
        verifyZeroInteractions(listener);
    }
}
