package org.jtrim.property;

import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ProxyListenerRegistry;
import org.jtrim.utils.ExceptionHelper;

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
        ExceptionHelper.checkNotNullArgument(initialSource, "initialSource");

        this.listeners = new ProxyListenerRegistry<>(initialSource::addChangeListener);
        this.currentSource = initialSource;
    }

    @Override
    public void replaceSource(PropertySource<? extends ValueType> newSource) {
        ExceptionHelper.checkNotNullArgument(newSource, "newSource");
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
