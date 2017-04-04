package org.jtrim.property.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author Kelemen Attila
 */
final class TestSwingProperty implements SwingPropertySource<Object, PropertyChangeListener> {
    private final PropertyChangeSupport listeners;
    private Object value;

    public TestSwingProperty(Object initialValue) {
        this.listeners = new PropertyChangeSupport(this);
        this.value = initialValue;
    }

    public void setValue(Object newValue) {
        this.value = newValue;
        listeners.firePropertyChange("value", null, null);
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public void addChangeListener(PropertyChangeListener listener) {
        listeners.addPropertyChangeListener(listener);
    }

    @Override
    public void removeChangeListener(PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(listener);
    }

    public static SwingForwarderFactory<PropertyChangeListener> listenerForwarder() {
        return TestSwingProperty::createForwarder;
    }

    private static PropertyChangeListener createForwarder(Runnable listener) {
        assert listener != null;
        return (PropertyChangeEvent evt) -> listener.run();
    }
}
