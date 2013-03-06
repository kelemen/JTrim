package org.jtrim.property;

/**
 *
 * @author Kelemen Attila
 */
public interface MutableProperty<ValueType> extends PropertySource<ValueType> {
    public void setValue(ValueType value);
}
