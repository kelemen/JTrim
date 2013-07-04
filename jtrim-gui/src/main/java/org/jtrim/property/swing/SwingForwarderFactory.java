package org.jtrim.property.swing;

/**
 *
 * @author Kelemen Attila
 */
public interface SwingForwarderFactory<ListenerType> {
    /***/
    public ListenerType createForwarder(Runnable listener);
}
