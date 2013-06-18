package org.jtrim.property;

import org.jtrim.collections.EqualityComparator;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

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
