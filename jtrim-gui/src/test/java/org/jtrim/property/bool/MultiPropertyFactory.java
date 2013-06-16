package org.jtrim.property.bool;

import org.jtrim.property.PropertySource;

/**
 * @param <InputType> the type of the value of the properties from which to
 *   property is to be created
 * @param <OutputType> the type of the value of the property to be created
 *
 * @author Kelemen Attila
 */
public interface MultiPropertyFactory<InputType, OutputType> {
    public PropertySource<OutputType> create(
            PropertySource<InputType> property1,
            PropertySource<InputType> property2);
}
