package org.jtrim2.utils;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Defines utility methods to help with lazily initialized values.
 */
public final class LazyValues {
    private static final Object NIL = new Object();
    private static final String UNKNOWN_VALUE_STR = "?";
    private static final String NULL_VALUE_STR = "null";

    /**
     * Returns a factory caching non-null value returned by the given factory. The value is cached forever.
     * <P>
     * Note: Despite caching the value forever, the returned factory does not guarantee that it
     * won't request the value multiple times from the specified factory. That is, the implementation
     * does not hold locks (or wait for other threads) while creating the value. Therefore, it is
     * possible that value gets created multiple times. However, it is guaranteed that every invocation
     * of the {@code get} method of the returned factory will return the exact same instance.
     *
     * @param <T> the type of the cached value
     * @param valueFactory the factory creating the value. This argument cannot be {@code null}.
     *   The factory may return a {@code null} value but {@code null} values are not cached, therefore
     *   the factory will be called again next time a value is requested if it returns {@code null}.
     * @return a factory caching the value returned by the given factory. This method
     *   never returns {@code null}.
     *
     * @see #lazyValueLocked(Supplier)
     */
    public static <T> Supplier<T> lazyNonNullValue(Supplier<? extends T> valueFactory) {
        return lazyNonNullValue(valueFactory, (obj, accepted) -> { });
    }

    /**
     * Returns a factory caching non-null value returned by the given factory. The value is cached forever.
     * <P>
     * An action can be specified what to do with objects after the factory creates them.
     * <B>Warning</B>: There is no guarantee that if the returned {@code Supplier} returns an
     * object, the post create action has already been called on that object. The only guarantee
     * is that it will be called <I>eventually</I>. Note however, that since it is guaranteed
     * that the returned provider never returns different objects, the post create action will
     * not be called more than once with its {@code accepted} argument {@code true}.
     * <P>
     * Note: Despite caching the value forever, the returned factory does not guarantee that it
     * won't request the value multiple times from the specified factory. That is, the implementation
     * does not hold locks (or wait for other threads) while creating the value. Therefore, it is
     * possible that value gets created multiple times. However, it is guaranteed that every invocation
     * of the {@code get} method of the returned factory will return the exact same instance.
     *
     * @param <T> the type of the cached value
     * @param valueFactory the factory creating the value. This argument cannot be {@code null}.
     *   The factory may return a {@code null} value but {@code null} values are not cached, therefore
     *   the factory will be called again next time a value is requested if it returns {@code null}.
     * @param postCreateAction the action to be called after the given factory creates an object.
     *   This argument cannot be {@code null}.
     * @return a factory caching the value returned by the given factory. This method
     *   never returns {@code null}.
     *
     * @see #lazyValueLocked(Supplier)
     */
    public static <T> Supplier<T> lazyNonNullValue(
            Supplier<? extends T> valueFactory,
            PostCreateAction<? super T> postCreateAction) {
        return new LazyNonNullValue<>(valueFactory, postCreateAction);
    }

    /**
     * Returns a factory caching non-null value returned by the given factory. The value is cached forever.
     * <P>
     * An action can be specified what to do with objects after the factory creates them.
     * <B>Warning</B>: There is no guarantee that if the returned {@code Supplier} returns an
     * object, {@code initAction} has already been called on that object. The only guarantee
     * is that it will be called <I>eventually</I>. Note however, that since it is guaranteed
     * that the returned provider never returns different objects, {@code initAction} will
     * not be called more than once.
     * <P>
     * Note: Despite caching the value forever, the returned factory does not guarantee that it
     * won't request the value multiple times from the specified factory. That is, the implementation
     * does not hold locks (or wait for other threads) while creating the value. Therefore, it is
     * possible that value gets created multiple times. However, it is guaranteed that every invocation
     * of the {@code get} method of the returned factory will return the exact same instance.
     *
     * @param <T> the type of the cached value
     * @param valueFactory the factory creating the value. This argument cannot be {@code null}.
     *   The factory may return a {@code null} value but {@code null} values are not cached, therefore
     *   the factory will be called again next time a value is requested if it returns {@code null}.
     * @param initAction the action to be called after the given factory creates an object which
     *   is going to be published. This action is not called for discarded objects.
     *   This argument cannot be {@code null}.
     * @return a factory caching the value returned by the given factory. This method
     *   never returns {@code null}.
     *
     * @see #lazyValueLocked(Supplier)
     * @see #lazyNonNullValue(Supplier, PostCreateAction)
     */
    public static <T> Supplier<T> lazyNonNullValueEventualInit(
            Supplier<? extends T> valueFactory,
            Consumer<? super T> initAction) {
        return lazyNonNullValue(valueFactory, (obj, accepted) -> {
            if (accepted) {
                initAction.accept(obj);
            }
        });
    }

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
     *
     * @see #lazyValueLocked(Supplier)
     */
    public static <T> Supplier<T> lazyValue(Supplier<? extends T> valueFactory) {
        return new LazyValue<>(valueFactory);
    }

