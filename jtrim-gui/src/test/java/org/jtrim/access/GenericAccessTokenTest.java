package org.jtrim.access;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jtrim.cancel.CancelableWaits;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.InterruptibleWait;
import org.jtrim.concurrent.*;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class GenericAccessTokenTest {

    public GenericAccessTokenTest() {
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
    public void testID() {
        String testID = "TEST-TOKEN-ID";
        AccessToken<?> token = AccessTokens.createToken(testID);
        assertEquals(testID, token.getAccessID());
    }

    @Test
    public void testReleaseListenerPreRelease() {
        AccessToken<?> token = AccessTokens.createToken("");

        final AtomicInteger callCount = new AtomicInteger(0);
        token.addReleaseListener(new Runnable() {
            @Override
            public void run() {
                callCount.incrementAndGet();
            }
        });

        assertEquals(0, callCount.get());
        assertFalse(token.isReleased());
        token.release();
        assertTrue(token.isReleased());
        assertEquals(1, callCount.get());
    }

    @Test
    public void testReleaseListenerPostRelease() {
        AccessToken<?> token = AccessTokens.createToken("");

        assertFalse(token.isReleased());
        token.release();
        assertTrue(token.isReleased());

        final AtomicInteger callCount = new AtomicInteger(0);
        token.addReleaseListener(new Runnable() {
            @Override
            public void run() {
                callCount.incrementAndGet();
            }
        });
        assertEquals(1, callCount.get());
    }

    @Test
    public void testReleaseWithExecute() {
        AccessToken<?> token = AccessTokens.createToken("");

        final AtomicInteger callCount = new AtomicInteger(0);
        TaskExecutor executor = token.createExecutor(SyncTaskExecutor.getSimpleExecutor());
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                callCount.incrementAndGet();
            }
        }, null);
        assertEquals(1, callCount.get());

        token.addReleaseListener(new Runnable() {
            @Override
            public void run() {
                callCount.incrementAndGet();
            }
        });
        token.release();
        assertEquals(2, callCount.get());
    }

    @Test
    public void testReleaseConcurrentWithTask() {
        AccessToken<?> token = AccessTokens.createToken("");

        TaskExecutorService threadPool = new ThreadPoolTaskExecutor("TEST-POOL", 1);
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            TaskExecutor executor = token.createExecutor(threadPool);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    CancelableWaits.await(cancelToken, new InterruptibleWait() {
                        @Override
                        public void await() throws InterruptedException {
                            latch.await();
                        }
                    });
                }
            }, null);

            token.release();
            latch.countDown();

            token.awaitRelease(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.SECONDS);
        } finally {
            threadPool.shutdown();
        }
    }

    @Test
    public void testReleaseConcurrentWithTasks() {
        AccessToken<?> token = AccessTokens.createToken("");

        TaskExecutorService threadPool = new ThreadPoolTaskExecutor("TEST-POOL", 1);
        try {
            final CountDownLatch latch = new CountDownLatch(1);
            TaskExecutor executor = token.createExecutor(threadPool);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    CancelableWaits.await(cancelToken, new InterruptibleWait() {
                        @Override
                        public void await() throws InterruptedException {
                            latch.await();
                        }
                    });
                }
            }, null);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), null);

            token.release();
            latch.countDown();

            token.awaitRelease(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.SECONDS);
        } finally {
            threadPool.shutdown();
        }
    }
}
