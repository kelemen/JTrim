package org.jtrim.property.swing;

import org.jtrim.event.EventDispatcher;
import org.jtrim.property.PropertySource;

/**
 * Contains static factory methods for conversion between <I>Swing</I>
 * properties and properties of <I>JTrim</I>.
 * <P>
 * <B>Warning</B>: There is a notable difference between <I>Swing</I> and
 * <I>JTrim</I> properties: <I>Swing</I> properties cannot be read from any
 * thread. They usually only allowed to be read from the Event Dispatch Thread.
 * This must be considered when using a property backed by a <I>Swing</I>
 * property.
 *
 * @author Kelemen Attila
 */
public final class SwingProperties {
    /**
     * Returns a {@code PropertySource} backed by a <I>Swing</I> property.
     * The returned property will completely rely on the specified
     * {@code SwingPropertySource}. That is, when its value is read, it will
     * request the {@code SwingPropertySource} to retrieve the value and it will
     * rely on its change listener to be notified of changes in the value of
     * the property.
     * <P>
     * <B>WARNING</B>: Although {@code PropertySource} requires that the
     * {@code getValue()} can be safely accessed from any thread, this is not
     * true for a {@code SwingPropertySource}. Therefore, you must always
     * consider that the {@code getValue()} method of the returned
     * {@code PropertySource} is called from an appropriate context (which is
     * usually the Event Dispatch Thread in Swing).
     *
     * @param <ValueType> the type of the value of the returned property
     * @param <ListenerType> the type of the listener used by the Swing
     *   property to notify client code of the changes in the value of the
     *   property
     * @param property the property backing the returned property. This
     *   argument cannot be {@code null}.
     * @param listenerForwarder the {@code SwingForwarderFactory} which creates
     *   a listener which might be registered with the backing property. The
     *   listener it creates must forward change notifications received from
     *   the {@code SwingPropertySource} to the {@code Runnable} passed to the
     *   {@code SwingForwarderFactory}. This argument cannot be {@code null}.
     * @return the {@code PropertySource} backed by a <I>Swing</I> property.
     *   This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <ValueType, ListenerType> PropertySource<ValueType> fromSwingSource(
            final SwingPropertySource<? extends ValueType, ? super ListenerType> property,
            final SwingForwarderFactory<? extends ListenerType> listenerForwarder) {

        return new SwingBasedPropertySource<>(property, listenerForwarder);
    }

    /**
     * Returns a {@code SwingPropertySource} backed by a property of
     * <I>JTrim</I>. That is, when its value is read, it will
     * request the {@code PropertySource} to retrieve the value and it will
     * rely on its change listener to be notified of changes in the value of
     * the property.
     * <P>
     * Note that the returned {@code SwingPropertySource} will inherit the
     * thread-safety property of the specified {@code PropertySource}. That is,
     * this means that the returned property can be accessed from any thread
     * (unless, there are some implementation restrictions of the passed
     * {@code PropertySource}).
     *
     * @param <ValueType> the type of the value of the returned property
     * @param <ListenerType> the type of the listener used by the Swing
     *   property to notify client code of the changes in the value of the
     *   property
     * @param property the property backing the returned property. This
     *   argument cannot be {@code null}.
     * @param eventDispatcher the {@code EventDispatcher} forwarding value
     *   change events to the listener of the {@code SwingPropertySource}. This
     *   argument cannot be {@code null}.
     * @return the {@code SwingPropertySource} backed by a property of
     *   <I>JTrim</I>. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public static <ValueType, ListenerType> SwingPropertySource<ValueType, ListenerType> toSwingSource(
            PropertySource<? extends ValueType> property,
            EventDispatcher<? super ListenerType, Void> eventDispatcher) {

        return new StandardBasedSwingPropertySource<>(property, eventDispatcher);
    }

    private SwingProperties() {
        throw new AssertionError();
    }
}
