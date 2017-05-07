package org.jtrim2.event;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import org.jtrim2.logs.LogCollector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class OneShotListenerManagerTest {
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

    private static OneShotListenerManager<ObjectEventListener, Object> create() {
        return new OneShotListenerManager<>();
    }

    @Test
    public void testGetListenerCount() {
        OneShotListenerManager<ObjectEventListener, Object> manager = create();

        ObjectEventListener listener1 = mock(ObjectEventListener.class);
        ObjectEventListener listener2 = mock(ObjectEventListener.class);
        ObjectEventListener listener3 = mock(ObjectEventListener.class);

        assertEquals(0, manager.getListenerCount());

        ListenerRef ref = manager.registerListener(listener1);
        assertEquals(1, manager.getListenerCount());

        manager.registerListener(listener2);
        assertEquals(2, manager.getListenerCount());

        manager.registerListener(listener3);
        assertEquals(3, manager.getListenerCount());

        ref.unregister();
        assertEquals(2, manager.getListenerCount());

        verifyZeroInteractions(listener1, listener2, listener3);

        Object eventArg = new Object();
        manager.onEvent(ObjectDispatcher.INSTANCE, eventArg);

        verify(listener2).onEvent(same(eventArg));
        verify(listener3).onEvent(same(eventArg));
        verifyNoMoreInteractions(listener1, listener2, listener3);

        assertEquals(0, manager.getListenerCount());
    }

    @Test
    public void testOnEventBeforeRegister() {
        Object testArg = new Object();

        OneShotListenerManager<ObjectEventListener, Object> manager = create();

        manager.onEvent(ObjectDispatcher.INSTANCE, testArg);
        ObjectEventListener listener = mock(ObjectEventListener.class);
        ListenerRef ref = manager.registerListener(listener);

        assertTrue("Listener was unregistered", ref.isRegistered());
        verifyZeroInteractions(listener);
    }

    @Test
    public void testOnEventAfterRegister() {
        Object testArg = new Object();

        OneShotListenerManager<ObjectEventListener, Object> manager = create();

        ObjectEventListener listener = mock(ObjectEventListener.class);
        ListenerRef ref = manager.registerListener(listener);
        assertTrue("Listener was unregistered", ref.isRegistered());
        verifyZeroInteractions(listener);

        manager.onEvent(ObjectDispatcher.INSTANCE, testArg);

        assertFalse("Listener is registered", ref.isRegistered());
        verify(listener).onEvent(same(testArg));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testOnEventBeforeRegisterOrNotify() {
        Object testArg = new Object();

        OneShotListenerManager<ObjectEventListener, Object> manager = create();

        manager.onEvent(ObjectDispatcher.INSTANCE, testArg);

        ObjectEventListener listener = mock(ObjectEventListener.class);
        ListenerRef ref = manager.registerOrNotifyListener(listener);

        assertFalse("Listener is registered", ref.isRegistered());
        verify(listener).onEvent(same(testArg));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testOnEventAfterRegisterOrNotify() {
        Object testArg = new Object();

        OneShotListenerManager<ObjectEventListener, Object> manager = create();

        ObjectEventListener listener = mock(ObjectEventListener.class);
        ListenerRef ref = manager.registerOrNotifyListener(listener);
        assertTrue("Listener was unregistered", ref.isRegistered());
        verifyZeroInteractions(listener);

        manager.onEvent(ObjectDispatcher.INSTANCE, testArg);

        assertFalse("Listener is registered", ref.isRegistered());
        verify(listener).onEvent(same(testArg));
        verifyNoMoreInteractions(listener);
    }

    public void doTestConcurrentRegisterWithEvent() throws InterruptedException {
        final Object testArg = new Object();
        final OneShotListenerManager<ObjectEventListener, Object> manager = create();

        int listenerCount = 2 * Runtime.getRuntime().availableProcessors();

        final ObjectEventListener[] listeners = new ObjectEventListener[listenerCount];
        for (int i = 0; i < listeners.length; i++) {
            listeners[i] = mock(ObjectEventListener.class);
        }

        final CountDownLatch startLatch = new CountDownLatch(listenerCount + 1);
        Thread[] registerThreads = new Thread[listenerCount];
        for (int i = 0; i < registerThreads.length; i++) {
            final int threadIndex = i;
            registerThreads[i] = new Thread() {
                @Override
                public void run() {
                    startLatch.countDown();
                    try {
                        startLatch.await();
                    } catch (InterruptedException ex) {
                        throw new RuntimeException();
                    }
                    manager.registerListener(listeners[threadIndex]);
                    manager.onEvent(ObjectDispatcher.INSTANCE, testArg);
                }
            };
        }
        Thread notifyThread = new Thread() {
            @Override
            public void run() {
                startLatch.countDown();
                try {
                    startLatch.await();
                } catch (InterruptedException ex) {
                    throw new RuntimeException();
                }
                manager.onEvent(ObjectDispatcher.INSTANCE, testArg);
            }
        };

        for (int i = 0; i < registerThreads.length; i++) {
            registerThreads[i].start();
        }
        notifyThread.start();

        for (int i = 0; i < registerThreads.length; i++) {
            registerThreads[i].join();
        }
        notifyThread.join();

        for (int i = 0; i < listeners.length; i++) {
            ObjectEventListener listener = listeners[i];
            verify(listener).onEvent(same(testArg));
            verifyNoMoreInteractions(listener);
        }
    }

    @Test(timeout = 20000)
    public void testConcurrentRegisterWithEvent() throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            doTestConcurrentRegisterWithEvent();
        }
    }

    @Test
    public void testFailedListener() {
        Object testArg = new Object();

        OneShotListenerManager<ObjectEventListener, Object> manager = create();

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

        try (LogCollector logs = LogCollector.startCollecting("org.jtrim2.event")) {
            manager.onEvent(ObjectDispatcher.INSTANCE, testArg);

            Throwable[] exceptions = logs.getExceptions(Level.SEVERE);
            assertEquals(2, exceptions.length);
            assertSame(exception1, exceptions[0]);
            assertSame(exception2, exceptions[1]);
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
