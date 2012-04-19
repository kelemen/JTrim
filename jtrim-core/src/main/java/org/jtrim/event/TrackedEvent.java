package org.jtrim.event;

import java.util.Collections;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class TrackedEvent<EventKindType, ArgType> {
    public static final EventCauses NO_CAUSE = NoCauses.INSTANCE;

    private final EventCauses causes;
    private final TriggeredEvent<EventKindType, ArgType> event;

    public TrackedEvent(TriggeredEvent<EventKindType, ArgType> event) {
        this(NO_CAUSE, event);
    }

    public TrackedEvent(EventCauses causes, TriggeredEvent<EventKindType, ArgType> event) {
        ExceptionHelper.checkNotNullArgument(causes, "causes");
        ExceptionHelper.checkNotNullArgument(event, "event");

        this.causes = causes;
        this.event = event;
    }

    public EventCauses getCauses() {
        return causes;
    }

    public TriggeredEvent<EventKindType, ArgType> getEvent() {
        return event;
    }

    private enum NoCauses implements EventCauses {
        INSTANCE;

        @Override
        public Iterable<TriggeredEvent<?, ?>> getCauses() {
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
