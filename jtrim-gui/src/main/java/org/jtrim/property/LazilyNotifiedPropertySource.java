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

    // Retrieving "lastValue" initially is unsafe. Consider the following scenario:
    //
    // 1. The value is read to "lastValue" and is "A".
    // 2. The value concurrently changes to "B".
    // 3. The listener gets actually registered and will be notified of changes.
    // 4. Later the value changes to "A".
    //
    // Notice that we would fail to forward notification request. Even though,
    // the value changed after the listener got registered (addChangeListener returned).

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
