package org.jtrim2.event;

import java.util.Collection;
import org.jtrim2.testutils.event.ListenerManagerTests;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

    private static <ListenerType> CopyOnTriggerListenerManager<ListenerType> create() {
        return new CopyOnTriggerListenerManager<>();
    }

    private static ListenerManagerTests.ManagerFactory createFactory() {
        return new ListenerManagerTests.ManagerFactory() {
            @Override
            public <ListenerType> ListenerManager<ListenerType> createEmpty(
                    Class<ListenerType> listenerClass, Class<?> argClass) {
                return create();
            }
        };
    }

    @Test
    public void testGenericManagerProperties() throws Throwable {
        ListenerManagerTests.executeAllTests(createFactory());
    }

    @Test
    public void testFailedListener() throws Throwable {
        ListenerManagerTests.executeTest("testFailedListener", createFactory());
    }

    private void checkContainsListener(Collection<ObjectEventListener> collection, ObjectEventListener... elements) {
        assertEquals(collection.size(), elements.length);
        for (ObjectEventListener element: elements) {
            assertTrue(collection.contains(element));
        }
    }

    @Test
    public void testGetListeners() {
        CopyOnTriggerListenerManager<ObjectEventListener> listeners = create();

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
