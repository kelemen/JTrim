package org.jtrim2.event.track;

import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;

public class LinkedEventTrackerTest {
    public static class GenericTests extends EventTrackerTests {
        public GenericTests() {
            super(Arrays.asList(LinkedEventTracker::new));
        }
    }

    // Since we implemented the equals and hashCode methods of the managers of
    // LinkedEventTracker, test them as well.

    @Test
    @SuppressWarnings("ObjectEqualsNull")
    public void testEqualsOfManager() {
        EventTracker tracker = new LinkedEventTracker();

        Object eventKind = new Object();
        TrackedListenerManager<Object> base = tracker.getManagerOfType(eventKind, Object.class);

        assertTrue(base.equals(base));
        assertTrue(base.equals(tracker.getManagerOfType(eventKind, Object.class)));
        assertFalse(base.equals(tracker.getManagerOfType(new Object(), Object.class)));
        assertFalse(base.equals(tracker.getManagerOfType(eventKind, Boolean.class)));
        assertFalse(base.equals(tracker.getManagerOfType(new Object(), Boolean.class)));
        assertFalse(base.equals(null));
        assertFalse(base.equals(new Object()));
        assertFalse(base.equals(new LinkedEventTracker().getManagerOfType(eventKind, Object.class)));
    }

    @Test
    public void testHashCodeOfManager() {
        EventTracker tracker = new LinkedEventTracker();

        Object eventKind = new Object();
        assertEquals(tracker.getManagerOfType(eventKind, Object.class).hashCode(),
                tracker.getManagerOfType(eventKind, Object.class).hashCode());
    }
}
