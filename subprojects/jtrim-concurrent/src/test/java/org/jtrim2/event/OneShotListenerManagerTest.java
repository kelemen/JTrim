package org.jtrim2.event;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import org.jtrim2.logs.LogCollector;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OneShotListenerManagerTest {
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

        verifyNoInteractions(listener1, listener2, listener3);

        Object eventArg = new Object();
        manager.onEvent(ObjectEventListener::onEvent, eventArg);

        verify(listener2).onEvent(same(eventArg));
        verify(listener3).onEvent(same(eventArg));
        verifyNoMoreInteractions(listener1, listener2, listener3);

        assertEquals(0, manager.getListenerCount());
    }

    @Test
    public void testOnEventBeforeRegister() {
        Object testArg = new Object();

        OneShotListenerManager<ObjectEventListener, Object> manager = create();

        manager.onEvent(ObjectEventListener::onEvent, testArg);
        ObjectEventListener listener = mock(ObjectEventListener.class);
        ListenerRef ref = manager.registerListener(listener);

        verifyNoInteractions(listener);
    }

    @Test
    public void testOnEventAfterRegister() {
        Object testArg = new Object();

        OneShotListenerManager<ObjectEventListener, Object> manager = create();

        ObjectEventListener listener = mock(ObjectEventListener.class);
        manager.registerListener(listener);
        verifyNoInteractions(listener);

        manager.onEvent(ObjectEventListener::onEvent, testArg);

        verify(listener).onEvent(same(testArg));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testOnEventBeforeRegisterOrNotify() {
        Object testArg = new Object();

        OneShotListenerManager<ObjectEventListener, Object> manager = create();

        manager.onEvent(ObjectEventListener::onEvent, testArg);

        ObjectEventListener listener = mock(ObjectEventListener.class);
        ListenerRef ref = manager.registerOrNotifyListener(listener);

        verify(listener).onEvent(same(testArg));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testOnEventAfterRegisterOrNotify() {
        Object testArg = new Object();

        OneShotListenerManager<ObjectEventListener, Object> manager = create();

        ObjectEventListener listener = mock(ObjectEventListener.class);
        ListenerRef ref = manager.registerOrNotifyListener(listener);
        verifyNoInteractions(listener);

        manager.onEvent(ObjectEventListener::onEvent, testArg);

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
                    manager.onEvent(ObjectEventListener::onEvent, testArg);
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
                manager.onEvent(ObjectEventListener::onEvent, testArg);
            }
        };

        for (Thread registerThread : registerThreads) {
            registerThread.start();
        }
        notifyThread.start();

        for (Thread registerThread : registerThreads) {
            registerThread.join();
        }
        notifyThread.join();

        for (ObjectEventListener listener : listeners) {
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
            manager.onEvent(ObjectEventListener::onEvent, testArg);

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
}
