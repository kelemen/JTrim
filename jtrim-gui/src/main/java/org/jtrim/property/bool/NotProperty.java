package org.jtrim.property.bool;

import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see BoolProperties#not(PropertySource)
 *
 * @author Kelemen Attila
 */
final class NotProperty implements PropertySource<Boolean> {
    private final PropertySource<Boolean> wrapped;

    public NotProperty(PropertySource<Boolean> wrapped) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");
        this.wrapped = wrapped;
    }

    @Override
    public Boolean getValue() {
        Boolean wrappedValue = wrapped.getValue();
        return wrappedValue != null ? !wrappedValue : null;
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return wrapped.addChangeListener(listener);
    }

    @Override
    public String toString() {
        Boolean value = getValue();
        return value != null ? value.toString() : "null";
    }
}
