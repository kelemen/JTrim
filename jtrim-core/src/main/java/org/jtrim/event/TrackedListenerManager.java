package org.jtrim.event;

/**
 *
 * @author Kelemen Attila
 */
public interface TrackedListenerManager<EventKindType, ArgType>
extends
        ListenerRegistry<TrackedEventListener<EventKindType, ArgType>> {

    public void onEvent(ArgType arg);
}
