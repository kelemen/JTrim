package org.jtrim.property;

/**
 *
 * @author Kelemen Attila
 */
public interface PropertyVerifier<ValueType> {
    public ValueType storeValue(ValueType value);
}
