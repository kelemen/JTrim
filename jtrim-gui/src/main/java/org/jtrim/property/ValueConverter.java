package org.jtrim.property;

/**
 *
 * @author Kelemen Attila
 */
public interface ValueConverter<InputType, OutputType> {
    /***/
    public OutputType convert(InputType input);
}
