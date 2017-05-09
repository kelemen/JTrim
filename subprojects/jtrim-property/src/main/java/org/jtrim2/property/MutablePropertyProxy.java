package org.jtrim2.property;

/**
 * Defines a {@code MutableProperty} which is backed by another
 * {@code MutableProperty}. The backing {@code MutableProperty} can be replaced
 * by a call to the {@link #replaceProperty(MutableProperty) replaceProperty}
 * method. The value of the {@code MutablePropertyProxy} always depends on the
 * actual backing {@code MutableProperty}.
 * <P>
 * The implementation takes care of properly registering listeners with the
 * backing {@code MutableProperty} and notify listeners registered with the
 * {@code MutablePropertyProxy}.
 * <P>
 * <B>Warning</B>: Adding a listener to the {@code MutablePropertyProxy}
 * may retain a reference to the listener even if the listener is
 * automatically unregistered by the backing {@code MutableProperty}. Therefore
 * you cannot rely on automatic unregistering. The only exception from this
 * rule is when the backing {@code MutableProperty} unregisters the listeners
 * immediately. In this latter case, implementations of
 * {@code MutablePropertyProxy} are required to detect this and not to retain
 * references to the listener.
 *
 * <h3>Thread safety</h3>
 * The {@link #replaceProperty(MutableProperty) replaceProperty} method of this
 * interface are not required to be thread-safe. That is, it must not be called
 * concurrently with other method calls of this interface unless they are
 * defined to be thread-safe. Other methods derived from {@code MutableProperty}
 * retain their thread-safety property.
 * <P>
 * <B>Recommendation</B>: When using the property for Swing components, it is
 * recommended to only call the {@code replaceProperty} from the <I>AWT Event
 * Dispatch Thread</I> because this is consitent with other property usages in
 * Swing.
 *
 * <h4>Synchronization transparency</h4>
 * The {@link #replaceProperty(MutableProperty) replaceProperty} method of this
 * interface is not required to be <I>synchronization transparent</I>.
 *
 * @param <ValueType> the type of the value of the {@code MutableProperty}
 *
 * @see PropertyFactory#proxyProperty(MutableProperty)
 */
public interface MutablePropertyProxy<ValueType>
extends
        MutableProperty<ValueType> {

    /**
     * Changes the backing {@code MutableProperty} to the newly specified one.
     * That is, setting a value for this {@code MutablePropertyProxy} by the
     * {@link #setValue(Object) setValue} method will set the value of the
     * backing {@code MutableProperty}.
     * <P>
     * Subsequent changes to the previous backing {@code MutableProperty} will
     * not affect this {@code MutablePropertyProxy} after this method returns.
     * <P>
     * Note: This method causes the
     * {@link #addChangeListener(Runnable) property change listeners} to be
     * notified and in most implementation this causes the listeners to be
     * called synchronously in this method (as with {@code setValue}).
     *
     * @param newProperty the new backing {@code MutableProperty}. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code MutableProperty} is {@code null}
     */
    public void replaceProperty(MutableProperty<ValueType> newProperty);
}
