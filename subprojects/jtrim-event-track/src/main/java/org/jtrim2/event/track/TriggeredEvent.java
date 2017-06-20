package org.jtrim2.event.track;

import java.util.Objects;

/**
 * Defines an event which has occurred or yet to be occur. The event is defined
 * by an event specific argument and the kind of event occurred. The kind of
 * event usually specifies the method of an interface to be notified.
 * <P>
 * Unlike the event specific argument itself, instances of
 * {@code TriggeredEvent} should describe the exact event.
 * <P>
 * The {@code TriggeredEvent} overrides {@link #equals(Object) equals} and
 * {@link #hashCode() hashCode} to be equal to other {@code TriggeredEvent}, if
 * and only, if they have the same event kind and event argument (the
 * equivalence is based on the {@code equals} method).
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed from multiple threads
 * concurrently. In case the {@link #getEventArg() event argument} and the
 * {@link #getEventKind() event kind} are immutable (and they are recommended to
 * be immutable), instances of this class are immutable as well.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @param <ArgType> the type of the {@link #getEventArg() event argument}
 *
 * @see EventCauses
 */
public final class TriggeredEvent<ArgType> {
    private final Object eventKind;
    private final ArgType eventArg;

    /**
     * Creates a new {@code TriggeredEvent} with the specified
     * {@link #getEventKind() event kind} and
     * {@link #getEventArg() event argument}.
     *
     * @param eventKind the kind of the event occurred. This argument cannot
     *   be {@code null}.
     * @param eventArg the event specific argument. This argument can be
     *   {@code null} if {@code null} argument is allowed for the specified
     *   kind of events.
     *
     * @throws NullPointerException thrown if the specified event kind is
     *   {@code null}
     */
    public TriggeredEvent(Object eventKind, ArgType eventArg) {
        Objects.requireNonNull(eventKind, "eventKind");

        this.eventKind = eventKind;
        this.eventArg = eventArg;
    }

    /**
     * Returns the kind of event occurred specified at construction time.
     * <P>
     * The kind of event usually specifies the method of an interface to be
     * notified.
     *
     * @return the kind of event occurred specified at construction time. This
     *   method never returns {@code null}.
     */
    public Object getEventKind() {
        return eventKind;
    }

    /**
     * Returns the event specific argument specified at construction time.
     * <P>
     * The exact meaning of this event argument depends on the
     * {@link #getEventKind() kind of event} occurred.
     *
     * @return the event specific argument specified at construction time. This
     *   method may return {@code null} if {@code null} was specified at
     *   construction time.
     */
    public ArgType getEventArg() {
        return eventArg;
    }

    /**
     * Checks whether this {@code TriggeredEvent} and the given object equals.
     * <P>
     * This method always returns {@code false} if the specified object is not
     * an instance of {@code TriggeredEvent}. Otherwise, this method returns
     * {@code true}, if and only, if the the {@link #getEventKind() event kind}
     * and the {@link #getEventArg() event argument} of both this and the
     * specified {@code TriggeredEvent} equals (based on the {@code equals}
     * method).
     *
     * @param obj the object to which this {@code TriggeredEvent} is to be
     *   compared to. This argument can be {@code null}, in which case,
     *   {@code false} is returned.
     *
     * @return {@code true} if the specified object is a {@code TriggeredEvent}
     *   and has equivalent event kind and event argument as this
     *   {@code TriggeredEvent}, {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final TriggeredEvent<?> other = (TriggeredEvent<?>) obj;
        return Objects.equals(this.eventKind, other.eventKind)
                && Objects.equals(this.eventArg, other.eventArg);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + Objects.hashCode(this.eventKind);
        hash = 13 * hash + Objects.hashCode(this.eventArg);
        return hash;
    }
}
