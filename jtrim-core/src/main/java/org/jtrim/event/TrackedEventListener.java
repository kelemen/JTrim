package org.jtrim.event;

/**
 *
 * @author Kelemen Attila
 */
public interface TrackedEventListener<EventKindType, ArgType> {
    public void onEvent(TrackedEvent<EventKindType, ArgType> trackedEvent);
}
