package org.jtrim.property;

import org.jtrim.collections.EqualityComparator;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
final class LazilyNotifiedPropertySource<ValueType> implements PropertySource<ValueType> {
    private final PropertySource<? extends ValueType> wrapped;
    private final EqualityComparator<? super ValueType> equality;

    public LazilyNotifiedPropertySource(
            PropertySource<? extends ValueType> wrapped,
            EqualityComparator<? super ValueType> equality) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");
        ExceptionHelper.checkNotNullArgument(equality, "equality");

        this.wrapped = wrapped;
        this.equality = equality;
    }

    @Override
    public ValueType getValue() {
        return wrapped.getValue();
    }

    @Override
    public ListenerRef addChangeListener(final Runnable listener) {
        return wrapped.addChangeListener(new LazyListener<>(wrapped, equality, listener));
    }

    static final class LazyListener<ValueType> implements Runnable {
        private final PropertySource<? extends ValueType> wrapped;
        private final EqualityComparator<? super ValueType> equality;
        private final Runnable listener;
        private ValueType lastValue;
        private boolean hasBeenRead;

        public LazyListener(
                PropertySource<? extends ValueType> wrapped,
                EqualityComparator<? super ValueType> equality,
                Runnable listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            this.wrapped = wrapped;
            this.equality = equality;
            this.hasBeenRead = false;
            this.listener = listener;
        }

        @Override
        public void run() {
            ValueType currentValue = wrapped.getValue();
            ValueType prevValue = lastValue;
            lastValue = currentValue;

            if (!hasBeenRead || !equality.equals(currentValue, prevValue)) {
                listener.run();
            }
            hasBeenRead = true;
        }
    }
}
