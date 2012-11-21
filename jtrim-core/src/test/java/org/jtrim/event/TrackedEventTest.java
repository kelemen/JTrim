package org.jtrim.event;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class TrackedEventTest {

    public TrackedEventTest() {
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

    private void checkNoCauses(EventCauses noCause) {
        assertFalse(noCause.getArgumentsOfKind(new Object()).iterator().hasNext());
        assertFalse(noCause.getArgumentsOfKind(null).iterator().hasNext());
        assertFalse(noCause.getCauses().iterator().hasNext());
        assertEquals(0, noCause.getNumberOfCauses());
        assertFalse(noCause.isCausedByEvent(null));
        assertFalse(noCause.isCausedByEvent(new TriggeredEvent<>(new Object(), new Object())));
        assertFalse(noCause.isCausedByKind(null));
        assertFalse(noCause.isCausedByKind(new Object()));
    }

    @Test
    public void testNoCauses() {
        checkNoCauses(TrackedEvent.NO_CAUSE);
    }

    private static <T> TrackedEvent<T> create(EventCauses causes, T eventArg) {
        return new TrackedEvent<>(causes, eventArg);
    }

    @Test
    public void testProperties1() {
        EventCauses causes = mock(EventCauses.class);
        Object eventArg = new Object();

        TrackedEvent<Object> event = create(causes, eventArg);
        assertSame(causes, event.getCauses());
        assertSame(eventArg, event.getEventArg());
    }

    @Test
    public void testProperties2() {
        EventCauses causes = mock(EventCauses.class);

        TrackedEvent<Object> event = create(causes, null);
        assertSame(causes, event.getCauses());
        assertNull(event.getEventArg());
    }

    @Test
    public void testProperties3() {
        Object eventArg = new Object();
        TrackedEvent<Object> event = new TrackedEvent<>(eventArg);
        assertSame(eventArg, event.getEventArg());
        checkNoCauses(event.getCauses());
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor() {
        Object eventArg = new Object();
        create(null, eventArg);
    }
}