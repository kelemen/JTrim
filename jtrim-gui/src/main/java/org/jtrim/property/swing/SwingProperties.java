package org.jtrim.property.swing;

import java.awt.Component;
import javax.swing.AbstractButton;
import javax.swing.text.Document;
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

    /**
     * Defines a property which tracks a bound property of a
     * {@code java.awt.Component}. Changes in the value of the property are
     * expected to be detectable via a listener added with
     * {@code Component.addPropertyChangeListener}. For example, the background
     * property of a component is such property. Note however, that the
     * {@code "text"} property of a {@code JTextField} is not.
     * <P>
     * The specified component is expected to have getter method for the
     * specified property. The getter method must follow the usual naming
     * convention: Must not have any argument, must start with "get" followed
     * by the property name whose first character is capitalized. For example,
     * if the property is "background", then the component must have a
     * {@code getBackground()} method.
     * <P>
     * Note that this method does not support boolean properties whose getter
     * methods start with the "is" prefix, only properties whose getter methods
     * start with the "get" prefix.
     * <P>
     * <B>WARNING</B>: Although {@code PropertySource} requires that the
     * {@code getValue()} can be safely accessed from any thread, this is not
     * true for the returned {@code PropertySource}. Therefore, in general, you
     * may only call the {@code getValue()} method of the returned
     * {@code PropertySource} from the Event Dispatch Thread (as required in the
     * majority of cases in Swing).
     *
     * @param <ValueType> the type of the value of the returned property
     * @param component the component whose property is to be returned in the
     *   standard format. This argument cannot be {@code null}.
     * @param propertyName the name of the property as it can be specified for
     *   the {@code PropertyChangeListener}. This argument cannot be
     *   {@code null}.
     * @param valueType the type of the value of the property. The getter method
     *   must have a return type which can be cast to this type. This argument
     *   cannot be {@code null}.
     * @return a property which tracks a bound property of a
     *   {@code java.awt.Component}. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws IllegalArgumentException throw if the specified property name is
     *   not valid because there is no appropriate getter method for it. This
     *   might be due to having a wrong return type or simply because the getter
     *   method does not exist.
     */
    public static <ValueType> PropertySource<ValueType> componentPropertySource(
            Component component,
            String propertyName,
            Class<ValueType> valueType) {
        return ComponentPropertySource.createProperty(component, propertyName, valueType);
    }

    /**
     * Defines a property which tracks the value of the {@code text} property
     * ({@code getText}) of the specified {@code Document}.
     * <P>
     * Note that this method might be used with {@code JTextComponent}
     * implementations as well (such as {@code JTextField}).
     * <P>
     * Although the {@code text} property of {@code Document} is not required
     * to be set on the Event Dispatch Thread, the listeners registered with
     * the returned {@code PropertySource} will always be called on the
     * Event Dispatch Thread.
     * <P>
     * <B>Warning</B>: You are not allowed to change the {@code text} property
     * of the {@code Document} in the listeners. Adjusting the text property may
     * or may not work and may even cause an unchecked exception to be thrown.
     *
     * @param document the {@code Document} whose text property is to be
     *   tracked. This argument cannot be {@code null}.
     * @return  a property which tracks the value of the {@code text} property
     *   of the specified {@code Document}. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified {@code Document} is
     *   {@code null}
     */
    public static PropertySource<String> documentTextSource(Document document) {
        return DocumentTextProperty.createProperty(document);
    }

    /***/
    public static PropertySource<Boolean> buttonSelectedSource(AbstractButton button) {
        return ButtonSelectedPropertySource.createProperty(button);
    }

    private SwingProperties() {
        throw new AssertionError();
    }
}
