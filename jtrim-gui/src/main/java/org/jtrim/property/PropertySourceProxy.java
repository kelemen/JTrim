package org.jtrim.property;

/**
 *
 * @author Kelemen Attila
 */
public interface PropertySourceProxy<ValueType>
extends
        PropertySource<ValueType> {

    public void replaceSource(PropertySource<? extends ValueType> newSource);
}
