package org.jtrim2.property;

/**
 * Defines a {@code PropertySource} whose value can be changed by the client
 * code by calling the {@link #setValue(Object) setValue} method.
 *
 * <h3>Thread safety</h3>
 * The {@link #setValue(Object) setValue} method does not need to be
 * thread-safe and must not be called concurrently with another {@code setValue}
 * call of the same {@code MutableProperty}. Methods inherited from
 * {@code PropertySource} however, may be called concurrently by multiple
 * threads. They may even be called concurrently with the {@code setValue}
 * method.
 * <P>
 * <B>Recommendation</B>: When using the property for Swing components, it is
 * recommended to only call the {@code setValue} from the <I>AWT Event
 * Dispatch Thread</I> because this is consitent with other property usages in
 * Swing.
 *
 * <h4>Synchronization transparency</h4>
 * The {@code setValue} method does not need to be
 * <I>synchronization transparent</I> but methods inherited from
 * {@code PropertySource} must honor the contract of {@code PropertySource}.
 * That is, only methods inherited from {@code PropertySource} required to be
 * <I>synchronization transparent</I>.
 *
 * @param <ValueType> the type of the value of the property
 *
 * @see PropertyFactory#memProperty(Object,PropertyVerifier, PropertyPublisher) PropertyFactory.memProperty
 * @see MutablePropertyProxy
 */
public interface MutableProperty<ValueType> extends PropertySource<ValueType> {
    /**
     * Sets the value of this property to the given value, so that subsequent
     * {@link #getValue() getValue} calls will return the currently set value.
     * <P>
     * This method will cause the currently registered
     * {@link #addChangeListener(Runnable) change listeners} to be notified
     * unless the property is set to its current value (not changed). Note
     * however, that implementations are not required to detect that the value
     * is not changed (set to the current value) and may notify the change
     * listeners anyway.
     * <P>
     * Implementations might decide to notify the listeners synchronously in
     * this {@code setValue} call but it is generally implementation dependent
     * when and from what context the listeners are notified.
     *
     * @param value the new value of this property. Implementations may put
     *   some constraint on the allowed value of this property.
     *
     * @throws NullPointerException thrown if the implementation does not allow
     *   {@code null} value for this property and the specified value is
     *   {@code null}
     * @throws IllegalArgumentException thrown if the implementation does not
     *   allow the property to be set for the specified value
     */
    public void setValue(ValueType value);
}
