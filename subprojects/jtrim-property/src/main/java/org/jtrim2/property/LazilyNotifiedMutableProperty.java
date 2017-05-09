package org.jtrim2.property;

import java.util.Objects;
import org.jtrim2.collections.EqualityComparator;
import org.jtrim2.event.ListenerRef;

/**
 * @see PropertyFactory#lazilyNotifiedProperty(MutableProperty, EqualityComparator)
 */
final class LazilyNotifiedMutableProperty<ValueType> implements MutableProperty<ValueType> {
    private final MutableProperty<ValueType> wrapped;
    private final EqualityComparator<? super ValueType> equality;

    public LazilyNotifiedMutableProperty(
            MutableProperty<ValueType> wrapped,
            EqualityComparator<? super ValueType> equality) {
        Objects.requireNonNull(wrapped, "wrapped");
        Objects.requireNonNull(equality, "equality");

        this.wrapped = wrapped;
        this.equality = equality;
    }

    @Override
    public void setValue(ValueType value) {
        wrapped.setValue(value);
    }

    @Override
    public ValueType getValue() {
        return wrapped.getValue();
    }

    @Override
    public ListenerRef addChangeListener(final Runnable listener) {
        return wrapped.addChangeListener(
                new LazilyNotifiedPropertySource.LazyListener<>(wrapped, equality, listener));
    }
}
