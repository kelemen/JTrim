package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.UnregisteredListenerRef;

/**
 * @see PropertyFactory#constSource(Object, PropertyPublisher)
 */
final class ConstSource<ValueType> implements PropertySource<ValueType> {
    private final ValueType value;
    private final PropertyPublisher<ValueType> publisher;

    public ConstSource(ValueType value, PropertyPublisher<ValueType> publisher) {
        Objects.requireNonNull(publisher, "publisher");

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
