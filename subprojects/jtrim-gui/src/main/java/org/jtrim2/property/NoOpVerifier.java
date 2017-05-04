package org.jtrim2.property;

/**
 * @see PropertyFactory#noOpVerifier()
 *
 * @author Kelemen Attila
 */
final class NoOpVerifier<ValueType> implements PropertyVerifier<ValueType> {
    private static final NoOpVerifier<Object> INSTANCE = new NoOpVerifier<>();

    @SuppressWarnings("unchecked")
    public static <ValueType> NoOpVerifier<ValueType> getInstance() {
        // This case is safe due to erasure and that storeValue returns the
        // same object as passed which of course should be type safe.
        return (NoOpVerifier<ValueType>)INSTANCE;
    }

    private NoOpVerifier() {
    }

    @Override
    public ValueType storeValue(ValueType value) {
        return value;
    }
}
