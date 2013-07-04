package org.jtrim.property.swing;

import org.jtrim.event.EventDispatcher;
import org.jtrim.property.PropertySource;

/**
 *
 * @author Kelemen Attila
 */
public final class SwingProperties {
    /***/
    public static <ValueType, ListenerType> PropertySource<ValueType> fromSwingSource(
            final SwingPropertySource<? extends ValueType, ? super ListenerType> property,
            final SwingForwarderFactory<? extends ListenerType> listenerForwarder) {

        return new SwingBasedPropertySource<>(property, listenerForwarder);
    }

    /***/
    public static <ValueType, ListenerType> SwingPropertySource<ValueType, ListenerType> toSwingSource(
            PropertySource<? extends ValueType> property,
            EventDispatcher<? super ListenerType, Void> eventDispatcher) {

        return new StandardBasedSwingPropertySource<>(property, eventDispatcher);
    }

    private SwingProperties() {
        throw new AssertionError();
    }
}
