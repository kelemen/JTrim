package org.jtrim.property;

/**
 *
 * @author Kelemen Attila
 */
public interface PropertyPublisher<ValueType> {
    public ValueType returnValue(ValueType value);
}
