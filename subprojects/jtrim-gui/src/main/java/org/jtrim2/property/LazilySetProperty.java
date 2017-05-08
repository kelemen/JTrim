package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.collections.EqualityComparator;
import org.jtrim2.event.ListenerRef;

/**
 *
 * @author Kelemen Attila
 */
final class LazilySetProperty<ValueType> implements MutableProperty<ValueType> {
    private final MutableProperty<ValueType> wrapped;
    private final EqualityComparator<? super ValueType> equality;

    public LazilySetProperty(
            MutableProperty<ValueType> wrapped,
            EqualityComparator<? super ValueType> equality) {
        Objects.requireNonNull(wrapped, "wrapped");
        Objects.requireNonNull(equality, "equality");

        this.wrapped = wrapped;
        this.equality = equality;
    }

    @Override
    public void setValue(ValueType value) {
        if (!equality.equals(getValue(), value)) {
            wrapped.setValue(value);
        }
    }

    @Override
    public ValueType getValue() {
        return wrapped.getValue();
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return wrapped.addChangeListener(listener);
    }
}
