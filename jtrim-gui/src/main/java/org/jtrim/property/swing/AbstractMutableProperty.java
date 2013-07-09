package org.jtrim.property.swing;

import org.jtrim.event.ListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
abstract class AbstractMutableProperty<ValueType> implements MutableProperty<ValueType> {
    private final PropertySource<? extends ValueType> source;

    public AbstractMutableProperty(PropertySource<? extends ValueType> source) {
        ExceptionHelper.checkNotNullArgument(source, "source");
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
