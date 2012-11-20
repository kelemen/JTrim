package org.jtrim.event;

import java.util.Collection;
import org.junit.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class CopyOnTriggerListenerManagerTest {
    public CopyOnTriggerListenerManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static CopyOnTriggerListenerManager<ObjectEventListener, Object> create() {
        return new CopyOnTriggerListenerManager<>();
    }

    private static void dispatchEvents(ListenerManager<ObjectEventListener, Object> manager, Object arg) {
        manager.onEvent(ObjectDispatcher.INSTANCE, arg);
    }

    @Test
    public void testSingleRegisterListener() {
        Object testArg = new Object();
        ObjectEventListener listener = mock(ObjectEventListener.class);

        ListenerManager<ObjectEventListener, Object> listeners = create();

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

    @Test
    public void testTwoRegisterListener() {
        ObjectEventListener listener1 = mock(ObjectEventListener.class);
        ObjectEventListener listener2 = mock(ObjectEventListener.class);

        Object testArg = new Object();
        ListenerManager<ObjectEventListener, Object> listeners = create();

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

    @Test
    public void testGetListenerCount() {
        ListenerManager<ObjectEventListener, Object> listeners = create();
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

    private void checkContainsListener(Collection<ObjectEventListener> collection, ObjectEventListener... elements) {
        assertEquals(collection.size(), elements.length);
        for (ObjectEventListener element: elements) {
            assertTrue(collection.contains(element));
        }
    }

    @Test
    public void testGetListeners() {
        CopyOnTriggerListenerManager<ObjectEventListener, Object> listeners = create();

        ObjectEventListener listener1 = mock(ObjectEventListener.class);
        ObjectEventListener listener2 = mock(ObjectEventListener.class);
        ObjectEventListener listener3 = mock(ObjectEventListener.class);

        checkContainsListener(listeners.getListeners());

        ListenerRef listenerRef1 = listeners.registerListener(listener1);
        checkContainsListener(listeners.getListeners(), listener1);

        ListenerRef listenerRef2 = listeners.registerListener(listener2);
        checkContainsListener(listeners.getListeners(), listener1, listener2);

        ListenerRef listenerRef3 = listeners.registerListener(listener3);
        checkContainsListener(listeners.getListeners(), listener1, listener2, listener3);

        listenerRef2.unregister();
        checkContainsListener(listeners.getListeners(), listener1, listener3);

        listenerRef2 = listeners.registerListener(listener2);
        checkContainsListener(listeners.getListeners(), listener1, listener2, listener3);

        listenerRef1.unregister();
        checkContainsListener(listeners.getListeners(), listener2, listener3);

        listenerRef2.unregister();
        checkContainsListener(listeners.getListeners(), listener3);

        listenerRef3.unregister();
        checkContainsListener(listeners.getListeners());
    }

    @Test
    public void testFailedListener() {
        Object testArg = new Object();

        ListenerManager<ObjectEventListener, Object> manager = create();

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
            assertSame(exception1, ex);
            Throwable[] suppressed = ex.getSuppressed();
            assertEquals(1, suppressed.length);
            assertSame(exception2, suppressed[0]);
        }

        verify(listener1).onEvent(same(testArg));
        verify(listener2).onEvent(same(testArg));
        verify(listener3).onEvent(same(testArg));
        verifyNoMoreInteractions(listener1, listener2, listener3);
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
}
