package org.jtrim.event;

import java.util.Collections;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class TrackedEvent<ArgType> {
    public static EventCauses noCause() {
        return NoCauses.INSTANCE;
    }

    private final EventCauses causes;
    private final ArgType eventArg;

    public TrackedEvent(ArgType eventArg) {
        this(noCause(), eventArg);
    }

    public TrackedEvent(
            EventCauses causes,
            ArgType eventArg) {
        ExceptionHelper.checkNotNullArgument(causes, "causes");

        this.causes = causes;
        this.eventArg = eventArg;
    }

    public EventCauses getCauses() {
        return causes;
    }

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
