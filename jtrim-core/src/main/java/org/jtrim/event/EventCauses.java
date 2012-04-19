package org.jtrim.event;

/**
 *
 * @author Kelemen Attila
 */
public interface EventCauses {
    public int getNumberOfCauses();

    public Iterable<TriggeredEvent<?, ?>> getCauses();
    public Iterable<Object> getArgumentsOfKind(Object eventKind);

    public boolean isCausedByKind(Object eventKind);
    public boolean isCausedByEvent(TriggeredEvent<?, ?> event);
}
