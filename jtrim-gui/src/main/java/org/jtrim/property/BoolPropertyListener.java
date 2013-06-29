package org.jtrim.property;

/**
 * Defines a listener interface to be notified whenever a boolean property
 * changes its value. This listener also receives the new value to which the
 * property has changed.
 * <P>
 * The listener is not required to be notified of every change but if a value
 * is changed to a particular value and is never changed again, then the
 * listener must eventually be notified of the last change. Also, it is required
 * that if the value of the associated property changes, the listener is
 * notified eventually, even if the value changes rapidly forever after.
 *
 * <h3>Thread safety</h3>
 * Instances of this interface are not required to be safely accessed by
 * multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The {@code onChangeValue} method of this interface are not required to be
 * <I>synchronization transparent</I> but must return reasonably quickly.
 *
 * @see BoolProperties#addBoolPropertyListener(PropertySource, BoolPropertyListener)
 *
 * @author Kelemen Attila
 */
public interface BoolPropertyListener {
    /**
     * Called whenever the value of the associated property changes. This
     * method does not need to be notified of every change except for the last
     * change. That is, if the property changes its value too fast, it is
     * allowed to skip some value change notification.
     *
     * @param newValue the value to which the associated property has been
     *   changed to
     */
    public void onChangeValue(boolean newValue);
}
