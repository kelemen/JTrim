package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.event.ListenerRef;

/**
 * @see PropertyFactory#trimmedString(PropertySource)
 */
final class TrimmedPropertySource implements PropertySource<String> {
    private final PropertySource<String> wrapped;

    public TrimmedPropertySource(PropertySource<String> wrapped) {
        Objects.requireNonNull(wrapped, "wrapped");

        this.wrapped = wrapped;
    }

    @Override
    public String getValue() {
        String value = wrapped.getValue();
        return value != null ? value.trim() : null;
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return wrapped.addChangeListener(listener);
    }
}
