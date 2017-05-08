package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.event.ListenerRef;

/**
 * @see BoolProperties#not(PropertySource)
 */
final class NotProperty implements PropertySource<Boolean> {
    private final PropertySource<Boolean> wrapped;

    public NotProperty(PropertySource<Boolean> wrapped) {
        Objects.requireNonNull(wrapped, "wrapped");
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
