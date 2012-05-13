package org.jtrim.event;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class OneShotListenerManagerTest {

    public OneShotListenerManagerTest() {
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

    @Test
    public void testOnEventBeforeRegister() {
        Integer testArg = 10;

        OneShotListenerManager<IntegerEventListener, Integer> manager;
        manager = new OneShotListenerManager<>();

        final AtomicReference<Integer> received = new AtomicReference<>(null);

        manager.onEvent(IntegerDispatcher.INSTANCE, testArg);
        ListenerRef ref = manager.registerListener(new IntegerEventListener() {
            @Override
            public void onEvent(Integer arg) {
                received.set(arg);
            }
        });

        assertTrue("Listener was unregistered", ref.isRegistered());
        assertNull("Listener has been notified", received.get());
    }

    @Test
    public void testOnEventAfterRegister() {
        Integer testArg = 10;

        OneShotListenerManager<IntegerEventListener, Integer> manager;
        manager = new OneShotListenerManager<>();

        final AtomicReference<Integer> received = new AtomicReference<>(null);

        ListenerRef ref = manager.registerListener(new IntegerEventListener() {
            @Override
            public void onEvent(Integer arg) {
                received.set(arg);
            }
        });
        assertTrue("Listener was unregistered", ref.isRegistered());
        assertNull("Listener has been notified", received.get());

        manager.onEvent(IntegerDispatcher.INSTANCE, testArg);

        assertFalse("Listener is registered", ref.isRegistered());
        assertEquals("Listener was not notified", testArg, received.get());
    }

    @Test
    public void testOnEventBeforeRegisterOrNotify() {
        Integer testArg = 10;

        OneShotListenerManager<IntegerEventListener, Integer> manager;
        manager = new OneShotListenerManager<>();

        final AtomicReference<Integer> received = new AtomicReference<>(null);

        manager.onEvent(IntegerDispatcher.INSTANCE, testArg);
        ListenerRef ref = manager.registerOrNotifyListener(new IntegerEventListener() {
            @Override
            public void onEvent(Integer arg) {
                received.set(arg);
            }
        });

        assertFalse("Listener is registered", ref.isRegistered());
        assertEquals("Listener was not notified", testArg, received.get());
    }

    @Test
    public void testOnEventAfterRegisterOrNotify() {
        Integer testArg = 10;

        OneShotListenerManager<IntegerEventListener, Integer> manager;
        manager = new OneShotListenerManager<>();

        final AtomicReference<Integer> received = new AtomicReference<>(null);

        ListenerRef ref = manager.registerOrNotifyListener(new IntegerEventListener() {
            @Override
            public void onEvent(Integer arg) {
                received.set(arg);
            }
        });
        assertTrue("Listener was unregistered", ref.isRegistered());
        assertNull("Listener has been notified", received.get());

        manager.onEvent(IntegerDispatcher.INSTANCE, testArg);

        assertFalse("Listener is registered", ref.isRegistered());
        assertEquals("Listener was not notified", testArg, received.get());
    }

    private interface IntegerEventListener {
        public void onEvent(Integer arg);
    }

    private enum IntegerDispatcher
    implements
            EventDispatcher<IntegerEventListener, Integer> {

        INSTANCE;

        @Override
        public void onEvent(IntegerEventListener eventListener, Integer arg) {
            eventListener.onEvent(arg);
        }
    }
}
