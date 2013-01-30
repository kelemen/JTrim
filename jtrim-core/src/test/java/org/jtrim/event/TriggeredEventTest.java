package org.jtrim.event;

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
public class TriggeredEventTest {

    public TriggeredEventTest() {
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

    private static <T> TriggeredEvent<T> create(Object eventKind, T eventArg) {
        return new TriggeredEvent<>(eventKind, eventArg);
    }

    @Test
    public void testProperties1() {
        Object eventKind = new Object();
        Object eventArg = new Object();
        TriggeredEvent<?> event = create(eventKind, eventArg);

        assertSame(eventKind, event.getEventKind());
        assertSame(eventArg, event.getEventArg());
    }

    @Test
    public void testProperties2() {
        Object eventKind = new Object();
        TriggeredEvent<?> event = create(eventKind, null);

        assertSame(eventKind, event.getEventKind());
        assertNull(event.getEventArg());
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalConstructor() {
        Object eventArg = new Object();
        create(null, eventArg);
    }

    /**
     * Test of equals method, of class TriggeredEvent.
     */
    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEquals() {
        Object eventKind = new Object();
        Object eventArg = new Object();
        TriggeredEvent<?> event = create(eventKind, eventArg);

        assertTrue(event.equals(create(eventKind, eventArg)));
        assertTrue(event.equals(event));
        assertFalse(event.equals(null));
        assertFalse(event.equals(new Object()));
        assertFalse(event.equals(create(eventKind, null)));
        assertFalse(event.equals(create(new Object(), eventArg)));
        assertFalse(event.equals(create(eventKind, new Object())));
        assertFalse(event.equals(create(new Object(), new Object())));
    }

    /**
     * Test of hashCode method, of class TriggeredEvent.
     */
    @Test
    public void testHashCode() {
        Object eventKind = new Object();
        Object eventArg = new Object();

        assertEquals(create(eventKind, eventArg).hashCode(), create(eventKind, eventArg).hashCode());
        assertEquals(create(eventKind, null).hashCode(), create(eventKind, null).hashCode());
    }
}