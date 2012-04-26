package org.jtrim.event;

import java.util.Collections;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an event and its causing events. This class was designed for
 * {@link EventTracker} implementations to be able to dispatch an event with its
 * {@link EventCauses causes}.
 *
 * <h3>Thread safety</h3>
 * The methods of this class are safe to be accessed from multiple threads
 * concurrently. In case the event argument and the arguments of the causes
 * are immutable (and they are recommended to be immutable), instances of this
 * class are immutable as well.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>.
 *
 * @param <ArgType> the type of the {@link #getEventArg() event argument}
 *
 * @see EventTracker
 * @see TrackedEventListener
 * @see TrackedListenerManager
 *
 * @author Kelemen Attila
 */
public final class TrackedEvent<ArgType> {
    /**
     * An {@code EventCauses} instance defining an empty cause list. That is,
     * its {@link EventCauses#getNumberOfCauses() getNumberOfCauses()} method
     * always returns zero. This instance is completely immutable.
     */
    public static final EventCauses NO_CAUSE = NoCauses.INSTANCE;

    private final EventCauses causes;
    private final ArgType eventArg;

    /**
     * Creates a {@code TrackedEvent} with no causes. Invoking this constructor
     * as passing {@link #NO_CAUSE} for the causes.
     *
     * @param eventArg the event argument of the event occurred. This argument
     *   can be {@code null} if the event allows {@code null} arguments.
     */
    public TrackedEvent(ArgType eventArg) {
        this(NO_CAUSE, eventArg);
    }

    /**
     * Creates a {@code TrackedEvent} with the given causes and argument.
     *
     * @param causes the causes of the event. This argument cannot be
     *   {@code null}.
     * @param eventArg the event argument of the event occurred. This argument
     *   can be {@code null} if the event allows {@code null} arguments.
     *
     * @throws NullPointerException thrown if the {@code causes} argument is
     *   {@code null}
     */
    public TrackedEvent(EventCauses causes, ArgType eventArg) {
        ExceptionHelper.checkNotNullArgument(causes, "causes");

        this.causes = causes;
        this.eventArg = eventArg;
    }

    /**
     * Returns the causes of the event which was specified at construction time.
     *
     * @return the causes of the event which was specified at construction time.
     *   This method never returns {@code null}.
     */
    public EventCauses getCauses() {
        return causes;
    }

    /**
     * Returns the event argument which describes the event occurred. This is
     * the same object as the one specified at construction time.
     * <P>
     * The event argument is intended to further describe a specific kind of
     * event. For example: for mouse click events, this can be a coordinate.
     * Note that the event argument can only be interpreted if the kind of event
     * occurred is also known.
     *
     * @return the event argument which describes the event occurred. This
     *   method may return {@code null} if {@code null} was specified at
     *   construction time.
     */
    public ArgType getEventArg() {
        return eventArg;
    }

    private enum NoCauses implements EventCauses {
        INSTANCE;

        @Override
        public Iterable<TriggeredEvent<?>> getCauses() {
            return Collections.emptySet();
        }

        @Override
        public Iterable<Object> getArgumentsOfKind(Object eventKind) {
            return Collections.emptySet();
        }

        @Override
        public int getNumberOfCauses() {
            return 0;
        }

        @Override
        public boolean isCausedByKind(Object eventKind) {
            return false;
        }

        @Override
        public boolean isCausedByEvent(TriggeredEvent<?> event) {
            return false;
        }
    }
}
