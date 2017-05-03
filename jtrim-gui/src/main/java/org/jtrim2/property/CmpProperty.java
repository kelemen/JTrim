package org.jtrim2.property;

import org.jtrim2.collections.EqualityComparator;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.utils.ExceptionHelper;

/**
 * @see BoolProperties#equals(PropertySource, PropertySource, EqualityComparator)
 *
 * @author Kelemen Attila
 */
final class CmpProperty implements PropertySource<Boolean> {
    private final Impl<?> impl;

    public <ValueType> CmpProperty(
            PropertySource<? extends ValueType> property1,
            PropertySource<? extends ValueType> property2,
            EqualityComparator<? super ValueType> comparator) {
        this.impl = new Impl<>(property1, property2, comparator);
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

    private static final class Impl<ValueType> extends MultiDependencyProperty<ValueType, Boolean> {
        private final EqualityComparator<? super ValueType> comparator;

        public Impl(
                PropertySource<? extends ValueType> property1,
                PropertySource<? extends ValueType> property2,
                EqualityComparator<? super ValueType> comparator) {
            super(property1, property2);

            ExceptionHelper.checkNotNullArgument(comparator, "comparator");

            this.comparator = comparator;
        }

        @Override
        public Boolean getValue() {
            return comparator.equals(properties[0].getValue(), properties[1].getValue());
        }
    }
}
