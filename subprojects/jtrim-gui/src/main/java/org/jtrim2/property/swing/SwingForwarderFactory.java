package org.jtrim2.property.swing;

/**
 * Creates listeners invoking the {@code run()} method of a {@code Runnable}.
 * This interface is intended to bridge the gap between <I>Swing</I> properties
 * and properties in <I>JTrim</I>. That is, this interface is used when
 * converting a <I>Swing</I> property to a property of <I>JTrim</I>, as done by
 * {@link SwingProperties#fromSwingSource(SwingPropertySource, SwingForwarderFactory) SwingProperties.fromSwingSource}.
 *
 * <h3>Thread safety</h3>
 * This interface is required to be thread-safe. That is, its method is allowed
 * to be called from multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The method of this interface is required to be
 * <I>synchronization transparent</I>.
 *
 * @param <ListenerType> the type of the listener to be created
 *
 * @see SwingProperties#fromSwingSource(SwingPropertySource, SwingForwarderFactory) SwingProperties.fromSwingSource
 */
public interface SwingForwarderFactory<ListenerType> {
    /**
     * Creates and returns a new listener which will call the {@code run()}
     * method of the specified {@code Runnable} when notified.
     *
     * @param listener the {@code Runnable} whose {@code run()} method is to
     *   be called whenever the returned listener is notified of an event this
     *   {@code SwingForwarderFactory} is associated with.
     * @return a new listener which will call the {@code run()} method of the
     *   specified {@code Runnable} when notified. This method may never return
     *   {@code null} and must return a new instance for each invocation.
     */
    public ListenerType createForwarder(Runnable listener);
}
