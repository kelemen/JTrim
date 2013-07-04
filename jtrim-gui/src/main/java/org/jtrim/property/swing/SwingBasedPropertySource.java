
package org.jtrim.property.swing;

import java.util.Objects;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see SwingProperties#fromSwingSource(SwingPropertySource, SwingForwarderFactory)
 *
 * @author Kelemen Attila
 */
final class SwingBasedPropertySource<ValueType, ListenerType>
implements
        PropertySource<ValueType> {

    private final SwingPropertySource<? extends ValueType, ? super ListenerType> property;
    private final SwingForwarderFactory<? extends ListenerType> listenerForwarder;

    public SwingBasedPropertySource(
            SwingPropertySource<? extends ValueType, ListenerType> property,
            SwingForwarderFactory<? extends ListenerType> listenerForwarder) {
        ExceptionHelper.checkNotNullArgument(property, "property");
        ExceptionHelper.checkNotNullArgument(listenerForwarder, "listenerForwarder");

        this.property = property;
        this.listenerForwarder = listenerForwarder;
    }

    @Override
    public ValueType getValue() {
        return property.getValue();
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        final ListenerType swingListener = listenerForwarder.createForwarder(listener);
        Objects.requireNonNull(swingListener, "listenerForwarder.createForwarder(...)");

        property.addChangeListener(swingListener);

        return new ListenerRef() {
            private volatile boolean registered = true;

            @Override
            public boolean isRegistered() {
                return registered;
            }

            @Override
            public void unregister() {
                property.removeChangeListener(swingListener);
                registered = false;
            }
        };
    }

}
