package org.jtrim.property;

/**
 * @see PropertyFactory#noOpPublisher()
 *
 * @author Kelemen Attila
 */
final class NoOpPublisher<ValueType> implements PropertyPublisher<ValueType> {
    private static final NoOpPublisher<Object> INSTANCE = new NoOpPublisher<>();

    @SuppressWarnings("unchecked")
    public static <ValueType> NoOpPublisher<ValueType> getInstance() {
        // This case is safe due to erasure and that returnValue returns the
        // same object as passed which of course should be type safe.
        return (NoOpPublisher<ValueType>)INSTANCE;
    }

    private NoOpPublisher() {
    }

    @Override
    public ValueType returnValue(ValueType value) {
        return value;
    }
}
