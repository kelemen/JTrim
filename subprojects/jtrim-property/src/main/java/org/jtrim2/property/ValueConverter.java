package org.jtrim2.property;

/**
 * Defines an arbitrary conversion from one value to another.
 *
 * <h2>Thread safety</h2>
 * Instances of this interface are required to be completely thread-safe
 * without any further synchronization.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this interface are required to be
 * <I>synchronization transparent</I> and may be called from any context.
 *
 * @param <InputType> the type of the object to be converted
 * @param <OutputType> the type of the resulting object
 *
 * @see PropertyFactory#convert(PropertySource, ValueConverter)
 */
public interface ValueConverter<InputType, OutputType> {
    /**
     * Converts the input to the appropriate value defined by this conversion.
     *
     * @param input the input object of the conversion. This argument can be
     *   {@code null} if the implementation supports converting {@code null}
     *   value.
     * @return the result of the conversion. Depending on the implementation,
     *   this method may or may not return {@code null}.
     */
    public OutputType convert(InputType input);
}
