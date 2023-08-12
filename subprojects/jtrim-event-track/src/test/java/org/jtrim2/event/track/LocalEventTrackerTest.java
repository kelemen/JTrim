package org.jtrim2.event.track;

import java.util.Arrays;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LocalEventTrackerTest {
    public static class GenericTests extends EventTrackerTests {
        public GenericTests() {
            super(Arrays.asList(LocalEventTrackerTest::create));
        }
    }

    private static LocalEventTracker create() {
        return new LocalEventTracker(new LinkedEventTracker());
    }

    @Test
    public void test() {
        ObjectEventListener listener1 = mock(ObjectEventListener.class);
        ObjectEventListener listener2 = mock(ObjectEventListener.class);
        ObjectEventListener listener3 = mock(ObjectEventListener.class);

        Object eventKind = new Object();

        EventTracker parent = new LinkedEventTracker();
        parent.getManagerOfType(eventKind, Object.class).registerListener(listener1);

        LocalEventTracker tracker = new LocalEventTracker(parent);
        tracker.getManagerOfType(eventKind, Object.class).registerListener(listener2);
        parent.getManagerOfType(eventKind, Object.class).registerListener(listener3);

        Object eventArg = new Object();
        tracker.getManagerOfType(eventKind, Object.class).onEvent(eventArg);

        verify(listener1).onEvent(argThat(eventTrack(eventArg)));
        verify(listener2).onEvent(argThat(eventTrack(eventArg)));
        verify(listener3).onEvent(argThat(eventTrack(eventArg)));
        verifyNoMoreInteractions(listener1, listener2, listener3);

        tracker.removeAllListeners();
        tracker.getManagerOfType(eventKind, Object.class).onEvent(eventArg);

        verify(listener1, times(2)).onEvent(argThat(eventTrack(eventArg)));
        verify(listener2).onEvent(argThat(eventTrack(eventArg)));
        verify(listener3, times(2)).onEvent(argThat(eventTrack(eventArg)));
        verifyNoMoreInteractions(listener1, listener2, listener3);
    }

    // Since we implemented the equals and hashCode methods of the managers of
    // LocalEventTracker, test them as well.

    @Test
    @SuppressWarnings({"ObjectEqualsNull", "SimplifiableAssertion", "EqualsWithItself", "ConstantValue"})
    public void testEqualsOfManager() {
        EventTracker tracker = create();

        Object eventKind = new Object();
        TrackedListenerManager<Object> base = tracker.getManagerOfType(eventKind, Object.class);

        assertTrue(base.equals(base));
        assertTrue(base.equals(tracker.getManagerOfType(eventKind, Object.class)));
        assertFalse(base.equals(tracker.getManagerOfType(new Object(), Object.class)));
        assertFalse(base.equals(tracker.getManagerOfType(eventKind, Boolean.class)));
        assertFalse(base.equals(tracker.getManagerOfType(new Object(), Boolean.class)));
        assertFalse(base.equals(null));
        assertFalse(base.equals(new Object()));
        assertFalse(base.equals(create().getManagerOfType(eventKind, Object.class)));
    }

    @Test
    public void testHashCodeOfManager() {
        EventTracker tracker = create();

        Object eventKind = new Object();
        assertEquals(tracker.getManagerOfType(eventKind, Object.class).hashCode(),
                tracker.getManagerOfType(eventKind, Object.class).hashCode());
    }

    private static ArgumentMatcher<TrackedEvent<Object>> eventTrack(
            final Object expected, Object... triggeredArgs) {
        return TrackedListenerManagerTests.eventTrack(expected, triggeredArgs);
    }

    private interface ObjectEventListener extends TrackedEventListener<Object> {
    }
}
