package org.jtrim.property;

import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see PropertyFactory#protectedView(PropertySource)
 *
 * @author Kelemen Attila
 */
final class DelegatedPropertySource<ValueType> implements PropertySource<ValueType> {
    private final PropertySource<? extends ValueType> source;

    public DelegatedPropertySource(PropertySource<? extends ValueType> source) {
        ExceptionHelper.checkNotNullArgument(source, "source");
        this.source = source;
    }

    @Override
    public ValueType getValue() {
        return source.getValue();
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return source.addChangeListener(listener);
    }
}
