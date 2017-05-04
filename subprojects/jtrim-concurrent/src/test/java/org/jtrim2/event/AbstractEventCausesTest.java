package org.jtrim2.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class AbstractEventCausesTest {

    public AbstractEventCausesTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static EventCauses create(TriggeredEvent<?>... events) {
        return new AbstractEventCausesImpl(events);
    }

    private static void cmpIterable(Iterable<?> iterable, Object... expected) {
        Iterator<?> itr = iterable.iterator();
        for (int i = 0; i < expected.length; i++) {
            assertTrue(itr.hasNext());
            assertSame(expected[i], itr.next());
        }
        assertFalse(itr.hasNext());
    }

    @Test
    public void testGetArgumentsOfKindForEmpty() {
        EventCauses causes = create();

        cmpIterable(causes.getArgumentsOfKind(null));
        cmpIterable(causes.getArgumentsOfKind(new Object()));
    }

    @Test
    public void testGetArgumentsOfKindFilter1() {
        TriggeredEvent<?> event1 = new TriggeredEvent<>(new Object(), new Object());
        TriggeredEvent<?> event2 = new TriggeredEvent<>(new Object(), new Object());
        TriggeredEvent<?> event3 = new TriggeredEvent<>(new Object(), new Object());
        EventCauses causes = create(event1, event2, event3);

        cmpIterable(causes.getArgumentsOfKind(null));
        cmpIterable(causes.getArgumentsOfKind(new Object()));

        cmpIterable(causes.getArgumentsOfKind(event1.getEventKind()), event1.getEventArg());
        cmpIterable(causes.getArgumentsOfKind(event2.getEventKind()), event2.getEventArg());
        cmpIterable(causes.getArgumentsOfKind(event3.getEventKind()), event3.getEventArg());
    }

    @Test
    public void testGetArgumentsOfKindFilter2() {
        Object eventKind = new Object();
        TriggeredEvent<?> event1 = new TriggeredEvent<>(eventKind, new Object());
        TriggeredEvent<?> event2 = new TriggeredEvent<>(eventKind, new Object());
        TriggeredEvent<?> event3 = new TriggeredEvent<>(eventKind, new Object());
        EventCauses causes = create(event1, event2, event3);

        cmpIterable(causes.getArgumentsOfKind(eventKind),
                event1.getEventArg(), event2.getEventArg(), event3.getEventArg());
    }

    @Test(expected = NoSuchElementException.class)
    public void testGetArgumentsOfKindTooManyNext() {
        TriggeredEvent<?> event1 = new TriggeredEvent<>(new Object(), new Object());
        TriggeredEvent<?> event2 = new TriggeredEvent<>(new Object(), new Object());
        TriggeredEvent<?> event3 = new TriggeredEvent<>(new Object(), new Object());
        EventCauses causes = create(event1, event2, event3);

        Iterator<?> iterator = causes.getArgumentsOfKind(event1.getEventKind()).iterator();
        try {
            iterator.next();
        } catch (NoSuchElementException ex) {
            fail("Early exception.");
        }
        iterator.next();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetArgumentsOfKindRemove() {
        TriggeredEvent<?> event1 = new TriggeredEvent<>(new Object(), new Object());
        TriggeredEvent<?> event2 = new TriggeredEvent<>(new Object(), new Object());
        TriggeredEvent<?> event3 = new TriggeredEvent<>(new Object(), new Object());
        EventCauses causes = create(event1, event2, event3);

        Iterator<?> iterator = causes.getArgumentsOfKind(event1.getEventKind()).iterator();
        iterator.next();
        iterator.remove();
    }

    @Test
    public void testIsCausedByEventForEmpty() {
        EventCauses causes = create();

        assertFalse(causes.isCausedByEvent(null));
        assertFalse(causes.isCausedByEvent(new TriggeredEvent<>(new Object(), new Object())));
    }

    @Test
    public void testIsCausedByEvent() {
        TriggeredEvent<?> event1 = new TriggeredEvent<>(new Object(), new Object());
        TriggeredEvent<?> event2 = new TriggeredEvent<>(new Object(), new Object());
        TriggeredEvent<?> event3 = new TriggeredEvent<>(new Object(), new Object());
        EventCauses causes = create(event1, event2, event3);

        assertFalse(causes.isCausedByEvent(null));
        assertFalse(causes.isCausedByEvent(new TriggeredEvent<>(new Object(), new Object())));
        assertTrue(causes.isCausedByEvent(event1));
        assertTrue(causes.isCausedByEvent(event2));
        assertTrue(causes.isCausedByEvent(event3));
    }

    @Test
    public void testIsCausedByKindForEmpty() {
        EventCauses causes = create();
        assertFalse(causes.isCausedByKind(null));
        assertFalse(causes.isCausedByKind(new Object()));
    }

    @Test
    public void testIsCausedByKind() {
        TriggeredEvent<?> event1 = new TriggeredEvent<>(new Object(), new Object());
        TriggeredEvent<?> event2 = new TriggeredEvent<>(new Object(), new Object());
        TriggeredEvent<?> event3 = new TriggeredEvent<>(new Object(), new Object());
        EventCauses causes = create(event1, event2, event3);

        assertFalse(causes.isCausedByKind(null));
        assertFalse(causes.isCausedByKind(new Object()));
        assertTrue(causes.isCausedByKind(event1.getEventKind()));
        assertTrue(causes.isCausedByKind(event2.getEventKind()));
        assertTrue(causes.isCausedByKind(event3.getEventKind()));
    }

    public static final class AbstractEventCausesImpl extends AbstractEventCauses {
        private final List<TriggeredEvent<?>> events;

        public AbstractEventCausesImpl(TriggeredEvent<?>... events) {
            this.events = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(events)));
        }

        @Override
        public int getNumberOfCauses() {
            return events.size();
        }

        @Override
        public Iterable<TriggeredEvent<?>> getCauses() {
            return events;
        }
    }
}
