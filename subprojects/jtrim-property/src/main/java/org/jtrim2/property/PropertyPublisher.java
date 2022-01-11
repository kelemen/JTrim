package org.jtrim2.property;

/**
 * Defines a method which protects a value when that value is being returned.
 * That is, this interface is intended to be used in situations like when a
 * value to be returned is mutable and must be copied, so that clients might not
 * alter the internal value.
 * <P>
 * This interface was designed to be used with a {@link PropertySource} which
 * might use this interface before returning the property by its
 * {@link PropertySource#getValue() getValue} method.
 *
 * <h2>Thread safety</h2>
 * Instances of this interface are required to be completely thread-safe
 * without any further synchronization.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this interface are required to be
 * <I>synchronization transparent</I> and may be called from any context.
 *
 * @param <ValueType> the type of the value to be published
 *
 * @see PropertyFactory
 * @see PropertyFactory#constSource(Object, PropertyPublisher) PropertyFactory.constSource
 * @see PropertyFactory#memProperty(Object, PropertyVerifier, PropertyPublisher) PropertyFactory.memProperty
 * @see PropertyFactory#noOpPublisher()
 */
public interface PropertyPublisher<ValueType> {
    /**
     * Creates the value to be returned from the passed value. This method might
     * make a defensive copy of the passed value or simply return the passed
     * value if it is immutable or cannot be abused.
     *
     * @param value the value on which the result of the method is based. This
     *   argument may or may not be {@code null} depending on the
     *   implementation.
     * @return the value to be returned to the client code. This is usually the
     *   defensive copy of the value passed in the argument. This method may or
     *   may not return {@code null} depending on the implementation.
     */
    public ValueType returnValue(ValueType value);
}
