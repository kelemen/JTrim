package org.jtrim2.property;

import java.util.ArrayList;
import java.util.List;
import org.jtrim2.utils.ExceptionHelper;

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
            result = verifier.storeValue(result);
        }
        return result;
    }
}
