package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ProxyListenerRegistry;

/**
 * @see PropertyFactory#proxySource(PropertySource)
 *
 * @author Kelemen Attila
 */
final class DefaultPropertySourceProxy<ValueType>
implements
        PropertySourceProxy<ValueType> {
    private final ProxyListenerRegistry<Runnable> listeners;
    private volatile PropertySource<? extends ValueType> currentSource;

    public DefaultPropertySourceProxy(PropertySource<? extends ValueType> initialSource) {
        Objects.requireNonNull(initialSource, "initialSource");

        this.listeners = new ProxyListenerRegistry<>(initialSource::addChangeListener);
        this.currentSource = initialSource;
    }

    @Override
    public void replaceSource(PropertySource<? extends ValueType> newSource) {
        Objects.requireNonNull(newSource, "newSource");
        listeners.replaceRegistry(newSource::addChangeListener);
        currentSource = newSource;
        listeners.onEvent(EventListeners.runnableDispatcher(), null);
    }

    @Override
    public ValueType getValue() {
        return currentSource.getValue();
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return listeners.registerListener(listener);
    }
}
