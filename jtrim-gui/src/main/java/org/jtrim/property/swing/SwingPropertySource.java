package org.jtrim.property.swing;

/**
 * @see SwingProperties#fromSwingSource(SwingPropertySource, SwingForwarderFactory)
 * @see SwingProperties#toSwingSource(PropertySource, EventDispatcher)
 *
 * @author Kelemen Attila
 */
public interface SwingPropertySource<ValueType, ListenerType> {
    /***/
    public ValueType getValue();

    /***/
    public void addChangeListener(ListenerType listener);

    /***/
    public void removeChangeListener(ListenerType listener);
}
