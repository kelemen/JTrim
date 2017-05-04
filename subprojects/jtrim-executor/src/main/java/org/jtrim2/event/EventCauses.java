package org.jtrim2.event;

/**
 * Defines the chain of events leading to a specific part of a code in the
 * execution.
 * <P>
 * How the causing events are tracked is in general implementation dependant.
 * However, this interface was designed for {@link EventTracker}
 * implementations which can keep track of events and provide the causes of
 * events.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface must be immutable and as such safe to be
 * accessed by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Implementations of this interface are required to be
 * <I>synchronization transparent</I>.
 *
 * @see AbstractEventCauses
 * @see EventTracker
 * @see TriggeredEvent
 *
 * @author Kelemen Attila
 */
public interface EventCauses {
    /**
     * Returns the number of causing events defined by this {@code EventCauses}.
     * <P>
     * This method returns the same number as the number of events the
     * {@code Iterable} returned by the {@link #getCauses() getCauses()} method
     * contains.
     *
     * @return the number of causing events defined by this {@code EventCauses}.
     *   This method always returns an integer greater than or equal to zero.
     */
    public int getNumberOfCauses();

    /**
     * Returns the causing events, iterated in backward order as they have
     * occurred. That is, the first event returned by this {@code Iterable} is
     * the immediate cause and the last one is the root cause.
     * <P>
     * The {@code Iterable} returned by this method will iterate over as many
     * elements as returned by the {@link #getNumberOfCauses()} method.
     * <P>
     * None of the elements of the returned {@code Iterable} are {@code null}
     * values.
     *
     * @return the causing events, iterated in backward order as they have
     *   occurred. This method never returns {@code null}.
     */
    public Iterable<TriggeredEvent<?>> getCauses();

    /**
     * Returns the {@link TriggeredEvent#getEventArg() event arguments} of
     * the causing events having the specified
     * {@link TriggeredEvent#getEventKind() event kind}. The arguments are
     * iterated in backwards order as they have occurred. That is, in the same
     * order as iterated by the {@code Iterable} returned by the
     * {@link #getCauses() getCauses()} method but different kind of events
     * are filtered out.
     *
     * @param eventKind the kind of the causing events to be returned. This
     *   argument can be {@code null}, in which case an empty {@code Iterable}
     *   is returned because there can be no cause with {@code null} event kind.
     * @return the {@link TriggeredEvent#getEventArg() event arguments} of
     *   the causing events having the specified
     *   {@link TriggeredEvent#getEventKind() event kind}. This method never
     *   returns {@code null}.
     */
    public Iterable<Object> getArgumentsOfKind(Object eventKind);

    /**
     * Checks if there is an event amongst the {@link #getCauses() causes} with
     * the given {@link TriggeredEvent#getEventKind() event kind}.
     * <P>
     * The equivalence is based on the {@code equals} method.
     * <P>
     * In case this method returns {@code true}, the
     * {@link #getArgumentsOfKind(Object) getArgumentsOfKind} returns a
     * non-empty {@code Iterable}.
     *
     * @param eventKind the kind of the event to be checked if there is such
     *   event amongst the causes. This argument can be {@code null}, in which
     *   case this method returns {@code false}.
     * @return {@code true} if there is an event amongst the
     *   {@link #getCauses() causes} with the given
     *   {@link TriggeredEvent#getEventKind() event kind}, {@code false}
     *   otherwise
     */
    public boolean isCausedByKind(Object eventKind);

    /**
     * Checks if the specified event is amongst the {@link #getCauses() causes}.
     * <P>
     * The equivalence is based on the {@code equals} method. That is, this
     * method looks for events of the same kind and argument.
     *
     * @param event the event to be checked if it is amongst the causes. This
     *   argument can be {@code null}, in which case this method returns
     *   {@code false}.
     * @return {@code true} if the event is amongst the
     *   {@link #getCauses() causes}, {@code false} otherwise
     */
    public boolean isCausedByEvent(TriggeredEvent<?> event);
}
