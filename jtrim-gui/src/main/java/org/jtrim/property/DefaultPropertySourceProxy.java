package org.jtrim.property;

import org.jtrim.event.ListenerRef;
import org.jtrim.event.ProxyListenerRegistry;
import org.jtrim.event.SimpleListenerRegistry;
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

        this.listeners = new ProxyListenerRegistry<>(asListenerRegistry(initialSource));
        this.currentSource = initialSource;
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
    public void replaceSource(PropertySource<? extends ValueType> newSource) {
        ExceptionHelper.checkNotNullArgument(newSource, "newSource");
        listeners.replaceRegistry(asListenerRegistry(newSource));
        currentSource = newSource;
        listeners.onEvent(RunnableDispatcher.INSTANCE, null);
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
