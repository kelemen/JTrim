package org.jtrim.event;

/**
 *
 * @author Kelemen Attila
 */
public interface TrackedEventListener<ArgType> {
    public void onEvent(TrackedEvent<ArgType> trackedEvent);
}
