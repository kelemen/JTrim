package org.jtrim.event;

import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public final class TriggeredEvent<EventKindType, ArgType> {
    private final EventKindType eventKind;
    private final ArgType eventArg;

    public TriggeredEvent(EventKindType eventKind, ArgType eventArg) {
        ExceptionHelper.checkNotNullArgument(eventKind, "eventKind");

        this.eventKind = eventKind;
        this.eventArg = eventArg;
    }

    public EventKindType getEventKind() {
        return eventKind;
    }

    public ArgType getEventArg() {
        return eventArg;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TriggeredEvent<?, ?> other = (TriggeredEvent<?, ?>)obj;
        if (!Objects.equals(this.eventKind, other.eventKind)) {
            return false;
        }
        if (!Objects.equals(this.eventArg, other.eventArg)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 13 * hash + Objects.hashCode(this.eventKind);
        hash = 13 * hash + Objects.hashCode(this.eventArg);
        return hash;
    }
}
