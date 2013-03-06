package org.jtrim.property;

import org.jtrim.event.ListenerRef;

/**
 *
 * @author Kelemen Attila
 */
public interface PropertySource<ValueType> {
    public ValueType getValue();
    public ListenerRef addChangeListener(Runnable listener);
}
