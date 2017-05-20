package org.jtrim2.event.track;

public interface TrackedManagerFactory {
    public <ArgType> TrackedListenerManager<ArgType> createEmpty(Class<ArgType> argClass);
}
