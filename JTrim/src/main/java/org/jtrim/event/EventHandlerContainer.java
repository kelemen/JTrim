package org.jtrim.event;

import java.util.Collection;

/**
 * Defines a collection of listeners and allows to notify all of them.
 *
 * @author Kelemen Attila
 */
public interface EventHandlerContainer<ListenerType, ArgType> {
    public ListenerRef<ListenerType> registerListener(ListenerType listener);
    public Collection<ListenerType> getListeners();
    public int getListenerCount();

    public void onEvent(
            EventDispatcher<? super ListenerType, ? super ArgType> eventDispatcher,
            ArgType arg);
}
