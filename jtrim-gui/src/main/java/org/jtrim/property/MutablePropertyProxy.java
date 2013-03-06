package org.jtrim.property;

/**
 *
 * @author Kelemen Attila
 */
public interface MutablePropertyProxy<ValueType>
extends
        MutableProperty<ValueType> {

    public void replaceProperty(MutableProperty<ValueType> newProperty);
}
