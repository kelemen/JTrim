package org.jtrim.event;

/**
 *
 * @author Kelemen Attila
 */
public interface EventCauses<EventKindType> {
    public int getNumberOfCauses();

    public Iterable<TriggeredEvent<EventKindType, ?>> getCauses();
    public Iterable<Object> getArgumentsOfKind(EventKindType eventKind);

    public boolean isCausedByKind(Object eventKind);
    public boolean isCausedByEvent(TriggeredEvent<? extends EventKindType, ?> event);
}