    /**
     * Returns a factory caching the value returned by the given factory. The value is cached forever.
     * <P>
     * Unlike {@link #lazyValue(Supplier) lazyValue}, the factory returned by this method will never execute
     * the source factory. That is, a lock will be acquired while calling the source factory.
     *
     * @param <T> the type of the cached value
     * @param valueFactory the factory creating the value. This argument cannot be {@code null}.
     * @return a factory caching the value returned by the given factory. This method
     *   never returns {@code null}.
     *
     * @see #lazyValue(Supplier)
     */
    public static <T> Supplier<T> lazyValueLocked(Supplier<? extends T> valueFactory) {
        return new LockedLazyValue<>(valueFactory);
    }

    @SuppressWarnings("unchecked")
    private static <T> T castLazyValue(Object obj) {
        return obj != NIL ? (T) obj : null;
    }

    private static Object wrapLazyValue(Object obj) {
        return obj != null ? obj : NIL;
    }

    /**
     * Defines an action to be executed after a lazy object creation.
     *
     * @param <T> the type of the objects to be created
     *
     * @see LazyValues#lazyNonNullValue(Supplier, PostCreateAction)
     */
    public interface PostCreateAction<T> {
        /**
         * Called right after the lazily constructed object got created. This method
         * is called even if the object will be discarded. You can use this action
         * for initialization and destruction as well.
         *
         * @param obj the lazily create object. This argument cannot be {@code null}.
         * @param accepted {@code true} if the object will be published, {@code false}
         *   if it will be discarded (and so it should destroyed)
         */
        public void apply(T obj, boolean accepted);
    }

    private static final class LazyNonNullValue<T> implements Supplier<T> {
        private volatile Supplier<? extends T> valueFactory;
        private final AtomicReference<T> valueRef;
        private final PostCreateAction<? super T> postCreateAction;

        public LazyNonNullValue(
                Supplier<? extends T> valueFactory,
                PostCreateAction<? super T> postCreateAction) {
            this.valueFactory = Objects.requireNonNull(valueFactory, "valueFactory");
            this.valueRef = new AtomicReference<>(null);
            this.postCreateAction = Objects.requireNonNull(postCreateAction, "postCreateAction");
        }

        @Override
        public T get() {
            T result = valueRef.get();
            if (result == null) {
                Supplier<? extends T> currentValueFactory = valueFactory;
                if (currentValueFactory == null) {
                    return valueRef.get();
                }

                result = currentValueFactory.get();
                if (!valueRef.compareAndSet(null, result)) {
                    postCreateAction.apply(result, false);
                    result = valueRef.get();
                } else if (result != null) {
                    valueFactory = null;
                    postCreateAction.apply(result, true);
                }
            }
            return result;
        }

        private String getCurrentValueStr() {
            T value = valueRef.get();
            return value == null ? UNKNOWN_VALUE_STR : value.toString();
        }

        @Override
        public String toString() {
            return "LazyValue{" + getCurrentValueStr() + '}';
        }
    }

    private static final class LazyValue<T> extends AbstractLazyValue<T> {
        private final AtomicReference<Object> valueRef;

        public LazyValue(Supplier<? extends T> valueFactory) {
            super(valueFactory);
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
                } else {
                    valueFactory = null;
                }
            }
            return castLazyValue(result);
        }

        @Override
        protected Object getCurrentValue() {
            return valueRef.get();
        }
    }

    private static final class LockedLazyValue<T> extends AbstractLazyValue<T> {
        private final ReentrantLock mainLock;
        private volatile Object cached;

        public LockedLazyValue(Supplier<? extends T> valueFactory) {
            super(valueFactory);
            this.mainLock = new ReentrantLock();
            this.cached = null;
        }

        @Override
        public T get() {
            Object result = cached;
            if (result == null) {
                mainLock.lock();
                try {
                    Supplier<? extends T> currentValueFactory = valueFactory;
                    if (currentValueFactory == null) {
                        return castLazyValue(cached);
                    }

                    result = wrapLazyValue(currentValueFactory.get());
                    cached = wrapLazyValue(result);
                    valueFactory = null;
                } finally {
                    mainLock.unlock();
                }
            }
            return castLazyValue(result);
        }

        @Override
        protected Object getCurrentValue() {
            return cached;
        }
    }

    private abstract static class AbstractLazyValue<T> implements Supplier<T> {
        protected volatile Supplier<? extends T> valueFactory;

        public AbstractLazyValue(Supplier<? extends T> valueFactory) {
            this.valueFactory = Objects.requireNonNull(valueFactory, "valueFactory");
        }

        protected abstract Object getCurrentValue();

        private String getCurrentValueStr() {
            Object value = getCurrentValue();
            if (value == null) {
                return UNKNOWN_VALUE_STR;
            }

            return value == NIL ? NULL_VALUE_STR : value.toString();
        }

        @Override
        public final String toString() {
            return "LazyValue{" + getCurrentValueStr() + '}';
        }
    }

    private LazyValues() {
        throw new AssertionError();
    }
}
