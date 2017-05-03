package org.jtrim2.property;

/**
 * Defines a method which defensively copies objects before being stored in an
 * internal field and also verifies if the given value is valid. That is, this
 * interface is intended to be used in situations like when a value to be stored
 * is mutable and must be copied, so that clients might not alter the internal
 * value; and may also be used throw an exception if a value is not allowed
 * (e.g.: it is {@code null}).
 * <P>
 * This interface was designed to be used with a {@link MutableProperty} which
 * might use this interface before storing a value by its
 * {@link MutableProperty#setValue(Object) setValue} method.
 *
 * <h3>Thread safety</h3>
 * Instances of this interface are required to be completely thread-safe
 * without any further synchronization.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are required to be
 * <I>synchronization transparent</I> and may be called from any context.
 *
 * @param <ValueType> the type of the value to be published
 *
 * @see PropertyFactory
 * @see PropertyFactory#memProperty(Object, PropertyVerifier, PropertyPublisher) PropertyFactory.memProperty
 * @see PropertyFactory#noOpVerifier()
 * @see PropertyFactory#notNullVerifier()
 *
 * @author Kelemen Attila
 */
public interface PropertyVerifier<ValueType> {
    /**
     * Creates the value to be stored from the passed value. This method might
     * make a defensive copy of the passed value or simply return the passed
     * value if it is immutable or cannot be abused.
     *
     * @param value the value on which the result of the method is based. What
     *   is considered a valid value is completely implementation dependent.
     * @return the value to be returned to the client code. This is usually the
     *   defensive copy of the value passed in the argument. This method may or
     *   may not return {@code null} depending on the implementation.
     *
     * @throws NullPointerException expected to be thrown if the passed value is
     *   {@code null} but {@code null} values are not permitted
     * @throws IllegalArgumentException expected to be thrown if the passed
     *   value is invalid for some reason
     */
    public ValueType storeValue(ValueType value);
}
