package org.jtrim.property;

import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see PropertyFactory#trimmedString(PropertySource)
 *
 * @author Kelemen Attila
 */
final class TrimmedPropertySource implements PropertySource<String> {
    private final PropertySource<String> wrapped;

    public TrimmedPropertySource(PropertySource<String> wrapped) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");

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
