package org.jtrim2.testutils.event;

import java.util.Collection;
import java.util.logging.Level;
import org.jtrim2.event.ListenerManager;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.JTrimTests;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class ListenerManagerTests extends JTrimTests<TestManagerFactory> {
    public ListenerManagerTests(Collection<? extends TestManagerFactory> factories) {
        super(factories);
    }

    private static void dispatchEvents(ListenerManager<ObjectEventListener> manager, Object arg) {
        manager.onEvent(ObjectEventListener::onEvent, arg);
    }

    private static ListenerManager<ObjectEventListener> createEmpty(TestManagerFactory factory) {
        return factory.createEmpty(ObjectEventListener.class, Object.class);
    }

    @Test
    public void testSingleRegisterListener() throws Exception {
        testAll((factory) -> {
            Object testArg = new Object();
            ObjectEventListener listener = mock(ObjectEventListener.class);

            ListenerManager<ObjectEventListener> listeners = createEmpty(factory);

            ListenerRef listenerRef = listeners.registerListener(listener);
            assertNotNull(listenerRef);
            verifyNoInteractions(listener);

            dispatchEvents(listeners, testArg);
            verify(listener).onEvent(same(testArg));

            listenerRef.unregister();
            verify(listener).onEvent(same(testArg));

            dispatchEvents(listeners, testArg);
            verifyNoMoreInteractions(listener);
        });
    }

    @Test
    public void testTwoRegisterListener() throws Exception {
        testAll((factory) -> {
            ObjectEventListener listener1 = mock(ObjectEventListener.class);
            ObjectEventListener listener2 = mock(ObjectEventListener.class);

            Object testArg = new Object();
            ListenerManager<ObjectEventListener> listeners = createEmpty(factory);

            ListenerRef listenerRef1 = listeners.registerListener(listener1);
            verifyNoInteractions(listener1);

            ListenerRef listenerRef2 = listeners.registerListener(listener2);
            verifyNoInteractions(listener2);

            dispatchEvents(listeners, testArg);
            verify(listener1).onEvent(same(testArg));
            verify(listener2).onEvent(same(testArg));

            listenerRef1.unregister();
            verify(listener1).onEvent(same(testArg));
            verify(listener2).onEvent(same(testArg));

            dispatchEvents(listeners, testArg);
            verify(listener1).onEvent(same(testArg));
            verify(listener2, times(2)).onEvent(same(testArg));

            listenerRef2.unregister();
            verify(listener1).onEvent(same(testArg));
            verify(listener2, times(2)).onEvent(same(testArg));

            dispatchEvents(listeners, testArg);
            verify(listener1).onEvent(same(testArg));
            verify(listener2, times(2)).onEvent(same(testArg));

            verifyNoMoreInteractions(listener1, listener2);
        });
    }

    @Test
    public void testGetListenerCount() throws Exception {
        testAll((factory) -> {
            ListenerManager<ObjectEventListener> listeners = createEmpty(factory);
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

            final ListenerManager<ObjectEventListener> listeners = createEmpty(factory);
            listeners.registerListener((Object arg) -> {
                listeners.registerListener(listener);
            });

            Object arg = new Object();
            dispatchEvents(listeners, arg);
            verifyNoInteractions(listener);

            dispatchEvents(listeners, arg);
            verify(listener).onEvent(same(arg));
            verifyNoMoreInteractions(listener);
        });
    }

    // This is not a generic test which should work for every listener
    public static void testFailedListener(TestManagerFactory factory) {
        Object testArg = new Object();

        ListenerManager<ObjectEventListener> manager = createEmpty(factory);

        ObjectEventListener listener1 = mock(ObjectEventListener.class);
        ObjectEventListener listener2 = mock(ObjectEventListener.class);
        ObjectEventListener listener3 = mock(ObjectEventListener.class);

        RuntimeException exception1 = new RuntimeException();
        RuntimeException exception3 = new RuntimeException();

        doThrow(exception1).when(listener1).onEvent(any());
        doThrow(exception3).when(listener3).onEvent(any());

        manager.registerListener(listener1);
        manager.registerListener(listener2);
        manager.registerListener(listener3);

        try (LogCollector logs = LogCollector.startCollecting("org.jtrim2.event")) {
            manager.onEvent(ObjectEventListener::onEvent, testArg);

            Throwable[] exceptions = logs.getExceptions(Level.SEVERE);
            assertEquals(2, exceptions.length);
            assertSame(exception1, exceptions[0]);
            assertSame(exception3, exceptions[1]);
        }

        verify(listener1).onEvent(same(testArg));
        verify(listener2).onEvent(same(testArg));
        verify(listener3).onEvent(same(testArg));
        verifyNoMoreInteractions(listener1, listener2, listener3);
    }


    private interface ObjectEventListener {
        public void onEvent(Object arg);
    }
}
