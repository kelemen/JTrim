package org.jtrim.event;

import java.util.concurrent.Phaser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class InitLaterListenerRefTest {
    public InitLaterListenerRefTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testPreUnregisterInit() {
        ListenerRef wrappedRef = new DummyListenerRef();
        InitLaterListenerRef listenerRef = new InitLaterListenerRef();

        assertTrue(listenerRef.isRegistered());

        listenerRef.init(wrappedRef);

        assertTrue(listenerRef.isRegistered());
        assertTrue(wrappedRef.isRegistered());

        listenerRef.unregister();

        assertFalse(listenerRef.isRegistered());
        assertFalse(wrappedRef.isRegistered());
    }

    @Test
    public void testPostUnregisterInit() {
        ListenerRef wrappedRef = new DummyListenerRef();
        InitLaterListenerRef listenerRef = new InitLaterListenerRef();

        assertTrue(listenerRef.isRegistered());

        listenerRef.unregister();

        assertTrue(listenerRef.isRegistered());

        listenerRef.init(wrappedRef);

        assertFalse(listenerRef.isRegistered());
        assertFalse(wrappedRef.isRegistered());
    }

    @Test(timeout = 10000)
    public void testConcurrentUnregisterInit() throws InterruptedException {
        final int testCount = 100;

        final ListenerRef[] wrappedRefs = new ListenerRef[testCount];
        final InitLaterListenerRef[] listenerRefs = new InitLaterListenerRef[testCount];

        for (int i = 0; i < testCount; i++) {
            wrappedRefs[i] = new DummyListenerRef();
            listenerRefs[i] = new InitLaterListenerRef();
        }

        final Phaser phaser = new Phaser(2);

        Thread unregisterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < testCount; i++) {
                    phaser.arriveAndAwaitAdvance();
                    listenerRefs[i].unregister();
                }
            }
        });
        unregisterThread.start();

        Thread initThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < testCount; i++) {
                    phaser.arriveAndAwaitAdvance();
                    listenerRefs[i].init(wrappedRefs[i]);
                }
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

    private static class DummyListenerRef implements ListenerRef {
        private volatile boolean registered;

        public DummyListenerRef() {
            this.registered = true;
        }

        @Override
        public boolean isRegistered() {
            return registered;
        }

        @Override
        public void unregister() {
            registered = false;
        }
    }
}
