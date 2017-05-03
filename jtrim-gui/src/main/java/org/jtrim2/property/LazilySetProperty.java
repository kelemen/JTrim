package org.jtrim2.property;

import org.jtrim2.collections.EqualityComparator;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.utils.ExceptionHelper;

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
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");
        ExceptionHelper.checkNotNullArgument(equality, "equality");

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
