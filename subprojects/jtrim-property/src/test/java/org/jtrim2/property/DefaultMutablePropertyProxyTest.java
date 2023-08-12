package org.jtrim2.property;

import org.jtrim2.event.ListenerRef;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DefaultMutablePropertyProxyTest {
    private static MemProperty<Object> createMemProperty(Object initialValue) {
        return new MemProperty<>(initialValue, PropertyFactory.noOpVerifier(), PropertyFactory.noOpPublisher());
    }

    private static DefaultMutablePropertyProxy<Object> create(MutableProperty<Object> initialProperty) {
        return new DefaultMutablePropertyProxy<>(initialProperty);
    }

    @Test
    public void testGetValueWithInitialProperty() {
        Object value1 = new Object();
        MemProperty<Object> backingProperty = createMemProperty(value1);

        DefaultMutablePropertyProxy<Object> proxy = create(backingProperty);
        assertSame(value1, proxy.getValue());

        Object value2 = new Object();
        backingProperty.setValue(value2);
        assertSame(value2, proxy.getValue());

        backingProperty.setValue(null);
        assertNull(proxy.getValue());
    }

    @Test
    public void testSetValueWithInitialProperty() {
        MemProperty<Object> backingProperty = createMemProperty(new Object());

        DefaultMutablePropertyProxy<Object> proxy = create(backingProperty);

        Object value1 = new Object();
        proxy.setValue(value1);
        assertSame(value1, backingProperty.getValue());
        assertSame(value1, proxy.getValue());

        Object value2 = new Object();
        proxy.setValue(value2);
        assertSame(value2, backingProperty.getValue());
        assertSame(value2, proxy.getValue());

        proxy.setValue(null);
        assertNull(backingProperty.getValue());
        assertNull(proxy.getValue());
    }

    @Test
    public void testListenerWithInitialPropertyChangingBackingProperty() {
        MemProperty<Object> backingProperty = createMemProperty(new Object());

        DefaultMutablePropertyProxy<Object> proxy = create(backingProperty);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.addChangeListener(listener);

        backingProperty.setValue(new Object());
        verify(listener).run();

        listenerRef.unregister();
        backingProperty.setValue(new Object());
        verify(listener).run();
    }

    @Test
    public void testListenerWithInitialPropertyChangingDirectly() {
        MemProperty<Object> backingProperty = createMemProperty(new Object());

        DefaultMutablePropertyProxy<Object> proxy = create(backingProperty);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.addChangeListener(listener);

        proxy.setValue(new Object());
        verify(listener).run();

        listenerRef.unregister();
        proxy.setValue(new Object());
        verify(listener).run();
    }

    @Test
    public void testGetValueWithReplacedProperty() {
        Object value1 = new Object();
        MemProperty<Object> origBackingProperty = createMemProperty(new Object());
        MemProperty<Object> backingProperty = createMemProperty(value1);

        DefaultMutablePropertyProxy<Object> proxy = create(origBackingProperty);
        proxy.replaceProperty(backingProperty);

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

        DefaultMutablePropertyProxy<Object> proxy = create(origBackingProperty);
        proxy.replaceProperty(backingProperty);

        assertSame(value1, proxy.getValue());

        origBackingProperty.setValue(new Object());
        assertSame(value1, proxy.getValue());
    }

    @Test
    public void testSetValueWithReplacedProperty() {
        Object origValue = new Object();
        MemProperty<Object> origBackingProperty = createMemProperty(origValue);
        MemProperty<Object> backingProperty = createMemProperty(new Object());

        DefaultMutablePropertyProxy<Object> proxy = create(origBackingProperty);
        proxy.replaceProperty(backingProperty);

        Object value1 = new Object();
        proxy.setValue(value1);
        assertSame(value1, backingProperty.getValue());
        assertSame(value1, proxy.getValue());

        Object value2 = new Object();
        proxy.setValue(value2);
        assertSame(value2, backingProperty.getValue());
        assertSame(value2, proxy.getValue());

        proxy.setValue(null);
        assertNull(backingProperty.getValue());
        assertNull(proxy.getValue());

        assertSame(origValue, origBackingProperty.getValue());
    }

    @Test
    public void testListenerWithReplacedPropertyChangingBackingProperty() {
        MemProperty<Object> origBackingProperty = createMemProperty(new Object());
        MemProperty<Object> backingProperty = createMemProperty(new Object());

        DefaultMutablePropertyProxy<Object> proxy = create(origBackingProperty);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.addChangeListener(listener);

        proxy.replaceProperty(backingProperty);
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

        DefaultMutablePropertyProxy<Object> proxy = create(origBackingProperty);
        proxy.replaceProperty(backingProperty);

        Runnable listener = mock(Runnable.class);
        proxy.addChangeListener(listener);

        origBackingProperty.setValue(new Object());
        verifyNoInteractions(listener);
    }

    @Test
    public void testListenerWithReplacedPropertyChangingDirectly() {
        MemProperty<Object> origBackingProperty = createMemProperty(new Object());
        MemProperty<Object> backingProperty = createMemProperty(new Object());

        DefaultMutablePropertyProxy<Object> proxy = create(origBackingProperty);

        Runnable listener = mock(Runnable.class);
        ListenerRef listenerRef = proxy.addChangeListener(listener);

        proxy.replaceProperty(backingProperty);
        verify(listener).run();

        proxy.setValue(new Object());
        verify(listener, times(2)).run();

        listenerRef.unregister();
        proxy.setValue(new Object());
        verify(listener, times(2)).run();
    }
}
