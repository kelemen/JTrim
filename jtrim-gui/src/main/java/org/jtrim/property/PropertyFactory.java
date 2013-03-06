package org.jtrim.property;

/**
 *
 * @author Kelemen Attila
 */
public final class PropertyFactory {
    public static <ValueType> MutableProperty<ValueType> memProperty(
            ValueType initialValue,
            PropertyVerifier<ValueType> verifier,
            PropertyPublisher<ValueType> publisher) {
        return new MemProperty<>(initialValue, verifier, publisher);
    }

    public static <ValueType> PropertyVerifier<ValueType> noOpVerifier() {
        return NoOpVerifier.getInstance();
    }

    public static <ValueType> PropertyPublisher<ValueType> noOpPublisher() {
        return NoOpPublisher.getInstance();
    }

    private PropertyFactory() {
        throw new AssertionError();
    }
}
