package org.jtrim2.property.swing;

import java.util.Objects;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;

/**
 *
 * @author Kelemen Attila
 */
abstract class AbstractMutableProperty<ValueType> implements MutableProperty<ValueType> {
    private final PropertySource<? extends ValueType> source;

    public AbstractMutableProperty(PropertySource<? extends ValueType> source) {
        Objects.requireNonNull(source, "source");
        this.source = source;
    }

    @Override
    public final ValueType getValue() {
        return source.getValue();
    }

    @Override
    public final ListenerRef addChangeListener(Runnable listener) {
        return source.addChangeListener(listener);
    }
}
