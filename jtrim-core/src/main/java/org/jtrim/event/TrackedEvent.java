package org.jtrim.event;

import java.util.Collections;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class TrackedEvent<EventKindType, ArgType> {
    public static <EventKindType> EventCauses<EventKindType> noCause() {
        // Notice that it is safe because this EventCauses object will never
        // return any object which is expected to be a type of EventKindType
        // and due to type erasure, EventKindType is an Object anyway in the
        // compiled code.
        @SuppressWarnings("unchecked")
        EventCauses<EventKindType> result = (EventCauses<EventKindType>)NoCauses.INSTANCE;
        return result;
    }

    private final EventCauses<EventKindType> causes;
    private final ArgType eventArg;

    public TrackedEvent(ArgType eventArg) {
        this(TrackedEvent.<EventKindType>noCause(), eventArg);
    }

    public TrackedEvent(
            EventCauses<EventKindType> causes,
            ArgType eventArg) {
        ExceptionHelper.checkNotNullArgument(causes, "causes");

        this.causes = causes;
        this.eventArg = eventArg;
    }

    public EventCauses<EventKindType> getCauses() {
        return causes;
    }

    public ArgType getEventArg() {
        return eventArg;
    }

    private enum NoCauses implements EventCauses<Object> {
        INSTANCE;

        @Override
        public Iterable<TriggeredEvent<Object, ?>> getCauses() {
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
        public boolean isCausedByEvent(TriggeredEvent<?, ?> event) {
            return false;
        }
    }
}
