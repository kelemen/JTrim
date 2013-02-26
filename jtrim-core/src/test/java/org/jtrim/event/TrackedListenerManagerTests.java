package org.jtrim.event;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public final class TrackedListenerManagerTests {
    private static void executeAllTests(
            ManagerFactory factory,
            Class<? extends Annotation> annotation) throws Exception {
        Throwable toThrow = null;

        int failureCount = 0;
        for (Method method: TrackedListenerManagerTests.class.getMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                try {
                    method.invoke(null, factory);
                } catch (InvocationTargetException ex) {
                    failureCount++;
                    if (toThrow == null) toThrow = ex.getCause();
                    else toThrow.addSuppressed(ex.getCause());
                }
            }
        }

        if (toThrow != null) {
            throw new RuntimeException(failureCount + " generic test(s) have failed.", toThrow);
        }
    }

    public static void executeAllTests(ManagerFactory factory, boolean includeNonGeneric) throws Exception {
        executeAllTests(factory, GenericTest.class);
        if (includeNonGeneric) {
            executeAllTests(factory, CommonNonGenericTest.class);
        }
    }

    public static void executeTest(String methodName, ManagerFactory factory) throws Throwable {
        try {
            Method method = ListenerManagerTests.class.getMethod(methodName, ManagerFactory.class);
            method.invoke(null, factory);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private static TrackedListenerManager<Object> createEmpty(ManagerFactory factory) {
        return factory.createEmpty(Object.class);
    }

    @GenericTest
    public static void testSingleRegisterListener(ManagerFactory factory) {
        Object testArg = new Object();
        ObjectEventListener listener = mock(ObjectEventListener.class);

        TrackedListenerManager<Object> listeners = createEmpty(factory);

        ListenerRef listenerRef = listeners.registerListener(listener);
        assertNotNull(listenerRef);
        verifyZeroInteractions(listener);
        assertTrue(listenerRef.isRegistered());

        listeners.onEvent(testArg);
        verify(listener).onEvent(argThat(eventTrack(testArg)));
        assertTrue(listenerRef.isRegistered());

        listenerRef.unregister();
        verify(listener).onEvent(argThat(eventTrack(testArg)));
        assertFalse(listenerRef.isRegistered());

        listeners.onEvent(testArg);
        verifyNoMoreInteractions(listener);
    }

    @GenericTest
    public static void testTwoRegisterListener(ManagerFactory factory) {
        ObjectEventListener listener1 = mock(ObjectEventListener.class);
        ObjectEventListener listener2 = mock(ObjectEventListener.class);

        Object testArg = new Object();
        TrackedListenerManager<Object> listeners = createEmpty(factory);

        ListenerRef listenerRef1 = listeners.registerListener(listener1);
        verifyZeroInteractions(listener1);
        assertTrue(listenerRef1.isRegistered());

        ListenerRef listenerRef2 = listeners.registerListener(listener2);
        verifyZeroInteractions(listener2);
        assertTrue(listenerRef2.isRegistered());

        listeners.onEvent(testArg);
        verify(listener1).onEvent(argThat(eventTrack(testArg)));
        verify(listener2).onEvent(argThat(eventTrack(testArg)));
        assertTrue(listenerRef1.isRegistered());
        assertTrue(listenerRef2.isRegistered());

        listenerRef1.unregister();
        verify(listener1).onEvent(argThat(eventTrack(testArg)));
        verify(listener2).onEvent(argThat(eventTrack(testArg)));
        assertFalse(listenerRef1.isRegistered());
        assertTrue(listenerRef2.isRegistered());

        listeners.onEvent(testArg);
        verify(listener1).onEvent(argThat(eventTrack(testArg)));
        verify(listener2, times(2)).onEvent(argThat(eventTrack(testArg)));
        assertFalse(listenerRef1.isRegistered());
        assertTrue(listenerRef2.isRegistered());

        listenerRef2.unregister();
        verify(listener1).onEvent(argThat(eventTrack(testArg)));
        verify(listener2, times(2)).onEvent(argThat(eventTrack(testArg)));
        assertFalse(listenerRef1.isRegistered());
        assertFalse(listenerRef2.isRegistered());

        listeners.onEvent(testArg);
        verify(listener1).onEvent(argThat(eventTrack(testArg)));
        verify(listener2, times(2)).onEvent(argThat(eventTrack(testArg)));
        assertFalse(listenerRef1.isRegistered());
        assertFalse(listenerRef2.isRegistered());

        verifyNoMoreInteractions(listener1, listener2);
    }

    @GenericTest
    public static void testGetListenerCount(ManagerFactory factory) {
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
    }

    @GenericTest
    public static void testRegistrationInEventHasNoEffect(ManagerFactory factory) {
        final ObjectEventListener listener = mock(ObjectEventListener.class);

        final TrackedListenerManager<Object> listeners = createEmpty(factory);
        listeners.registerListener(new ObjectEventListener() {
            @Override
            public void onEvent(TrackedEvent<Object> trackedEvent) {
                listeners.registerListener(listener);
            }
        });

        Object arg = new Object();
        listeners.onEvent(arg);
        verifyZeroInteractions(listener);

        listeners.onEvent(arg);
        verify(listener).onEvent(argThat(eventTrack(arg)));
        verifyNoMoreInteractions(listener);
    }

    // This is not a generic test which should work for every listener
    @CommonNonGenericTest
    @SuppressWarnings("unchecked")
    public static void testFailedListener(ManagerFactory factory) {
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

        try {
            manager.onEvent(testArg);
            fail("Exception expected.");
        } catch (RuntimeException ex) {
            if (ex == exception1) {
                Throwable[] suppressed = ex.getSuppressed();
                assertEquals(1, suppressed.length);
                assertSame(exception3, suppressed[0]);
            }
            else if (ex == exception3) {
                Throwable[] suppressed = ex.getSuppressed();
                assertEquals(1, suppressed.length);
                assertSame(exception1, suppressed[0]);
            }
            else {
                fail("Unexpected exception.");
            }
        }

        verify(listener1).onEvent(argThat(eventTrack(testArg)));
        verify(listener2).onEvent(argThat(eventTrack(testArg)));
        verify(listener3).onEvent(argThat(eventTrack(testArg)));
        verifyNoMoreInteractions(listener1, listener2, listener3);
    }

    @GenericTest
    public static void testTrackEvent(ManagerFactory factory) {
        final TrackedListenerManager<Object> manager = createEmpty(factory);

        final ObjectEventListener listener = mock(ObjectEventListener.class);

        Object testArg1 = new Object();
        final Object testArg2 = new Object();

        final InitLaterListenerRef listenerRef = new InitLaterListenerRef();
        listenerRef.init(manager.registerListener(new ObjectEventListener() {
            @Override
            public void onEvent(TrackedEvent<Object> trackedEvent) {
                listenerRef.unregister();
                manager.registerListener(listener);
                manager.onEvent(testArg2);
            }
        }));
        manager.onEvent(testArg1);

        verify(listener).onEvent(argThat(eventTrack(testArg2, testArg1)));
        verifyNoMoreInteractions(listener);
    }

    @GenericTest
    public static void testTrackedEventCausesMisuses(ManagerFactory factory) {
        final TrackedListenerManager<Object> manager = createEmpty(factory);

        Object testArg1 = new Object();
        final Object testArg2 = new Object();

        final AtomicReference<TrackedEvent<Object>> trackedEventRef
                = new AtomicReference<>(null);

        final InitLaterListenerRef listenerRef = new InitLaterListenerRef();
        listenerRef.init(manager.registerListener(new ObjectEventListener() {
            @Override
            public void onEvent(TrackedEvent<Object> trackedEvent) {
                listenerRef.unregister();
                manager.registerListener(new ObjectEventListener() {
                    @Override
                    public void onEvent(TrackedEvent<Object> trackedEvent) {
                        trackedEventRef.set(trackedEvent);
                    }
                });
                manager.onEvent(testArg2);
            }
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
    }

    public static Matcher<TrackedEvent<Object>> eventTrack(
            final Object expected, Object... triggeredArgs) {
        final Object[] matchCauses = triggeredArgs.clone();

        return new ArgumentMatcher<TrackedEvent<Object>>() {
            @Override
            public boolean matches(Object argument) {
                TrackedEvent<?> trackedEvent = (TrackedEvent<?>)argument;
                if (expected != trackedEvent.getEventArg()) {
                    return false;
                }

                if (trackedEvent.getCauses().getNumberOfCauses() != matchCauses.length) {
                    return false;
                }

                Iterator<TriggeredEvent<?>> causesItr = trackedEvent.getCauses().getCauses().iterator();
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
        };
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    private @interface CommonNonGenericTest {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    private @interface GenericTest {
    }

    public static interface ManagerFactory {
        public <ArgType> TrackedListenerManager<ArgType> createEmpty(Class<ArgType> argClass);
    }

    private interface ObjectEventListener extends TrackedEventListener<Object> {
    }

    private TrackedListenerManagerTests() {
        throw new AssertionError();
   }
}
