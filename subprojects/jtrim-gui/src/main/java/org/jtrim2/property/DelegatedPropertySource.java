package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.event.ListenerRef;

/**
 * @see PropertyFactory#protectedView(PropertySource)
 */
final class DelegatedPropertySource<ValueType> implements PropertySource<ValueType> {
    private final PropertySource<? extends ValueType> source;

    public DelegatedPropertySource(PropertySource<? extends ValueType> source) {
        Objects.requireNonNull(source, "source");
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
