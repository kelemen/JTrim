package org.jtrim.event;

/**
 *
 * @author Kelemen Attila
 */
public interface TrackedListenerManager<ArgType>
extends
        ListenerRegistry<TrackedEventListener<ArgType>> {

    public void onEvent(ArgType arg);
}
