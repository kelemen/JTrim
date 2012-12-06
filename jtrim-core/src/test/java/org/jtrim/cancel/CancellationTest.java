package org.jtrim.cancel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class CancellationTest {

    public CancellationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        // Clear the interrupted status of the current thread before executing
        // any of the test methods.
        Thread.interrupted();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testAnyToken() {
        CancellationToken token = Cancellation.anyToken(Cancellation.CANCELED_TOKEN);
        assertTrue(token instanceof CombinedTokenAny);
        assertTrue(token.isCanceled());
    }

    @Test
    public void testAllToken() {
        CancellationToken token = Cancellation.allTokens(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue(token instanceof CombinedTokenAll);
        assertFalse(token.isCanceled());
    }

    @Test
    public void testUncancelableToken() {
        assertFalse(Cancellation.UNCANCELABLE_TOKEN.isCanceled());

        final AtomicInteger invoked = new AtomicInteger(0);
        Cancellation.UNCANCELABLE_TOKEN.addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        assertEquals(0, invoked.get());

        Cancellation.UNCANCELABLE_TOKEN.checkCanceled();
    }

    @Test
    public void testCanceledToken() {
        assertTrue(Cancellation.CANCELED_TOKEN.isCanceled());

        final AtomicInteger invoked = new AtomicInteger(0);
        Cancellation.CANCELED_TOKEN.addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        assertEquals(1, invoked.get());
    }

    @Test(expected = OperationCanceledException.class)
    public void testCanceledTokenCheckCancel() {
        Cancellation.CANCELED_TOKEN.checkCanceled();
    }

    @Test
    public void testDoNothingController() {
        Cancellation.DO_NOTHING_CONTROLLER.cancel();
    }

    @Test
    public void testDoAsCancelableNormal() {
        final AtomicReference<Thread> executingThread = new AtomicReference<>(null);
        Integer result = Cancellation.doAsCancelable(Cancellation.CANCELED_TOKEN, new InterruptibleTask<Integer>() {
            @Override
            public Integer execute(CancellationToken cancelToken) {
                executingThread.set(Thread.currentThread());
                return 100;
            }
        });
        assertSame(Thread.currentThread(), executingThread.get());
        assertEquals(100, result.intValue());
    }

    @Test
    public void testDoAsCancelablePreCanceled() {
        Cancellation.doAsCancelable(Cancellation.CANCELED_TOKEN, new InterruptibleTask<Void>() {
            @Override
            public Void execute(CancellationToken cancelToken) throws InterruptedException {
                return null;
            }
        });

        assertTrue(Thread.interrupted());
    }

    @Test
    public void testDoAsCancelableCanceledInTask() {
        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        Cancellation.doAsCancelable(cancelSource.getToken(), new InterruptibleTask<Void>() {
            @Override
            public Void execute(CancellationToken cancelToken) {
                assertFalse(Thread.currentThread().isInterrupted());
                cancelSource.getController().cancel();
                assertTrue(Thread.currentThread().isInterrupted());
                return null;
            }
        });

        assertTrue(Thread.interrupted());
    }

    @Test(expected = OperationCanceledException.class)
    public void testDoAsCancelableTaskThrowsInterrupt() {
        Cancellation.doAsCancelable(Cancellation.UNCANCELABLE_TOKEN, new InterruptibleTask<Void>() {
            @Override
            public Void execute(CancellationToken cancelToken) throws InterruptedException {
                throw new InterruptedException();
            }
        });
    }
}
