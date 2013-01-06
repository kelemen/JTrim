package org.jtrim.concurrent.async;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
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
public class InitLaterDataControllerTest {

    public InitLaterDataControllerTest() {
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
    public void testStateBeforeInit1() {
        assertNull(new InitLaterDataController().getDataState());
    }

    @Test
    public void testStateBeforeInit2() {
        AsyncDataState initialState = mock(AsyncDataState.class);
        assertSame(initialState, new InitLaterDataController(initialState).getDataState());
    }

    @Test
    public void testStateAfterInit() {
        AsyncDataState state = mock(AsyncDataState.class);
        AsyncDataController wrappedController = mock(AsyncDataController.class);

        stub(wrappedController.getDataState()).toReturn(state);

        InitLaterDataController controller = new InitLaterDataController();
        controller.initController(wrappedController);
        assertSame(state, controller.getDataState());
    }

    @Test
    public void testControlArgForwarding() {
        ArgCollectorController wrappedController = new ArgCollectorController();

        int preInitArgCount = 3;
        int postInitArgCount = 4;
        Object[] args = new Object[preInitArgCount + postInitArgCount];

        InitLaterDataController controller = new InitLaterDataController();
        for (int i = 0; i < preInitArgCount; i++) {
            args[i] = new Object();
            controller.controlData(args[i]);
        }

        controller.initController(wrappedController);

        for (int i = preInitArgCount; i < args.length; i++) {
            args[i] = new Object();
            controller.controlData(args[i]);
        }

        assertArrayEquals(args, wrappedController.getControlArgs());
    }

    @Test(expected = IllegalStateException.class)
    public void testMultipleInitAttempt() {
        InitLaterDataController controller = new InitLaterDataController();
        controller.initController(mock(AsyncDataController.class));
        controller.initController(mock(AsyncDataController.class));
    }

    private void testConcurrentForward(int forwardThreadCount) throws Throwable {
        AsyncDataState state = mock(AsyncDataState.class);
        final ArgCollectorController wrappedController = new ArgCollectorController(state);
        final InitLaterDataController controller = new InitLaterDataController();

        final Object[] args = new Object[forwardThreadCount];
        for (int i = 0; i < args.length; i++) {
            args[i] = new Object();
        }

        final CountDownLatch syncLatch = new CountDownLatch(forwardThreadCount + 1);

        Thread initThread = new Thread(new Runnable() {
            @Override
            public void run() {
                syncLatch.countDown();
                try {
                    syncLatch.await();
                } catch (InterruptedException ex) {
                }

                controller.initController(wrappedController);
            }
        });
        Thread[] forwardThreads = new Thread[forwardThreadCount];
        final Object[] requestedStates = new Object[forwardThreads.length];

        // Fill this array to random values because null object will be a valid
        // value after the threads complete.
        for (int i = 0; i < requestedStates.length; i++) {
            requestedStates[i] = new Object();
        }

        for (int i = 0; i < forwardThreads.length; i++) {
            final int argIndex = i;
            forwardThreads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    syncLatch.countDown();
                    try {
                        syncLatch.await();
                    } catch (InterruptedException ex) {
                    }

                    controller.controlData(args[argIndex]);
                    requestedStates[argIndex] = controller.getDataState();
                }
            });
        }

        Throwable lastError = null;
        try {
            initThread.start();
            for (Thread thread: forwardThreads) {
                thread.start();
            }
        } finally {
            try {
                initThread.join();
            } catch (Throwable ex) {
                lastError = ex;
            }

            for (Thread thread: forwardThreads) {
                try {
                    thread.join();
                } catch (Throwable ex) {
                    lastError = ex;
                }
            }
        }

        if (lastError != null) {
            throw lastError;
        }

        for (Object requestedState: requestedStates) {
            assertTrue("The state must be either null or the state after initialization.",
                    requestedState == null || requestedState == state);
        }

        assertSame(state, controller.getDataState());

        Set<?> expected = new HashSet<>(Arrays.asList(args));
        Set<?> received = new HashSet<>(Arrays.asList(wrappedController.getControlArgs()));

        assertEquals(expected, received);
    }

    @Test(timeout = 30000)
    public void testConcurrentForwardAndInit() throws Throwable {
        int testCount = 100;
        for (int i = 0; i < testCount; i++) {
            testConcurrentForward(1);
        }
    }

    @Test(timeout = 30000)
    public void testConcurrentForward() throws Throwable {
        int forwardThreadCount = Runtime.getRuntime().availableProcessors() * 2;
        int testCount = forwardThreadCount * 100;

        for (int i = 0; i < testCount; i++) {
            testConcurrentForward(forwardThreadCount);
        }
    }

    /**
     * Test of toString method, of class InitLaterDataController.
     */
    @Test
    public void testToString() {
        assertNotNull(new InitLaterDataController().toString());
    }

    private static class ArgCollectorController implements AsyncDataController {
        private final AsyncDataState state;
        private final Queue<Object> argQueue;

        public ArgCollectorController() {
            this(null);
        }

        public ArgCollectorController(AsyncDataState state) {
            this.state = state;
            this.argQueue = new ConcurrentLinkedQueue<>();
        }

        public Object[] getControlArgs() {
            return argQueue.toArray();
        }

        @Override
        public void controlData(Object controlArg) {
            argQueue.add(controlArg);
        }

        @Override
        public AsyncDataState getDataState() {
            return state;
        }
    }
}