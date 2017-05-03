package org.jtrim2.concurrent;

import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.OperationCanceledException;
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
public class WaitableSignalTest {
    private static final long POST_ACTION_WAIT_MS = 10;

    public WaitableSignalTest() {
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

    @Test(timeout = 10000)
    public void testSignalingSignal() {
        assertTrue(WaitableSignal.SIGNALING_SIGNAL.isSignaled());
        WaitableSignal.SIGNALING_SIGNAL.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
        WaitableSignal.SIGNALING_SIGNAL.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        WaitableSignal.SIGNALING_SIGNAL.signal();
        assertTrue(WaitableSignal.SIGNALING_SIGNAL.isSignaled());
    }

    /**
     * Test of signal and isSignal methods, of class WaitableSignal.
     */
    @Test
    public void testSignal() {
        WaitableSignal signal = new WaitableSignal();
        assertFalse(signal.isSignaled());
        signal.signal();
        assertTrue(signal.isSignaled());
    }


    /**
     * Test of waitSignal method, of class WaitableSignal.
     */
    @Test(timeout = 5000)
    public void testWaitSignal() {
        WaitableSignal signal = new WaitableSignal();
        signal.signal();
        signal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test(timeout = 5000)
    public void testWaitSignalOnOtherThread() throws InterruptedException {
        final WaitableSignal signal = new WaitableSignal();

        PostActionTask signalTask = new PostActionTask(signal::signal);
        signalTask.start();
        try {
            signal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        } finally {
            signalTask.join();
        }
    }

    @Test(expected = OperationCanceledException.class, timeout = 5000)
    public void testWaitSignalPreCanceled() {
        WaitableSignal signal = new WaitableSignal();
        signal.waitSignal(Cancellation.CANCELED_TOKEN);
    }

    @Test(expected = OperationCanceledException.class, timeout = 5000)
    public void testWaitSignalPostCanceled() {
        WaitableSignal signal = new WaitableSignal();

        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        PostActionTask cancelTask = new PostActionTask(cancelSource.getController()::cancel);
        cancelTask.start();
        try {
            signal.waitSignal(cancelSource.getToken());
        } finally {
            cancelTask.join();
        }
    }

    /**
     * Test of waitSignal method, of class WaitableSignal.
     */
    @Test(timeout = 5000)
    public void testWaitSignalWithTimeout() {
        WaitableSignal signal = new WaitableSignal();
        signal.signal();
        assertTrue(signal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));
    }

    @Test(timeout = 5000)
    public void testWaitSignalOnOtherThreadWithTimeout() throws InterruptedException {
        final WaitableSignal signal = new WaitableSignal();

        PostActionTask signalTask = new PostActionTask(signal::signal);
        signalTask.start();
        try {
            boolean signaled = signal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
            assertTrue(signaled);
        } finally {
            signalTask.join();
        }
    }

    @Test(timeout = 5000)
    public void testWaitSignalWithTimeoutTimeouts() {
        WaitableSignal signal = new WaitableSignal();
        boolean signaled = signal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, 100, TimeUnit.NANOSECONDS);
        assertFalse(signaled);
    }

    @Test(expected = OperationCanceledException.class, timeout = 5000)
    public void testWaitSignalPreCanceledWithTimeout() {
        WaitableSignal signal = new WaitableSignal();
        signal.tryWaitSignal(Cancellation.CANCELED_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
    }

    @Test(expected = OperationCanceledException.class, timeout = 5000)
    public void testWaitSignalPostCanceledWithTimeout() {
        WaitableSignal signal = new WaitableSignal();

        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        PostActionTask cancelTask = new PostActionTask(cancelSource.getController()::cancel);
        cancelTask.start();
        try {
            signal.tryWaitSignal(cancelSource.getToken(), Long.MAX_VALUE, TimeUnit.DAYS);
        } finally {
            cancelTask.join();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testillegalWaitSignal() {
        WaitableSignal signal = new WaitableSignal();
        signal.waitSignal(null);
    }

    @Test(expected = NullPointerException.class)
    public void testillegalWaitSignalWithTimeout1() {
        WaitableSignal signal = new WaitableSignal();
        signal.tryWaitSignal(null, 1, TimeUnit.DAYS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testillegalWaitSignalWithTimeout2() {
        WaitableSignal signal = new WaitableSignal();
        signal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, -1, TimeUnit.DAYS);
    }

    @Test(expected = NullPointerException.class)
    public void testillegalWaitSignalWithTimeout3() {
        WaitableSignal signal = new WaitableSignal();
        signal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, 1, null);
    }

    private static class PostActionTask {
        private final Thread thread;

        public PostActionTask(final Runnable task) {
            assert task != null;
            this.thread = new Thread(() -> {
                try {
                    Thread.sleep(POST_ACTION_WAIT_MS);
                } catch (InterruptedException ex) {
                } finally {
                    task.run();
                }
            });
        }

        public void start() {
            thread.start();
        }

        public void join() {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            }
        }
    }
}
