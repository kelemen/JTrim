package org.jtrim.property;

import org.jtrim.utils.ExceptionHelper;

/**
 * @see PropertyFactory#typeCheckerVerifier(Class)
 *
 * @author Kelemen Attila
 */
final class TypeCheckerVerifier<ValueType> implements PropertyVerifier<ValueType> {
    private final Class<ValueType> expectedType;

    public TypeCheckerVerifier(Class<ValueType> expectedType) {
        ExceptionHelper.checkNotNullArgument(expectedType, "expectedType");
        this.expectedType = expectedType;
    }

    @Override
    public ValueType storeValue(ValueType value) {
        if (value == null) {
            return null;
        }

        Class<?> valueClass = value.getClass();
        if (!expectedType.isAssignableFrom(valueClass)) {
            throw new IllegalArgumentException("The passed value has an incorrect type. "
                    + "Expected: " + expectedType.getName() + " but received: "
                    + valueClass.getName());
        }
        return value;
    }

}
