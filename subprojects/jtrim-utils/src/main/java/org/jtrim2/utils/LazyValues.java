package org.jtrim2.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Defines utility methods to help with lazily initialized values.
 */
public final class LazyValues {
    private static final Object NIL = new Object();

    /**
     * Returns a factory caching the value returned by the given factory. The value is cached forever.
     * <P>
     * Note: Despite caching the value forever, the returned factory does not guarantee that it
     * won't request the value multiple times from the specified factory. That is, the implementation
     * does not hold locks (or wait for other threads) while creating the value. Therefore, it is
     * possible that value gets created multiple times. However, it is guaranteed that every invocation
     * of the {@code get} method of the returned factory will return the exact same instance.
     *
     * @param <T> the type of the cached value
     * @param valueFactory the factory creating the value. This argument cannot be {@code null}.
     * @return a factory caching the value returned by the given factory. This method
     *   never returns {@code null}.
     */
    public static <T> Supplier<T> lazyValue(Supplier<? extends T> valueFactory) {
        return new LazyValue<>(valueFactory);
    }

    @SuppressWarnings("unchecked")
    private static <T> T castLazyValue(Object obj) {
        return obj != NIL ? (T)obj : null;
    }

    private static Object wrapLazyValue(Object obj) {
        return obj != null ? obj : NIL;
    }

    private static final class LazyValue<T> implements Supplier<T> {
        private volatile Supplier<? extends T> valueFactory;
        private final AtomicReference<Object> valueRef;

        public LazyValue(Supplier<? extends T> valueFactory) {
            this.valueFactory = Objects.requireNonNull(valueFactory, "valueFactory");
            this.valueRef = new AtomicReference<>(null);
        }

        @Override
        public T get() {
            Object result = valueRef.get();
            if (result == null) {
                Supplier<? extends T> currentValueFactory = valueFactory;
                if (currentValueFactory == null) {
                    return castLazyValue(valueRef.get());
                }

                result = wrapLazyValue(currentValueFactory.get());
                if (!valueRef.compareAndSet(null, result)) {
                    result = valueRef.get();
                }
                else {
                    valueFactory = null;
                }
            }
            return castLazyValue(result);
        }

        @Override
        public String toString() {
            Object value = valueRef.get();
            String valueStr = value != null ? value.toString() : "?";

            return "LazyValue{" + valueStr + '}';
        }
    }

    private LazyValues() {
        throw new AssertionError();
    }
}
