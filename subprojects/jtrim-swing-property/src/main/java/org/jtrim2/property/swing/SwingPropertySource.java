package org.jtrim2.property.swing;

/**
 * Defines the value of a usual property in Swing. The value of the property
 * may change and client code might be notified of this change via listeners.
 * Listener notification uses the {@code addListener}, {@code removeListener}
 * idiom.
 *
 * <h2>Thread safety</h2>
 * Adding and removing change listeners must be safe to be called from any
 * thread. However, it is not generally safe to read the value of the property
 * from any thread. In Swing, the usual constraint is that properties might only
 * be accessed from the Event Dispatch Thread.
 * <P>
 * Similar to {@link org.jtrim2.property.PropertySource}, listeners might not
 * be notified concurrently.
 * <P>
 * Regardless of from what thread the property might be accessed, all
 * implementations are required to allow reading the value of the property
 * from a listener. Therefore, if reading a property is allowed from only a
 * particular thread, the listeners must be invoked on that thread as well.
 *
 * <h3>Synchronization transparency</h3>
 * Methods of this interface should be <I>synchronization transparent</I>.
 * However, since (usually) there is no documented guarantee for synchronization
 * transparency in Swing properties, one must take care when accessing Swing
 * properties from dangerous contexts (e.g.: while holding a lock).
 *
 * @param <ValueType> the type of the value of the property
 * @param <ListenerType> the type of the listener which is notified whenever
 *   the value of the property changes
 *
 * @see SwingProperties#fromSwingSource(SwingPropertySource, SwingForwarderFactory) SwingProperties.fromSwingSource
 * @see SwingProperties#toSwingSource(PropertySource, EventDispatcher) SwingProperties.toSwingSource
 */
public interface SwingPropertySource<ValueType, ListenerType> {
    /**
     * Retrieves the current value of this property. Implementations of this
     * method should avoid doing any expensive computations or otherwise block.
     *
     * @return the current value of this property. The return value might be
     *   {@code null}, if the implementation allows {@code null} values.
     */
    public ValueType getValue();

    /**
     * Registers a listener to be notified after the value of this property
     * changes. In what context the listener is called is implementation
     * dependent but in Swing, it is usually required that listeners be notified
     * on the Event Dispatch Thread.
     * <P>
     * Once a listener is notified, it needs to get the current value of this
     * property by calling the {@link #getValue() getValue()} method. Note that,
     * it is allowed for implementations to notify the listener even if the
     * property does not change. Also, implementations may merge listener
     * notifications. That is, if a value is changed multiple times before it is
     * notified, implementations may decide to only notify the listener once.
     * <P>
     * Note however, that listeners are not allowed to be called concurrently.
     * That is, listeners registered to a particular property
     * are not allowed to run concurrently with each other.
     * <P>
     * Listeners might be unregistered by a subsequent call to
     * {@code removeChangeListener} with the listener added. Adding the same
     * listener multiple times will cause the listener to be notified as many
     * times as it has been added. Also, a listener must be removed as many
     * times as it has been added, in order to prevent more change notification.
     * What considered to be the "same" listener is implementation dependent.
     * Equivalence might be based on reference or the {@code equals} method.
     *
     * @param listener the listener which is to be notified whenever the value
     *   of this property changes. What method (and with what argument) is
     *   called is implementation dependent. However, most listener types should
     *   not have more than one method. This argument can be {@code null}, in
     *   which case, this method is defined to do nothing.
     */
    public void addChangeListener(ListenerType listener);

    /**
     * Removes a previously added listener, so that it is no longer to be
     * notified. Equivalence of listeners might be based on reference or the
     * {@code equals} method. If a listener has been added multiple times, it
     * must be removed as many times as it has been added.
     * <P>
     * Removing a listener only guarantees, that the listener will eventually
     * be stop receiving more notifications. There is no strong guarantee, that
     * removing a listener will prevent all subsequent listener notifications.
     * That is, for a short period of time, it is possible that the listener
     * will still be notified.
     *
     * @param listener the listener no longer needed to be notified. This
     *   argument can be {@code null}, in which case, this method is defined to
     *   do nothing.
     */
    public void removeChangeListener(ListenerType listener);
}
