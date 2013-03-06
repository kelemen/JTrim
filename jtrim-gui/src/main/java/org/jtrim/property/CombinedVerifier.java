package org.jtrim.property;

import java.util.ArrayList;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;

/**
 * @see PropertyFactory#combinedVerifier(List)
 *
 * @author Kelemen Attila
 */
final class CombinedVerifier<ValueType> implements PropertyVerifier<ValueType> {
    private final List<? extends PropertyVerifier<ValueType>> verifiers;

    public CombinedVerifier(List<? extends PropertyVerifier<ValueType>> verifiers) {
        this.verifiers = new ArrayList<>(verifiers);

        ExceptionHelper.checkNotNullElements(this.verifiers, "verifiers");
    }

    @Override
    public ValueType storeValue(ValueType value) {
        ValueType result = value;
        for (PropertyVerifier<ValueType> verifier: verifiers) {
            value = verifier.storeValue(value);
        }
        return result;
    }
}
