package org.jtrim.property;

import java.util.Arrays;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

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

    public static <ValueType> MutablePropertyProxy<ValueType> proxyProperty(
            MutableProperty<ValueType> initialProperty) {
        return new DefaultMutablePropertyProxy<>(initialProperty);
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

    public static <ValueType> PropertySourceProxy<ValueType> proxySource(
            PropertySource<? extends ValueType> initialSource) {
        return new DefaultPropertySourceProxy<>(initialSource);
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

    public static <ValueType> PropertyVerifier<ValueType> combinedVerifier(
            PropertyVerifier<ValueType> verifier1,
            PropertyVerifier<ValueType> verifier2) {
        return combinedVerifier(Arrays.asList(verifier1, verifier2));
    }

    public static <ValueType> PropertyVerifier<ValueType> combinedVerifier(
            List<? extends PropertyVerifier<ValueType>> verifiers) {

        switch (verifiers.size()) {
            case 0:
                return noOpVerifier();
            case 1: {
                PropertyVerifier<ValueType> result = verifiers.get(0);
                ExceptionHelper.checkNotNullArgument(result, "verifiers[0]");
                return result;
            }
            default:
                return new CombinedVerifier<>(verifiers);
        }
    }

    public static <ValueType> PropertyVerifier<List<ValueType>> listVerifier(
            PropertyVerifier<ValueType> elementVerifier,
            boolean allowNullList) {
        return new ListVerifier<>(elementVerifier, allowNullList);
    }

    public static <ValueType> PropertyPublisher<ValueType> noOpPublisher() {
        return NoOpPublisher.getInstance();
    }

    private PropertyFactory() {
        throw new AssertionError();
    }
}
