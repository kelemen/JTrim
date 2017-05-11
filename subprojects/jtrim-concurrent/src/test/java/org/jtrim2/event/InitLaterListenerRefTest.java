package org.jtrim2.event;

import java.util.concurrent.Phaser;
import org.junit.Test;

import static org.junit.Assert.*;

public class InitLaterListenerRefTest {
    @Test
    public void testPreUnregisterInit() {
        DummyListenerRef wrappedRef = new DummyListenerRef();
        InitLaterListenerRef listenerRef = new InitLaterListenerRef();

        listenerRef.init(wrappedRef);

        assertTrue(wrappedRef.isRegistered());
        listenerRef.unregister();
        assertFalse(wrappedRef.isRegistered());
    }

    @Test
    public void testPostUnregisterInit() {
        DummyListenerRef wrappedRef = new DummyListenerRef();
        InitLaterListenerRef listenerRef = new InitLaterListenerRef();

        listenerRef.unregister();

        listenerRef.init(wrappedRef);
        assertFalse(wrappedRef.isRegistered());
    }

    @Test(timeout = 10000)
    public void testConcurrentUnregisterInit() throws InterruptedException {
        final int testCount = 100;

        final DummyListenerRef[] wrappedRefs = new DummyListenerRef[testCount];
        final InitLaterListenerRef[] listenerRefs = new InitLaterListenerRef[testCount];

        for (int i = 0; i < testCount; i++) {
            wrappedRefs[i] = new DummyListenerRef();
            listenerRefs[i] = new InitLaterListenerRef();
        }

        final Phaser phaser = new Phaser(2);

        Thread unregisterThread = new Thread(() -> {
            for (int i = 0; i < testCount; i++) {
                phaser.arriveAndAwaitAdvance();
                listenerRefs[i].unregister();
            }
        });
        unregisterThread.start();

        Thread initThread = new Thread(() -> {
            for (int i = 0; i < testCount; i++) {
                phaser.arriveAndAwaitAdvance();
                listenerRefs[i].init(wrappedRefs[i]);
            }
        });
        initThread.start();

        unregisterThread.join();
        initThread.join();

        for (int i = 0; i < testCount; i++) {
            assertFalse(
                    "Every wrapped listener should have been unregistered.",
                    wrappedRefs[i].isRegistered());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testInitTwice() {
        InitLaterListenerRef listenerRef = new InitLaterListenerRef();
        listenerRef.init(new DummyListenerRef());
        listenerRef.init(new DummyListenerRef());
    }

    @Test(expected = NullPointerException.class)
    public void testInitNull() {
        InitLaterListenerRef listenerRef = new InitLaterListenerRef();
        listenerRef.init(null);
    }

    private static class DummyListenerRef implements ListenerRef {
        private volatile boolean registered;

        public DummyListenerRef() {
            this.registered = true;
        }

        public boolean isRegistered() {
            return registered;
        }

        @Override
        public void unregister() {
            registered = false;
        }
    }
}
