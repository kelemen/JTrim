package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ProxyListenerRegistry;

/**
 * @see PropertyFactory#proxyProperty(MutableProperty)
 */
final class DefaultMutablePropertyProxy<ValueType>
implements
        MutablePropertyProxy<ValueType> {

    private final ProxyListenerRegistry<Runnable> listeners;
    private volatile MutableProperty<ValueType> currentProperty;

    public DefaultMutablePropertyProxy(MutableProperty<ValueType> initialProperty) {
        Objects.requireNonNull(initialProperty, "initialProperty");

        this.listeners = new ProxyListenerRegistry<>(initialProperty::addChangeListener);
        this.currentProperty = initialProperty;
    }

    @Override
    public void replaceProperty(MutableProperty<ValueType> newProperty) {
        Objects.requireNonNull(newProperty, "newProperty");

        listeners.replaceRegistry(newProperty::addChangeListener);
        currentProperty = newProperty;
        listeners.onEvent(EventListeners.runnableDispatcher(), null);
    }

    @Override
    public void setValue(ValueType value) {
        currentProperty.setValue(value);
    }

    @Override
    public ValueType getValue() {
        return currentProperty.getValue();
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return listeners.registerListener(listener);
    }
}
