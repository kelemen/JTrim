package org.jtrim.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public final class ListenerManagerTests {
    public static void executeAllTests(ManagerFactory factory) throws Exception {
        Throwable toThrow = null;

        int failureCount = 0;
        for (Method method: ListenerManagerTests.class.getMethods()) {
            if (method.isAnnotationPresent(GenericTest.class)) {
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

    public static void executeTest(String methodName, ManagerFactory factory) throws Throwable {
        try {
            Method method = ListenerManagerTests.class.getMethod(methodName, ManagerFactory.class);
            method.invoke(null, factory);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    private static void dispatchEvents(ListenerManager<ObjectEventListener, Object> manager, Object arg) {
        manager.onEvent(ObjectDispatcher.INSTANCE, arg);
    }

    private static ListenerManager<ObjectEventListener, Object> createEmpty(ManagerFactory factory) {
        return factory.createEmpty(ObjectEventListener.class, Object.class);
    }

    @GenericTest
    public static void testSingleRegisterListener(ManagerFactory factory) {
        Object testArg = new Object();
        ObjectEventListener listener = mock(ObjectEventListener.class);

        ListenerManager<ObjectEventListener, Object> listeners = createEmpty(factory);

        ListenerRef listenerRef = listeners.registerListener(listener);
        assertNotNull(listenerRef);
        verifyZeroInteractions(listener);
        assertTrue(listenerRef.isRegistered());

        dispatchEvents(listeners, testArg);
        verify(listener).onEvent(same(testArg));
        assertTrue(listenerRef.isRegistered());

        listenerRef.unregister();
        verify(listener).onEvent(same(testArg));
        assertFalse(listenerRef.isRegistered());

        dispatchEvents(listeners, testArg);
        verifyNoMoreInteractions(listener);
    }

    @GenericTest
    public void testTwoRegisterListener(ManagerFactory factory) {
        ObjectEventListener listener1 = mock(ObjectEventListener.class);
        ObjectEventListener listener2 = mock(ObjectEventListener.class);

        Object testArg = new Object();
        ListenerManager<ObjectEventListener, Object> listeners = createEmpty(factory);

        ListenerRef listenerRef1 = listeners.registerListener(listener1);
        verifyZeroInteractions(listener1);
        assertTrue(listenerRef1.isRegistered());

        ListenerRef listenerRef2 = listeners.registerListener(listener2);
        verifyZeroInteractions(listener2);
        assertTrue(listenerRef2.isRegistered());

        dispatchEvents(listeners, testArg);
        verify(listener1).onEvent(same(testArg));
        verify(listener2).onEvent(same(testArg));
        assertTrue(listenerRef1.isRegistered());
        assertTrue(listenerRef2.isRegistered());

        listenerRef1.unregister();
        verify(listener1).onEvent(same(testArg));
        verify(listener2).onEvent(same(testArg));
        assertFalse(listenerRef1.isRegistered());
        assertTrue(listenerRef2.isRegistered());

        dispatchEvents(listeners, testArg);
        verify(listener1).onEvent(same(testArg));
        verify(listener2, times(2)).onEvent(same(testArg));
        assertFalse(listenerRef1.isRegistered());
        assertTrue(listenerRef2.isRegistered());

        listenerRef2.unregister();
        verify(listener1).onEvent(same(testArg));
        verify(listener2, times(2)).onEvent(same(testArg));
        assertFalse(listenerRef1.isRegistered());
        assertFalse(listenerRef2.isRegistered());

        dispatchEvents(listeners, testArg);
        verify(listener1).onEvent(same(testArg));
        verify(listener2, times(2)).onEvent(same(testArg));
        assertFalse(listenerRef1.isRegistered());
        assertFalse(listenerRef2.isRegistered());

        verifyNoMoreInteractions(listener1, listener2);
    }

    @GenericTest
    public void testGetListenerCount(ManagerFactory factory) {
        ListenerManager<ObjectEventListener, Object> listeners = createEmpty(factory);
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

    public void testFailedListener(ManagerFactory factory) {
        Object testArg = new Object();

        ListenerManager<ObjectEventListener, Object> manager = createEmpty(factory);

        ObjectEventListener listener1 = mock(ObjectEventListener.class);
        ObjectEventListener listener2 = mock(ObjectEventListener.class);
        ObjectEventListener listener3 = mock(ObjectEventListener.class);

        RuntimeException exception1 = new RuntimeException();
        RuntimeException exception2 = new RuntimeException();

        doThrow(exception1).when(listener1).onEvent(any());
        doThrow(exception2).when(listener2).onEvent(any());

        manager.registerListener(listener1);
        manager.registerListener(listener2);
        manager.registerListener(listener3);

        try {
            manager.onEvent(ObjectDispatcher.INSTANCE, testArg);
            fail("Exception expected.");
        } catch (RuntimeException ex) {
            if (ex == exception1) {
                Throwable[] suppressed = ex.getSuppressed();
                assertEquals(1, suppressed.length);
                assertSame(exception2, suppressed[0]);
            }
            else if (ex == exception2) {
                Throwable[] suppressed = ex.getSuppressed();
                assertEquals(1, suppressed.length);
                assertSame(exception1, suppressed[0]);
            }
            else {
                fail("Unexpected exception.");
            }
        }

        verify(listener1).onEvent(same(testArg));
        verify(listener2).onEvent(same(testArg));
        verify(listener3).onEvent(same(testArg));
        verifyNoMoreInteractions(listener1, listener2, listener3);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    private @interface GenericTest {
    }

    public static interface ManagerFactory {
        public <ListenerType, ArgType> ListenerManager<ListenerType, ArgType> createEmpty(
                Class<ListenerType> listenerClass, Class<ArgType> argClass);
    }

    private interface ObjectEventListener {
        public void onEvent(Object arg);
    }

    private enum ObjectDispatcher
    implements
            EventDispatcher<ObjectEventListener, Object> {

        INSTANCE;

        @Override
        public void onEvent(ObjectEventListener eventListener, Object arg) {
            eventListener.onEvent(arg);
        }
    }

    private ListenerManagerTests() {
        throw new AssertionError();
   }
}
