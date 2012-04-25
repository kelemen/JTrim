package org.jtrim.event;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A convenient base class for {@link EventCauses} implementations.
 * <P>
 * This class provides default implementations for the methods
 * {@link #getArgumentsOfKind(Object) getArgumentsOfKind}},
 * {@link #isCausedByEvent(TriggeredEvent) isCausedByEvent}} and
 * {@link #isCausedByKind(Object) isCausedByKind}}.
 *
 * <h3>Thread safety</h3>
 * The implemented methods keep the thread-safety property of subclasses, so
 * if they adhere to the contract of {@code EventCauses}, the methods
 * implemented by {@code AbstractEventCauses} will not break the contract.
 *
 * <h4>Synchronization transparency</h4>
 * In case subclasses are <I>synchronization transparent</I>, the methods
 * implemented by {@code AbstractEventCauses} are also
 * <I>synchronization transparent</I>.
 *
 * @param <EventKindType> the type of the kind of events which can be
 *   detected as cause
 *
 * @author Kelemen Attila
 */
public abstract class AbstractEventCauses<EventKindType>
implements
        EventCauses<EventKindType> {

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method completely relies on the
     * {@link #getCauses() getCauses()} method and filters out the events with
     * different event kinds.
     */
    @Override
    public Iterable<Object> getArgumentsOfKind(final EventKindType eventKind) {
        if (eventKind == null) {
            return Collections.emptySet();
        }

        final Iterable<TriggeredEvent<EventKindType, ?>> causes = getCauses();
        return new Iterable<Object>() {
            @Override
            public Iterator<Object> iterator() {
                return new EventKindIterator<>(eventKind, causes.iterator());
            }
        };
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method completely relies on the
     * {@link #getCauses() getCauses()} method. This method checks every element
     * within the causes {@code Iterable} and returns {@code true} if finds the
     * specified event amongst them.
     */
    @Override
    public boolean isCausedByEvent(TriggeredEvent<? extends EventKindType, ?> event) {
        if (event == null) {
            return false;
        }

        for (TriggeredEvent<EventKindType, ?> cause: getCauses()) {
            if (Objects.equals(event, cause)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc }
     * <P>
     * <B>Implementation note</B>: This method completely relies on the
     * {@link #getCauses() getCauses()} method. This method checks every element
     * within the causes {@code Iterable} and returns {@code true} if finds an
     * event amongst them with the given event kind.
     */
    @Override
    public boolean isCausedByKind(EventKindType eventKind) {
        if (eventKind == null) {
            return false;
        }

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
