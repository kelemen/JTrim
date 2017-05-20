package org.jtrim2.testutils.event;

import org.jtrim2.event.ListenerManager;

public interface TestManagerFactory {
    public <ListenerType> ListenerManager<ListenerType> createEmpty(
            Class<ListenerType> listenerClass, Class<?> argClass);
}
