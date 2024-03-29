package org.jtrim2.event.track;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jtrim2.event.InitLaterListenerRef;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.JTrimTests;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class TrackedListenerManagerTests extends JTrimTests<TrackedManagerFactory> {
    public TrackedListenerManagerTests(Collection<? extends TrackedManagerFactory> factories) {
        super(factories);
    }

    private static TrackedListenerManager<Object> createEmpty(TrackedManagerFactory factory) {
        return factory.createEmpty(Object.class);
    }

    @Test
    public void testSingleRegisterListener() throws Exception {
        testAll((factory) -> {
            Object testArg = new Object();
            ObjectEventListener listener = mock(ObjectEventListener.class);

            TrackedListenerManager<Object> listeners = createEmpty(factory);

            ListenerRef listenerRef = listeners.registerListener(listener);
            assertNotNull(listenerRef);
            verifyNoInteractions(listener);

            listeners.onEvent(testArg);
            verify(listener).onEvent(argThat(eventTrack(testArg)));

            listenerRef.unregister();
            verify(listener).onEvent(argThat(eventTrack(testArg)));

            listeners.onEvent(testArg);
            verifyNoMoreInteractions(listener);
        });
    }

    @Test
    public void testTwoRegisterListener() throws Exception {
        testAll((factory) -> {
            ObjectEventListener listener1 = mock(ObjectEventListener.class);
            ObjectEventListener listener2 = mock(ObjectEventListener.class);

            Object testArg = new Object();
            TrackedListenerManager<Object> listeners = createEmpty(factory);

            ListenerRef listenerRef1 = listeners.registerListener(listener1);
            verifyNoInteractions(listener1);

            ListenerRef listenerRef2 = listeners.registerListener(listener2);
            verifyNoInteractions(listener2);

            listeners.onEvent(testArg);
            verify(listener1).onEvent(argThat(eventTrack(testArg)));
            verify(listener2).onEvent(argThat(eventTrack(testArg)));

            listenerRef1.unregister();
            verify(listener1).onEvent(argThat(eventTrack(testArg)));
            verify(listener2).onEvent(argThat(eventTrack(testArg)));

            listeners.onEvent(testArg);
            verify(listener1).onEvent(argThat(eventTrack(testArg)));
            verify(listener2, times(2)).onEvent(argThat(eventTrack(testArg)));

            listenerRef2.unregister();
            verify(listener1).onEvent(argThat(eventTrack(testArg)));
            verify(listener2, times(2)).onEvent(argThat(eventTrack(testArg)));

            listeners.onEvent(testArg);
            verify(listener1).onEvent(argThat(eventTrack(testArg)));
            verify(listener2, times(2)).onEvent(argThat(eventTrack(testArg)));

            verifyNoMoreInteractions(listener1, listener2);
        });
    }

    @Test
    public void testGetListenerCount() throws Exception {
        testAll((factory) -> {
            TrackedListenerManager<Object> listeners = createEmpty(factory);
            assertEquals(listeners.getListenerCount(), 0);

            ListenerRef listenerRef1 = listeners.registerListener(mock(ObjectEventListener.class));
            assertEquals(listeners.getListenerCount(), 1);

            ListenerRef listenerRef2 = listeners.registerListener(mock(ObjectEventListener.class));
            assertEquals(listeners.getListenerCount(), 2);

            ListenerRef listenerRef3 = listeners.registerListener(mock(ObjectEventListener.class));
            assertEquals(listeners.getListenerCount(), 3);

            listenerRef2.unregister();
            assertEquals(listeners.getListenerCount(), 2);

            listenerRef2 = listeners.registerListener(mock(ObjectEventListener.class));
            assertEquals(listeners.getListenerCount(), 3);

            listenerRef1.unregister();
            assertEquals(listeners.getListenerCount(), 2);

            listenerRef2.unregister();
            assertEquals(listeners.getListenerCount(), 1);

            listenerRef3.unregister();
            assertEquals(listeners.getListenerCount(), 0);
        });
    }

    @Test
    public void testRegistrationInEventHasNoEffect() throws Exception {
        testAll((factory) -> {
            final ObjectEventListener listener = mock(ObjectEventListener.class);

            final TrackedListenerManager<Object> listeners = createEmpty(factory);
            listeners.registerListener((TrackedEvent<Object> trackedEvent) -> {
                listeners.registerListener(listener);
            });

            Object arg = new Object();
            listeners.onEvent(arg);
            verifyNoInteractions(listener);

            listeners.onEvent(arg);
            verify(listener).onEvent(argThat(eventTrack(arg)));
            verifyNoMoreInteractions(listener);
        });
    }

    // This is not a generic test which should work for every listener
    // But do it until we have an implementation for which it is not needed.
    @SuppressWarnings("unchecked")
    @Test
    public void testFailedListener() throws Exception {
        testAll((factory) -> {
            Object testArg = new Object();

            TrackedListenerManager<Object> manager = createEmpty(factory);

            ObjectEventListener listener1 = mock(ObjectEventListener.class);
            ObjectEventListener listener2 = mock(ObjectEventListener.class);
            ObjectEventListener listener3 = mock(ObjectEventListener.class);

            RuntimeException exception1 = new RuntimeException();
            RuntimeException exception3 = new RuntimeException();

            doThrow(exception1).when(listener1).onEvent(any(TrackedEvent.class));
            doThrow(exception3).when(listener3).onEvent(any(TrackedEvent.class));

            manager.registerListener(listener1);
            manager.registerListener(listener2);
            manager.registerListener(listener3);

            try (LogCollector logs = LogCollector.startCollecting("org.jtrim2.event")) {
                manager.onEvent(testArg);

                Throwable[] exceptions = logs.getExceptions(Level.SEVERE);
                assertEquals(2, exceptions.length);
                assertSame(exception1, exceptions[0]);
                assertSame(exception3, exceptions[1]);
            }

            verify(listener1).onEvent(argThat(eventTrack(testArg)));
            verify(listener2).onEvent(argThat(eventTrack(testArg)));
            verify(listener3).onEvent(argThat(eventTrack(testArg)));
            verifyNoMoreInteractions(listener1, listener2, listener3);
        });
    }

    @Test
    public void testTrackEvent() throws Exception {
        testAll((factory) -> {
            final TrackedListenerManager<Object> manager = createEmpty(factory);

            final ObjectEventListener listener = mock(ObjectEventListener.class);

            Object testArg1 = new Object();
            final Object testArg2 = new Object();

            final InitLaterListenerRef listenerRef = new InitLaterListenerRef();
            listenerRef.init(manager.registerListener((TrackedEvent<Object> trackedEvent) -> {
                listenerRef.unregister();
                manager.registerListener(listener);
                manager.onEvent(testArg2);
            }));
            manager.onEvent(testArg1);

            verify(listener).onEvent(argThat(eventTrack(testArg2, testArg1)));
            verifyNoMoreInteractions(listener);
        });
    }

    @Test
    public void testTrackedEventCausesMisuses() throws Exception {
        testAll((factory) -> {
            final TrackedListenerManager<Object> manager = createEmpty(factory);

            Object testArg1 = new Object();
            final Object testArg2 = new Object();

            final AtomicReference<TrackedEvent<Object>> trackedEventRef
                    = new AtomicReference<>(null);

            final InitLaterListenerRef listenerRef = new InitLaterListenerRef();
            listenerRef.init(manager.registerListener((TrackedEvent<Object> trackedEvent) -> {
                listenerRef.unregister();
                manager.registerListener(trackedEventRef::set);
                manager.onEvent(testArg2);
            }));
            manager.onEvent(testArg1);

            TrackedEvent<Object> trackedEvent = trackedEventRef.get();

            Iterator<TriggeredEvent<?>> itr = trackedEvent.getCauses().getCauses().iterator();
            itr.next();
            try {
                itr.next();
                fail("Expected: NoSuchElementException.");
            } catch (NoSuchElementException ex) {
            }

            itr = trackedEvent.getCauses().getCauses().iterator();
            itr.next();
            try {
                itr.remove();
                fail("Expected: UnsupportedOperationException.");
            } catch (UnsupportedOperationException ex) {
            }
        });
    }

    public static ArgumentMatcher<TrackedEvent<Object>> eventTrack(
            final Object expected, Object... triggeredArgs) {
        final Object[] matchCauses = triggeredArgs.clone();

        return new ArgumentMatcher<>() {
            @Override
            public boolean matches(TrackedEvent<Object> argument) {
                if (expected != argument.getEventArg()) {
                    return false;
                }

                if (argument.getCauses().getNumberOfCauses() != matchCauses.length) {
                    return false;
                }

                Iterator<TriggeredEvent<?>> causesItr = argument.getCauses().getCauses().iterator();
                for (int i = 0; i < matchCauses.length; i++) {
                    if (!causesItr.hasNext()) {
                        return false;
                    }

                    if (!Objects.equals(matchCauses[i], causesItr.next().getEventArg())) {
                        return false;
                    }
                }
                return !causesItr.hasNext();
            }

            @Override
            public String toString() {
                String triggeredArgsStr = Stream.of(triggeredArgs)
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
                return "eventTrack{expected=" + expected + ", triggeredArgs=" +  triggeredArgsStr + "}";
            }
        };
    }

    private interface ObjectEventListener extends TrackedEventListener<Object> {
    }
}
