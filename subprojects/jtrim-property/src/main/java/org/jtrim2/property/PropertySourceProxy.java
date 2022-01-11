package org.jtrim2.property;

/**
 * Defines a {@code PropertySource} which is backed by another
 * {@code PropertySource}. The backing {@code PropertySource} can be replaced
 * by a call to the {@link #replaceSource(PropertySource) replaceSource}
 * method. The value of the {@code PropertySourceProxy} always depends on the
 * actual backing {@code PropertySource}.
 * <P>
 * The implementation takes care of properly registering listeners with the
 * backing {@code PropertySource} and notify listeners registered with the
 * {@code PropertySourceProxy}.
 * <P>
 * <B>Warning</B>: Adding a listener to the {@code PropertySourceProxy}
 * may retain a reference to the listener even if the listener is
 * automatically unregistered by the backing {@code PropertySource}. Therefore
 * you cannot rely on automatic unregistering. The only exception from this
 * rule is when the backing {@code PropertySource} unregisters the listeners
 * immediately. In this latter case, implementations of
 * {@code PropertySourceProxy} are required to detect this and not to retain
 * references to the listener.
 *
 * <h2>Thread safety</h2>
 * The {@link #replaceSource(PropertySource) replaceSource} method of this
 * interface are not required to be thread-safe. That is, it must not be called
 * concurrently with other method calls of this interface unless they are
 * defined to be thread-safe. Other methods derived from {@code PropertySource}
 * retain their thread-safety property.
 * <P>
 * <B>Recommendation</B>: When using the property for Swing components, it is
 * recommended to only call the {@code replaceSource} from the <I>AWT Event
 * Dispatch Thread</I> because this is consitent with other property usages in
 * Swing.
 *
 * <h3>Synchronization transparency</h3>
 * The {@link #replaceSource(PropertySource) replaceSource} method of this
 * interface is not required to be <I>synchronization transparent</I>.
 *
 * @param <ValueType> the type of the value of the {@code PropertySource}
 *
 * @see PropertyFactory#proxySource(PropertySource)
 */
public interface PropertySourceProxy<ValueType>
extends
        PropertySource<ValueType> {

    /**
     * Changes the backing {@code PropertySource} to the newly specified one.
     * That is, getting the value of this {@code PropertySourceProxy} will
     * return the actual value of the newly set backing {@code PropertySource}
     * after this method returns.
     * <P>
     * Subsequent changes to the previous backing {@code PropertySource} will
     * not affect this {@code PropertySourceProxy} after this method returns.
     * <P>
     * Note: This method causes the
     * {@link #addChangeListener(Runnable) property change listeners} to be
     * notified and in most implementation this causes the listeners to be
     * called synchronously in this method.
     *
     * @param newSource the new backing {@code PropertySource}. This argument
     * cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     * {@code PropertySource} is {@code null}
     */
    public void replaceSource(PropertySource<? extends ValueType> newSource);
}
