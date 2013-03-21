package org.jtrim.property;

import org.jtrim.event.ListenerRef;
import org.jtrim.event.ProxyListenerRegistry;
import org.jtrim.event.SimpleListenerRegistry;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see PropertyFactory#proxyProperty(MutableProperty)
 *
 * @author Kelemen Attila
 */
final class DefaultMutablePropertyProxy<ValueType>
implements
        MutablePropertyProxy<ValueType> {

    private final ProxyListenerRegistry<Runnable> listeners;
    private volatile MutableProperty<ValueType> currentProperty;

    public DefaultMutablePropertyProxy(MutableProperty<ValueType> initialProperty) {
        ExceptionHelper.checkNotNullArgument(initialProperty, "initialProperty");

        this.listeners = new ProxyListenerRegistry<>(asListenerRegistry(initialProperty));
        this.currentProperty = initialProperty;
    }

    private static SimpleListenerRegistry<Runnable> asListenerRegistry(final PropertySource<?> source) {
        assert source != null;

        return new SimpleListenerRegistry<Runnable>() {
            @Override
            public ListenerRef registerListener(Runnable listener) {
                return source.addChangeListener(listener);
            }
        };
    }

    @Override
    public void replaceProperty(MutableProperty<ValueType> newProperty) {
        ExceptionHelper.checkNotNullArgument(newProperty, "newProperty");

        listeners.replaceRegistry(asListenerRegistry(newProperty));
        currentProperty = newProperty;
        listeners.onEvent(RunnableDispatcher.INSTANCE, null);
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
