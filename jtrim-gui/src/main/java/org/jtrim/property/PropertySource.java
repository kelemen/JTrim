package org.jtrim.property;

import org.jtrim.event.ListenerRef;

/**
 * Defines the value of an arbitrary property. The value of this property might
 * change in an implementation dependent way. Some property might be changed by
 * client code, some might change due to external (and uncontrollable) events.
 * <P>
 * For example, the value of the property can be derived from the content of a
 * file and might get updated after the content of that file changes.
 * <P>
 * <B>Concurrency warning</B>: Although reading the properties is defined to be
 * "thread-safe", reading a single property concurrently is dangerous, unless
 * the two threads are completely independent of each other. For example,
 * consider the following scenario: Two threads read the same property and
 * they overwrite each other's result. See the following example code
 * <P>
 * <pre>
 * PropertySource&lt;Boolean&gt; property = ...;
 *
 *
 * Thread1:
 *   if (property.getValue()) {
 *     x = 1;
 *   }
 *
 * Thread2:
 *   if (!property.getValue()) {
 *     x = 2;
 *   }
 * </pre>
 * You might expect in the above code, that if {@code property.getValue()}
 * becomes and remains {@code true}, {@code x} will eventually be 1. However,
 * this is not the case because if the property was {@code false} before,
 * Thread2 might have already noticed it to be {@code false}, if after this the
 * property is quickly set to {@code true} and Thread1 also completes quickly,
 * Thread2 might continue after them setting {@code x} to 2.
 * <P>
 * To avoid the above problem, you may read the property value from a
 * {@link #addChangeListener(Runnable) change listener} because the listeners
 * may not be called concurrently, so reads won't be concurrent.
 *
 * <h3>Thread safety</h3>
 * Instances of this interface are required to be completely thread-safe
 * without any further synchronization.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are required to be
 * <I>synchronization transparent</I> and may be called from any context.
 *
 * @param <ValueType> the type of the value of the property
 *
 * @see PropertyFactory#constSource(Object) PropertyFactory.constSource
 * @see PropertyFactory#memProperty(Object,PropertyVerifier, PropertyPublisher) PropertyFactory.memProperty
 * @see PropertySourceProxy
 *
 * @author Kelemen Attila
 */
public interface PropertySource<ValueType> {
    /**
     * Returns the current value of this property. Implementations of this
     * method must not do any expensive computations or otherwise block. That
     * is, this method might be called from threads where responsiveness is
     * necessary, such as the <I>AWT Event Dispatch Thread</I>.
     * <P>
     * Implementation note:
     * Implementations are recommended to only return the value of a
     * {@code volatile} field (and possibly make defensive copy of the value).
     * If the property is read from some other sources then a separate thread
     * should read the value and update the volatile field.
     *
     * @return the current value of this property. This method may return
     *   {@code null} if the implementation allows {@code null} values for a
     *   property.
     */
    public ValueType getValue();

    /**
     * Registers a listener to be notified after the value of this property
     * changes. In what context the listener is called is implementation
     * dependent.
     * <P>
     * Once a listener is notified, it needs to get the current value of this
     * property by calling the {@link #getValue() getValue()} method. Note that,
     * it is allowed for implementations to notify the listener even if the
     * property does not change. Also, implementations may merge listener
     * notifications. That is, if a value is changed multiple times before it is
     * notified, implementations may decide to only notify the listener once.
     * <P>
     * Note however, that listeners are not allowed to be called concurrently.
     * That is, listeners registered to a particular {@code PropertySource}
     * are not allowed to run concurrently with each other. This maybe achieved
     * in several ways: For example, {@link MutableProperty} does not allow its
     * {@link MutableProperty#setValue(Object) setValue} method to be called
     * concurrently (in general) and if (as usual) it notified listeners in the
     * {@code setValue} method, the listeners won't be notified concurrently.
     * That is, if they area, then the client code is at fault for calling
     * {@code setValue} concurrently.
     *
     * @param listener the listener whose {@code run()} method is to be called
     *   whenever the value of this property changes. This argument cannot be
     *   {@code null}.
     * @return the {@code ListenerRef} which can be used to unregister the
     *   currently added listener, so that it will no longer be notified of
     *   subsequent changes. This method may never return {@code null}.
     */
    public ListenerRef addChangeListener(Runnable listener);
}
