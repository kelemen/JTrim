package org.jtrim.cancel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.concurrent.TaskExecutionException;
import org.junit.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

    @Test(timeout = 20000)
    public void testListenForCancellationIllegalState() {
        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        final AtomicReference<WaitableListenerRef> refRef = new AtomicReference<>(null);
        final AtomicReference<Throwable> exceptionRef = new AtomicReference<>(null);
        WaitableListenerRef ref = Cancellation.listenForCancellation(cancelSource.getToken(), new Runnable() {
            @Override
            public void run() {
                try {
                    refRef.get().unregisterAndWait(Cancellation.UNCANCELABLE_TOKEN);
                } catch (Throwable ex) {
                    exceptionRef.set(ex);
                }
            }
        });
        refRef.set(ref);
        cancelSource.getController().cancel();
        assertTrue("Expected IllegalStateException: " + exceptionRef.get().getClass().getName(),
                exceptionRef.get() instanceof IllegalStateException);
    }

    @Test(timeout = 20000)
    public void testListenForCancellation1() {
        Runnable listener = mock(Runnable.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        WaitableListenerRef ref = Cancellation.listenForCancellation(cancelSource.getToken(), listener);
        assertTrue(ref.isRegistered());
        cancelSource.getController().cancel();
        ref.unregisterAndWait(Cancellation.UNCANCELABLE_TOKEN);

        verify(listener).run();
    }

    @Test(timeout = 20000)
    public void testListenForCancellationAfterClose1() {
        Runnable listener = mock(Runnable.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        final WaitableListenerRef ref = Cancellation.listenForCancellation(cancelSource.getToken(), listener);
        ref.unregisterAndWait(Cancellation.UNCANCELABLE_TOKEN);
        cancelSource.getController().cancel();

        verifyZeroInteractions(listener);
    }

    @Test(timeout = 20000)
    public void testListenForCancellation2() {
        Runnable listener = mock(Runnable.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        WaitableListenerRef ref = Cancellation.listenForCancellation(cancelSource.getToken(), listener);
        cancelSource.getController().cancel();
        ref.unregister();
        assertFalse(ref.isRegistered());

        verify(listener).run();
    }

    @Test(timeout = 20000)
    public void testListenForCancellationAfterClose2() {
        Runnable listener = mock(Runnable.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        final WaitableListenerRef ref = Cancellation.listenForCancellation(cancelSource.getToken(), listener);
        ref.unregister();
        assertFalse(ref.isRegistered());
        cancelSource.getController().cancel();

        verifyZeroInteractions(listener);
    }

    @Test(timeout = 20000)
    public void testListenForCancellationConcurrent() {
        for (int i = 0; i < 100; i++) {
            final AtomicInteger counter = new AtomicInteger(0);
            final CancellationSource cancelSource = Cancellation.createCancellationSource();

            final WaitableListenerRef ref = Cancellation.listenForCancellation(cancelSource.getToken(), new Runnable() {
                @Override
                public void run() {
                    counter.incrementAndGet();
                }
            });

            Runnable cancelTask = new Runnable() {
                @Override
                public void run() {
                    cancelSource.getController().cancel();
                }
            };
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    ref.unregisterAndWait(Cancellation.UNCANCELABLE_TOKEN);
                }
            };
            runConcurrently(cancelTask, closeTask);
            int value1 = counter.get();
            // This method call is here only to allow some time to pass before the
            // second counter.get()
            cancelSource.getController().cancel();
            assertEquals(value1, counter.get());
        }
    }

    private static void runConcurrently(Runnable... tasks) {
        final CountDownLatch latch = new CountDownLatch(tasks.length);
        Thread[] threads = new Thread[tasks.length];
        final Throwable[] exceptions = new Throwable[tasks.length];

        for (int i = 0; i < threads.length; i++) {
            final Runnable task = tasks[i];
            final int threadIndex = i;
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        latch.countDown();
                        latch.await();

                        task.run();
                    } catch (Throwable ex) {
                        exceptions[threadIndex] = ex;
                    }
                }
            });
        }

        try {
            for (int i = 0; i < threads.length; i++) {
                threads[i].start();
            }
        } finally {
            boolean interrupted = false;
            for (int i = 0; i < threads.length; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException ex) {
                    interrupted = true;
                }
            }

            if (interrupted) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Unexpected interrupt.");
            }
        }

        TaskExecutionException toThrow = null;
        for (int i = 0; i < exceptions.length; i++) {
            Throwable current = exceptions[i];
            if (current != null) {
                if (toThrow == null) toThrow = new TaskExecutionException(current);
                else toThrow.addSuppressed(current);
            }
        }
        if (toThrow != null) {
            throw toThrow;
        }
    }
}
