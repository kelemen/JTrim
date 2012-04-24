package org.jtrim.event;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 *
 * @author Kelemen Attila
 */
public abstract class AbstractEventCauses<EventKindType>
implements
        EventCauses<EventKindType> {

    @Override
    public Iterable<Object> getArgumentsOfKind(final EventKindType eventKind) {
        final Iterable<TriggeredEvent<EventKindType, ?>> causes = getCauses();
        return new Iterable<Object>() {
            @Override
            public Iterator<Object> iterator() {
                return new EventKindIterator<>(eventKind, causes.iterator());
            }
        };
    }

    @Override
    public boolean isCausedByEvent(TriggeredEvent<? extends EventKindType, ?> event) {
        for (TriggeredEvent<EventKindType, ?> cause: getCauses()) {
            if (Objects.equals(event, cause)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCausedByKind(Object eventKind) {
        for (TriggeredEvent<EventKindType, ?> cause: getCauses()) {
            if (Objects.equals(eventKind, cause.getEventKind())) {
                return true;
            }
        }
        return false;
    }

    private static class EventKindIterator<EventKindType>
    implements
            Iterator<Object> {

        private final EventKindType eventKind;
        private final Iterator<TriggeredEvent<EventKindType, ?>> itr;
        private TriggeredEvent<EventKindType, ?> current;

        public EventKindIterator(
                EventKindType eventKind,
                Iterator<TriggeredEvent<EventKindType, ?>> itr) {
            this.eventKind = eventKind;
            this.itr = itr;
            this.current = null;
        }

        private void moveItrToNext() {
            while (itr.hasNext()) {
                current = itr.next();
                Objects.requireNonNull(current, "A cause in EventCauses is null.");
                if (Objects.equals(eventKind, current.getEventKind())) {
                    return;
                }
            }
            current = null;
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public Object next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Object result = current.getEventArg();
            moveItrToNext();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("The cause cannot be removed.");
        }
    }
}
