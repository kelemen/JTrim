package org.jtrim.event;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author Kelemen Attila
 */
public interface EventTracker<EventKindType> {
    public <ArgType> TrackedListenerManager<EventKindType, ArgType> getContainerOfType(
            EventKindType eventKind,
            Class<ArgType> argType);

    public Executor createTrackedExecutor(Executor executor);
    public ExecutorService createTrackedExecutorService(ExecutorService executor);
}
