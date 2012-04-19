package org.jtrim.event;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class CopyOnTriggerListenerManagerTest {

    private static final EventDispatcher<Runnable, Void> TEST_EVENT_DISPATCHER
            = new RunnableDispatcher();

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

    private static CopyOnTriggerListenerManager<Runnable, Void> createInstance() {
        return new CopyOnTriggerListenerManager<>();
    }

    private static void dispatchEvents(ListenerManager<Runnable, Void> container) {
        container.onEvent(TEST_EVENT_DISPATCHER, null);
    }

    private void assertListenerRef(
            ListenerRef<?> listenerRef,
            CountingListener listener,
            long expectedRunCount,
            boolean expectedRegisteredState) {

        assertEquals(listener.getRunCount(), expectedRunCount);
        assertSame(listenerRef.getListener(), listener);
        assertTrue("isRegistered() != " + expectedRegisteredState,
                listenerRef.isRegistered() == expectedRegisteredState);
    }

    @Test
    public void testSingleRegisterListener() {
        CountingListener listener = new CountingListener();

        ListenerManager<Runnable, Void> listeners = createInstance();
        ListenerRef<?> listenerRef = listeners.registerListener(listener);
        assertNotNull(listenerRef);
        assertListenerRef(listenerRef, listener, 0, true);

        dispatchEvents(listeners);
        assertListenerRef(listenerRef, listener, 1, true);

        listenerRef.unregister();
        assertListenerRef(listenerRef, listener, 1, false);

        dispatchEvents(listeners);
        assertListenerRef(listenerRef, listener, 1, false);
    }

    @Test
    public void testTwoRegisterListener() {
        CountingListener listener1 = new CountingListener();
        CountingListener listener2 = new CountingListener();

        ListenerManager<Runnable, Void> listeners = createInstance();

        ListenerRef<?> listenerRef1 = listeners.registerListener(listener1);
        assertListenerRef(listenerRef1, listener1, 0, true);

        ListenerRef<?> listenerRef2 = listeners.registerListener(listener2);
        assertListenerRef(listenerRef2, listener2, 0, true);

        dispatchEvents(listeners);
        assertListenerRef(listenerRef1, listener1, 1, true);
        assertListenerRef(listenerRef2, listener2, 1, true);

        listenerRef1.unregister();
        assertListenerRef(listenerRef1, listener1, 1, false);
        assertListenerRef(listenerRef2, listener2, 1, true);

        dispatchEvents(listeners);
        assertListenerRef(listenerRef1, listener1, 1, false);
        assertListenerRef(listenerRef2, listener2, 2, true);

        listenerRef2.unregister();
        assertListenerRef(listenerRef1, listener1, 1, false);
        assertListenerRef(listenerRef2, listener2, 2, false);

        dispatchEvents(listeners);
        assertListenerRef(listenerRef1, listener1, 1, false);
        assertListenerRef(listenerRef2, listener2, 2, false);
    }

    @Test
    public void testGetListenerCount() {
        ListenerManager<Runnable, Void> listeners = createInstance();
        assertEquals(listeners.getListenerCount(), 0);

        ListenerRef<?> listenerRef1 = listeners.registerListener(new CountingListener());
        assertEquals(listeners.getListenerCount(), 1);

        ListenerRef<?> listenerRef2 = listeners.registerListener(new CountingListener());
        assertEquals(listeners.getListenerCount(), 2);

        ListenerRef<?> listenerRef3 = listeners.registerListener(new CountingListener());
        assertEquals(listeners.getListenerCount(), 3);

        listenerRef2.unregister();
        assertEquals(listeners.getListenerCount(), 2);

        listenerRef2 = listeners.registerListener(new CountingListener());
        assertEquals(listeners.getListenerCount(), 3);

        listenerRef1.unregister();
        assertEquals(listeners.getListenerCount(), 2);

        listenerRef2.unregister();
        assertEquals(listeners.getListenerCount(), 1);

        listenerRef3.unregister();
        assertEquals(listeners.getListenerCount(), 0);
    }

    private void checkContainsListener(Collection<Runnable> collection, Runnable... elements) {
        assertEquals(collection.size(), elements.length);
        for (Runnable element: elements) {
            assertTrue(collection.contains(element));
        }
    }

    @Test
    public void testGetListeners() {
        CountingListener listener1 = new CountingListener();
        CountingListener listener2 = new CountingListener();
        CountingListener listener3 = new CountingListener();

        CopyOnTriggerListenerManager<Runnable, Void> listeners = createInstance();
        checkContainsListener(listeners.getListeners());

        ListenerRef<?> listenerRef1 = listeners.registerListener(listener1);
        checkContainsListener(listeners.getListeners(), listener1);

        ListenerRef<?> listenerRef2 = listeners.registerListener(listener2);
        checkContainsListener(listeners.getListeners(), listener1, listener2);

        ListenerRef<?> listenerRef3 = listeners.registerListener(listener3);
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

    private static class CountingListener implements Runnable {
        private final AtomicLong runCount;

        public CountingListener() {
            this.runCount = new AtomicLong();
        }

        @Override
        public void run() {
            runCount.incrementAndGet();
        }

        public long getRunCount() {
            return runCount.get();
        }
    }

    private static class RunnableDispatcher implements EventDispatcher<Runnable, Void> {
        @Override
        public void onEvent(Runnable eventListener, Void arg) {
            eventListener.run();
        }
    }
}
