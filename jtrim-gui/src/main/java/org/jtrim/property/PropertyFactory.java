package org.jtrim.property;

/**
 *
 * @author Kelemen Attila
 */
public final class PropertyFactory {
    public static <ValueType> MutableProperty<ValueType> memProperty(ValueType initialValue) {
        return memProperty(initialValue, false);
    }

    public static <ValueType> MutableProperty<ValueType> memProperty(
            ValueType initialValue,
            boolean allowNulls) {
        if (allowNulls) {
            return memProperty(initialValue,
                    PropertyFactory.<ValueType>noOpVerifier(),
                    PropertyFactory.<ValueType>noOpPublisher());
        }
        else {
            return memProperty(initialValue,
                    PropertyFactory.<ValueType>notNullVerifier(),
                    PropertyFactory.<ValueType>noOpPublisher());
        }
    }

    public static <ValueType> MutableProperty<ValueType> memProperty(
            ValueType initialValue,
            PropertyVerifier<ValueType> verifier) {
        return memProperty(initialValue, verifier, PropertyFactory.<ValueType>noOpPublisher());
    }

    public static <ValueType> MutableProperty<ValueType> memProperty(
            ValueType initialValue,
            PropertyVerifier<ValueType> verifier,
            PropertyPublisher<ValueType> publisher) {
        return new MemProperty<>(initialValue, verifier, publisher);
    }

    public static <ValueType> PropertySource<ValueType> constSource(ValueType value) {
        return constSource(value, PropertyFactory.<ValueType>noOpPublisher());
    }

    public static <ValueType> PropertySource<ValueType> protectedView(
            PropertySource<? extends ValueType> source) {
        return new DelegatedPropertySource<>(source);
    }

    public static <ValueType> PropertySource<ValueType> constSource(
            ValueType value,
            PropertyPublisher<ValueType> publisher) {
        return new ConstSource<>(value, publisher);
    }

    public static <ValueType> PropertyVerifier<ValueType> noOpVerifier() {
        return NoOpVerifier.getInstance();
    }

    public static <ValueType> PropertyVerifier<ValueType> notNullVerifier() {
        return NotNullVerifier.getInstance();
    }

    public static <ValueType> PropertyVerifier<ValueType> typeCheckerVerifier(
            Class<ValueType> expectedType) {
        return new TypeCheckerVerifier<>(expectedType);
    }

    public static <ValueType> PropertyPublisher<ValueType> noOpPublisher() {
        return NoOpPublisher.getInstance();
    }

    private PropertyFactory() {
        throw new AssertionError();
    }
}
