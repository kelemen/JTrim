package org.jtrim2.property.swing;

import java.util.Objects;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.PropertySource;

/**
 * @see SwingProperties#fromSwingSource(SwingPropertySource, SwingForwarderFactory)
 */
final class SwingBasedPropertySource<ValueType, ListenerType>
implements
        PropertySource<ValueType> {

    private final SwingPropertySource<? extends ValueType, ? super ListenerType> property;
    private final SwingForwarderFactory<? extends ListenerType> listenerForwarder;

    public SwingBasedPropertySource(
            SwingPropertySource<? extends ValueType, ListenerType> property,
            SwingForwarderFactory<? extends ListenerType> listenerForwarder) {
        Objects.requireNonNull(property, "property");
        Objects.requireNonNull(listenerForwarder, "listenerForwarder");

        this.property = property;
        this.listenerForwarder = listenerForwarder;
    }

    @Override
    public ValueType getValue() {
        return property.getValue();
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");

        final ListenerType swingListener = listenerForwarder.createForwarder(listener);
        Objects.requireNonNull(swingListener, "listenerForwarder.createForwarder(...)");

        property.addChangeListener(swingListener);
        return () -> {
            property.removeChangeListener(swingListener);
        };
    }

}
