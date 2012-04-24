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
    private final TriggeredEvent<EventKindType, ArgType> event;

    public TrackedEvent(TriggeredEvent<EventKindType, ArgType> event) {
        this(TrackedEvent.<EventKindType>noCause(), event);
    }

    public TrackedEvent(
            EventCauses<EventKindType> causes,
            TriggeredEvent<EventKindType, ArgType> event) {
        ExceptionHelper.checkNotNullArgument(causes, "causes");
        ExceptionHelper.checkNotNullArgument(event, "event");

        this.causes = causes;
        this.event = event;
    }

    public EventCauses<EventKindType> getCauses() {
        return causes;
    }

    public TriggeredEvent<EventKindType, ArgType> getEvent() {
        return event;
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
