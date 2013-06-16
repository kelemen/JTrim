package org.jtrim.property.bool;

import org.jtrim.collections.EqualityComparator;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class CmpToConstProperty implements PropertySource<Boolean> {
    private final Impl<?> impl;

    public <ValueType> CmpToConstProperty(
            PropertySource<? extends ValueType> property,
            ValueType constValue,
            EqualityComparator<? super ValueType> comparator) {
        this.impl = new Impl<>(property, constValue, comparator);
    }

    @Override
    public Boolean getValue() {
        return impl.getValue();
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return impl.addChangeListener(listener);
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

    private static final class Impl<ValueType> {
        private final PropertySource<? extends ValueType> property;
        private final ValueType constValue;
        private final EqualityComparator<? super ValueType> comparator;

        public Impl(
                PropertySource<? extends ValueType> property,
                ValueType constValue,
                EqualityComparator<? super ValueType> comparator) {
            ExceptionHelper.checkNotNullArgument(property, "property");
            ExceptionHelper.checkNotNullArgument(constValue, "constValue");
            ExceptionHelper.checkNotNullArgument(comparator, "comparator");

            this.property = property;
            this.constValue = constValue;
            this.comparator = comparator;
        }

        public boolean getValue() {
            return comparator.equals(property.getValue(), constValue);
        }

        public ListenerRef addChangeListener(Runnable listener) {
            return property.addChangeListener(listener);
        }
    }
}
