package org.jtrim2.property;

import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.UnregisteredListenerRef;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see PropertyFactory#constSource(Object, PropertyPublisher)
 *
 * @author Kelemen Attila
 */
final class ConstSource<ValueType> implements PropertySource<ValueType> {
    private final ValueType value;
    private final PropertyPublisher<ValueType> publisher;

    public ConstSource(ValueType value, PropertyPublisher<ValueType> publisher) {
        ExceptionHelper.checkNotNullArgument(publisher, "publisher");

        this.value = value;
        this.publisher = publisher;
    }

    @Override
    public ValueType getValue() {
        return publisher.returnValue(value);
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return UnregisteredListenerRef.INSTANCE;
    }
}
