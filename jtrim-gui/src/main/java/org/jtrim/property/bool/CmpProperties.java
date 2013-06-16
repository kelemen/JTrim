package org.jtrim.property.bool;

import org.jtrim.collections.EqualityComparator;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see BoolProperties#equals(PropertySource, PropertySource, EqualityComparator)
 *
 * @author Kelemen Attila
 */
final class CmpProperties implements PropertySource<Boolean> {
    private final Impl<?> impl;

    public <ValueType> CmpProperties(
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

    private static final class Impl<ValueType> {
        private final PropertySource<? extends ValueType> property1;
        private final PropertySource<? extends ValueType> property2;
        private final EqualityComparator<? super ValueType> comparator;

        public Impl(
                PropertySource<? extends ValueType> property1,
                PropertySource<? extends ValueType> property2,
                EqualityComparator<? super ValueType> comparator) {
            ExceptionHelper.checkNotNullArgument(property1, "property1");
            ExceptionHelper.checkNotNullArgument(property2, "property2");
            ExceptionHelper.checkNotNullArgument(comparator, "comparator");

            this.property1 = property1;
            this.property2 = property2;
            this.comparator = comparator;
        }

        public boolean getValue() {
            return comparator.equals(property1.getValue(), property2.getValue());
        }

        public ListenerRef addChangeListener(Runnable listener) {
            ListenerRef ref1 = property1.addChangeListener(listener);
            ListenerRef ref2;

            try {
                ref2 = property2.addChangeListener(listener);
            } catch (Throwable ex) {
                try {
                    ref1.unregister();
                } catch (Throwable unregisterEx) {
                    ex.addSuppressed(unregisterEx);
                }
                throw ex;
            }

            if (ref1 == null) {
                ref2.unregister();
                throw new NullPointerException("ref1 == null");
            }

            if (ref2 == null) {
                ref1.unregister();
                throw new NullPointerException("ref2 == null");
            }

            return ListenerRegistries.combineListenerRefs(ref1, ref2);
        }
    }
}
