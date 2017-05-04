package org.jtrim2.executor;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jtrim2.utils.ExceptionHelper;
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
public class FutureResultHolderTest {

    public FutureResultHolderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        Thread.interrupted(); // clear interrupted status
    }

    @After
    public void tearDown() {
    }

    private static void checkDone(FutureResultHolder<Object> holder, Object expectedResult) throws Exception {
        assertTrue(holder.hasCompleted());
        assertTrue(holder.hasCompletedWithSuccess());
        assertFalse(holder.hasCompletedWithError());

        assertSame(expectedResult, holder.tryGetResult());
        assertSame(expectedResult, holder.waitResult());
        assertSame(expectedResult, holder.waitResult(0, TimeUnit.NANOSECONDS));
        assertSame(expectedResult, holder.waitResult(Long.MAX_VALUE, TimeUnit.DAYS));

        Future<Object> future = holder.asFuture();
        assertFalse(future.isCancelled());
        assertTrue(future.isDone());
        assertSame(expectedResult, future.get());
        assertSame(expectedResult, future.get(0, TimeUnit.NANOSECONDS));
        assertSame(expectedResult, future.get(Long.MAX_VALUE, TimeUnit.DAYS));
    }

    private static void checkError(FutureResultHolder<Object> holder, Throwable error) throws Exception {
        assertTrue(holder.hasCompleted());
        assertFalse(holder.hasCompletedWithSuccess());
        assertTrue(holder.hasCompletedWithError());

        try {
            holder.tryGetResult();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertSame(error, ex.getCause());
        }
        try {
            holder.waitResult();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertSame(error, ex.getCause());
        }
        try {
            holder.waitResult(0, TimeUnit.NANOSECONDS);
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertSame(error, ex.getCause());
        }
        try {
            holder.waitResult(Long.MAX_VALUE, TimeUnit.DAYS);
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertSame(error, ex.getCause());
        }

        Future<Object> future = holder.asFuture();
        assertFalse(future.isCancelled());
        assertTrue(future.isDone());

        try {
            future.get();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertSame(error, ex.getCause());
        }
        try {
            future.get(0, TimeUnit.NANOSECONDS);
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertSame(error, ex.getCause());
        }
        try {
            future.get(Long.MAX_VALUE, TimeUnit.DAYS);
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertSame(error, ex.getCause());
        }
    }

    private static void checkCanceled(FutureResultHolder<Object> holder) throws Exception {
        assertTrue(holder.hasCompleted());
        assertFalse(holder.hasCompletedWithSuccess());
        assertTrue(holder.hasCompletedWithError());

        try {
            holder.tryGetResult();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof CancellationException);
        }
        try {
            holder.waitResult();
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof CancellationException);
        }
        try {
            holder.waitResult(0, TimeUnit.NANOSECONDS);
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof CancellationException);
        }
        try {
            holder.waitResult(Long.MAX_VALUE, TimeUnit.DAYS);
            fail("Exception expected");
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof CancellationException);
        }

        Future<Object> future = holder.asFuture();
        assertTrue(future.isCancelled());
        assertTrue(future.isDone());

        try {
            future.get();
            fail("Exception expected");
        } catch (CancellationException ex) {
        }
        try {
            future.get(0, TimeUnit.NANOSECONDS);
            fail("Exception expected");
        } catch (CancellationException ex) {
        }
        try {
            future.get(Long.MAX_VALUE, TimeUnit.DAYS);
            fail("Exception expected");
        } catch (CancellationException ex) {
        }
    }

    @Test(timeout = 5000)
    public void testGetTimeout() throws Exception {
        FutureResultHolder<Object> holder = new FutureResultHolder<>();
        assertNull(holder.waitResult(100, TimeUnit.NANOSECONDS));

        try {
            holder.asFuture().get(100, TimeUnit.NANOSECONDS);
            fail("Timeout expected");
        } catch (TimeoutException ex) {
        }
    }

    @Test(timeout = 5000)
    public void testBeforeStore() throws Exception {
        FutureResultHolder<Object> holder = new FutureResultHolder<>();
        assertFalse(holder.hasCompleted());
        assertFalse(holder.hasCompletedWithError());
        assertFalse(holder.hasCompletedWithSuccess());
        assertNull(holder.tryGetResult());

        assertFalse(holder.asFuture().isCancelled());
        assertFalse(holder.asFuture().isDone());
    }

    @Test(timeout = 5000)
    public void testStore() throws Exception {
        Object result = new Object();
        FutureResultHolder<Object> holder = new FutureResultHolder<>();

        assertTrue(holder.tryStoreResult(result));
        checkDone(holder, result);

        assertFalse(holder.tryStoreResult(new Object()));
        checkDone(holder, result);

        assertFalse(holder.trySetError(new Exception()));
        checkDone(holder, result);
    }

    @Test(timeout = 10000)
    public void testException() throws Exception {
        Exception exception = new Exception("TEST-EXCEPTION");
        FutureResultHolder<Object> holder = new FutureResultHolder<>();

        assertTrue(holder.trySetError(exception));
        checkError(holder, exception);

        assertFalse(holder.tryStoreResult(new Object()));
        checkError(holder, exception);

        assertFalse(holder.trySetError(new Exception()));
        checkError(holder, exception);
    }

    @Test(timeout = 10000)
    public void testCancel() throws Exception {
        FutureResultHolder<Object> holder = new FutureResultHolder<>();

        assertTrue(holder.trySetError(new CancellationException()));
        checkCanceled(holder);

        assertFalse(holder.tryStoreResult(new Object()));
        checkCanceled(holder);

        assertFalse(holder.trySetError(new Exception()));
        checkCanceled(holder);
    }

    @Test(timeout = 10000)
    public void testCancelThroughFuture() throws Exception {
        FutureResultHolder<Object> holder = new FutureResultHolder<>();

        holder.asFuture().cancel(true);
        checkCanceled(holder);

        assertFalse(holder.tryStoreResult(new Object()));
        checkCanceled(holder);

        assertFalse(holder.trySetError(new Exception()));
        checkCanceled(holder);
    }

    @Test(timeout = 10000)
    public void testConcurrentStore1() throws Exception {
        final Object result = new Object();
        final FutureResultHolder<Object> holder = new FutureResultHolder<>();

        Thread storeThread = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            } finally {
                holder.tryStoreResult(result);
            }
        });
        storeThread.start();
        try {
            assertSame(result, holder.waitResult());
        } finally {
            storeThread.interrupt();
            storeThread.join();
        }
        checkDone(holder, result);
    }

    @Test(timeout = 10000)
    public void testConcurrentStore2() throws Exception {
        final Object result = new Object();
        final FutureResultHolder<Object> holder = new FutureResultHolder<>();

        Thread storeThread = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            } finally {
                holder.tryStoreResult(result);
            }
        });
        storeThread.start();
        try {
            assertSame(result, holder.waitResult(Long.MAX_VALUE, TimeUnit.DAYS));
        } finally {
            storeThread.interrupt();
            storeThread.join();
        }
        checkDone(holder, result);
    }

    @Test(timeout = 20000)
    public void testConcurrentStore3() throws Exception {
        // This test starts two threads where both of them set the result.
        // They are synchronized to increase the chance of actually setting
        // the result concurrently.

        for (int testIndex = 0; testIndex < 1000; testIndex++) {
            final int threadCount = 2;
            final Object result = new Object();
            final FutureResultHolder<Object> holder = new FutureResultHolder<>();

            final CountDownLatch storeLatch = new CountDownLatch(threadCount);
            Runnable storeTask = () -> {
                try {
                    storeLatch.countDown();
                    storeLatch.await();
                } catch (InterruptedException ex) {
                } finally {
                    holder.tryStoreResult(result);
                }
            };
            Thread[] storeThreads = new Thread[threadCount];
            for (int i = 0; i < storeThreads.length; i++) {
                storeThreads[i] = new Thread(storeTask);
            }
            try {
                for (int i = 0; i < storeThreads.length; i++) {
                    storeThreads[i].start();
                }

                assertSame(result, holder.waitResult());
            } finally {
                Throwable toThrow = null;
                for (int i = 0; i < storeThreads.length; i++) {
                    try {
                        storeThreads[i].interrupt();
                        storeThreads[i].join();
                    } catch (Throwable ex) {
                        toThrow = ex;
                    }
                }

                ExceptionHelper.rethrowIfNotNull(toThrow);
            }
            checkDone(holder, result);
        }
    }

    @Test(timeout = 10000, expected = InterruptedException.class)
    public void testInterruptWait1() throws Exception {
        FutureResultHolder<Object> holder = new FutureResultHolder<>();

        Thread.currentThread().interrupt();
        holder.waitResult();
    }

    @Test(timeout = 10000, expected = InterruptedException.class)
    public void testInterruptWait2() throws Exception {
        FutureResultHolder<Object> holder = new FutureResultHolder<>();

        Thread.currentThread().interrupt();
        holder.waitResult(Long.MAX_VALUE, TimeUnit.DAYS);
    }
}
