package org.jtrim.property;

import org.jtrim.utils.ExceptionHelper;

/**
 * @see PropertyFactory#notNullVerifier()
 *
 * @author Kelemen Attila
 */
final class NotNullVerifier<ValueType>
implements
        PropertyVerifier<ValueType> {
    private static final NotNullVerifier<Object> INSTANCE = new NotNullVerifier<>();

    @SuppressWarnings("unchecked")
    public static <ValueType> NotNullVerifier<ValueType> getInstance() {
        // This case is safe due to erasure and that storeValue returns the
        // same object as passed which of course should be type safe.
        return (NotNullVerifier<ValueType>)INSTANCE;
    }

    private NotNullVerifier() {
    }

    @Override
    public ValueType storeValue(ValueType value) {
        ExceptionHelper.checkNotNullArgument(value, "value");
        return value;
    }
}
