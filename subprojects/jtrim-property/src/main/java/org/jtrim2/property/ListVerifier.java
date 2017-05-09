package org.jtrim2.property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @see PropertyFactory#listVerifier(PropertyVerifier, boolean)
 */
final class ListVerifier<ElementType> implements PropertyVerifier<List<ElementType>> {
    private final PropertyVerifier<ElementType> elementVerifier;
    private final boolean allowNullList;

    public ListVerifier(PropertyVerifier<ElementType> elementVerifier, boolean allowNullList) {
        Objects.requireNonNull(elementVerifier, "elementVerifier");

        this.elementVerifier = elementVerifier;
        this.allowNullList = allowNullList;
    }

    private List<ElementType> storeListNotNull(List<ElementType> value) {
        Objects.requireNonNull(value, "value");

        List<ElementType> result = new ArrayList<>(value.size());
        for (ElementType element: value) {
            result.add(elementVerifier.storeValue(element));
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public List<ElementType> storeValue(List<ElementType> value) {
        if (value == null && allowNullList) {
            return null;
        }

        return storeListNotNull(value);
    }
}
