package org.jtrim2.executor;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ExecutorsExTest {
    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(ExecutorsEx.class);
    }

    /**
     * Test of canceledFuture method, of class ExecutorsEx.
     */
    @Test
    public void testCanceledFuture() {
        assertSame(CanceledFuture.INSTANCE, ExecutorsEx.canceledFuture());
    }

    /**
     * Test of shutdownExecutorsNow method, of class ExecutorsEx.
     */
    @Test
    public void testShutdownExecutorsNow_ExecutorServiceArr() {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        ExecutorsEx.shutdownExecutorsNow(executor1, executor2);

        verify(executor1).shutdownNow();
        verify(executor2).shutdownNow();
        verifyNoMoreInteractions(executor1, executor2);

        ExecutorsEx.shutdownExecutorsNow();
    }

    /**
     * Test of shutdownExecutorsNow method, of class ExecutorsEx.
     */
    @Test
    public void testShutdownExecutorsNow_Collection() {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        ExecutorsEx.shutdownExecutorsNow(Arrays.asList(executor1, executor2));

        verify(executor1).shutdownNow();
        verify(executor2).shutdownNow();
        verifyNoMoreInteractions(executor1, executor2);

        ExecutorsEx.shutdownExecutorsNow(Collections.<ExecutorService>emptyList());
    }

    @Test
    public void testAwaitExecutor() throws InterruptedException {
        ExecutorService executor = mock(ExecutorService.class);

        when(executor.isTerminated()).thenReturn(false).thenReturn(true);
        when(executor.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(Boolean.TRUE);

        ExecutorsEx.awaitExecutor(executor);

        verify(executor, atLeast(2)).isTerminated();
        verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(executor);
    }

    @Test
    public void testAwaitExecutors_ExecutorServiceArr() throws InterruptedException {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        when(executor1.isTerminated()).thenReturn(false).thenReturn(true);
        when(executor1.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(Boolean.TRUE);

        when(executor2.isTerminated()).thenReturn(false).thenReturn(true);
        when(executor2.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(Boolean.TRUE);

        ExecutorsEx.awaitExecutors(executor1, executor2);

        verify(executor1, atLeast(2)).isTerminated();
        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));

        verify(executor2, atLeast(2)).isTerminated();
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));

        verifyNoMoreInteractions(executor1, executor2);

        ExecutorsEx.awaitExecutors();
    }

    @Test
    public void testAwaitExecutors_Collection() throws InterruptedException {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        when(executor1.isTerminated()).thenReturn(false).thenReturn(true);
        when(executor1.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(Boolean.TRUE);

        when(executor2.isTerminated()).thenReturn(false).thenReturn(true);
        when(executor2.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(Boolean.TRUE);

        ExecutorsEx.awaitExecutors(Arrays.asList(executor1, executor2));

        verify(executor1, atLeast(2)).isTerminated();
        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));

        verify(executor2, atLeast(2)).isTerminated();
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));

        verifyNoMoreInteractions(executor1, executor2);

        ExecutorsEx.awaitExecutors(Collections.<ExecutorService>emptyList());
    }

    @Test(timeout = 5000)
    public void testAwaitExecutors_3args_1() throws Exception {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        when(executor1.isTerminated()).thenReturn(true);
        when(executor1.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(Boolean.TRUE);

        when(executor2.isTerminated()).thenReturn(true);
        when(executor2.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(Boolean.TRUE);

        assertTrue(ExecutorsEx.awaitExecutors(5, TimeUnit.DAYS, executor1, executor2));

        verify(executor1, atLeast(1)).isTerminated();
        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));

        verify(executor2, atLeast(1)).isTerminated();
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));

        verifyNoMoreInteractions(executor1, executor2);

        assertTrue(ExecutorsEx.awaitExecutors(5, TimeUnit.DAYS));
    }

    @Test(timeout = 5000)
    public void testAwaitExecutors_3args_1Timeout() throws Exception {
        for (boolean result: Arrays.asList(false, true)) {
            ExecutorService executor = mock(ExecutorService.class);
            when(executor.isTerminated()).thenReturn(result);

            assertEquals(result, ExecutorsEx.awaitExecutors(0, TimeUnit.NANOSECONDS, executor));
        }
    }

    @Test
    public void testAwaitExecutors_3args_2() throws Exception {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        when(executor1.isTerminated()).thenReturn(true);
        when(executor1.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(Boolean.TRUE);

        when(executor2.isTerminated()).thenReturn(true);
        when(executor2.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(Boolean.TRUE);

        assertTrue(ExecutorsEx.awaitExecutors(5, TimeUnit.DAYS, Arrays.asList(executor1, executor2)));

        verify(executor1, atLeast(1)).isTerminated();
        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));

        verify(executor2, atLeast(1)).isTerminated();
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));

        verifyNoMoreInteractions(executor1, executor2);

        assertTrue(ExecutorsEx.awaitExecutors(5, TimeUnit.DAYS, Collections.<ExecutorService>emptyList()));
    }

    @Test
    public void testAwaitExecutors_3args_2Timeout() throws Exception {
        for (boolean result: Arrays.asList(false, true)) {
            ExecutorService executor = mock(ExecutorService.class);
            when(executor.isTerminated()).thenReturn(result);

            assertEquals(result, ExecutorsEx.awaitExecutors(0, TimeUnit.NANOSECONDS, Collections.singleton(executor)));
        }
    }

    /**
     * Test of asUnstoppableExecutor method, of class ExecutorsEx.
     */
    @Test
    public void testAsUnstoppableExecutor() {
        ExecutorService wrapped = mock(ExecutorService.class);
        ExecutorService executor = ExecutorsEx.asUnstoppableExecutor(wrapped);
        assertTrue(executor instanceof UnstoppableExecutor);
    }

    @Test(timeout = 10000)
    public void testNewMultiThreadedExecutor_int_boolean() throws InterruptedException {
        for (boolean daemon: Arrays.asList(false, true)) {
            ThreadPoolExecutor executor = ExecutorsEx.newMultiThreadedExecutor(10, daemon);
            try {
                assertEquals(10, executor.getMaximumPoolSize());

                final AtomicBoolean executingIsDaemon = new AtomicBoolean(!daemon);
                final CountDownLatch doneLatch = new CountDownLatch(1);
                executor.execute(() -> {
                    executingIsDaemon.set(Thread.currentThread().isDaemon());
                    doneLatch.countDown();
                });
                doneLatch.await();

                assertEquals(daemon, executingIsDaemon.get());
            } finally {
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            }
        }
    }

    @Test
    public void testNewMultiThreadedExecutor_3args_1() throws InterruptedException {
        String poolName = "ExecutorsExTest-Pool";
        for (boolean daemon: Arrays.asList(false, true)) {
            ThreadPoolExecutor executor = ExecutorsEx.newMultiThreadedExecutor(10, daemon, poolName);
            try {
                assertEquals(10, executor.getMaximumPoolSize());

                final AtomicReference<String> threadName = new AtomicReference<>(null);
                final AtomicBoolean executingIsDaemon = new AtomicBoolean(!daemon);
                final CountDownLatch doneLatch = new CountDownLatch(1);
                executor.execute(() -> {
                    threadName.set(Thread.currentThread().getName());
                    executingIsDaemon.set(Thread.currentThread().isDaemon());
                    doneLatch.countDown();
                });
                doneLatch.await();

                assertEquals(daemon, executingIsDaemon.get());
                assertTrue(threadName.get().contains(poolName));
            } finally {
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            }
        }
    }

    @Test
    public void testNewMultiThreadedExecutor_3args_2() throws InterruptedException {
        for (boolean daemon: Arrays.asList(false, true)) {
            ThreadPoolExecutor executor = ExecutorsEx.newMultiThreadedExecutor(10, 1547, daemon);
            try {
                assertEquals(10, executor.getMaximumPoolSize());
                assertEquals(1547, executor.getKeepAliveTime(TimeUnit.MILLISECONDS));

                final AtomicBoolean executingIsDaemon = new AtomicBoolean(!daemon);
                final CountDownLatch doneLatch = new CountDownLatch(1);
                executor.execute(() -> {
                    executingIsDaemon.set(Thread.currentThread().isDaemon());
                    doneLatch.countDown();
                });
                doneLatch.await();

                assertEquals(daemon, executingIsDaemon.get());
            } finally {
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            }
        }
    }

    @Test
    public void testNewMultiThreadedExecutor_4args() throws InterruptedException {
        String poolName = "ExecutorsExTest-Pool";
        for (boolean daemon: Arrays.asList(false, true)) {
            ThreadPoolExecutor executor = ExecutorsEx.newMultiThreadedExecutor(10, 1547, daemon, poolName);
            try {
                assertEquals(10, executor.getMaximumPoolSize());
                assertEquals(1547, executor.getKeepAliveTime(TimeUnit.MILLISECONDS));

                final AtomicReference<String> threadName = new AtomicReference<>(null);
                final AtomicBoolean executingIsDaemon = new AtomicBoolean(!daemon);
                final CountDownLatch doneLatch = new CountDownLatch(1);
                executor.execute(() -> {
                    threadName.set(Thread.currentThread().getName());
                    executingIsDaemon.set(Thread.currentThread().isDaemon());
                    doneLatch.countDown();
                });
                doneLatch.await();

                assertEquals(daemon, executingIsDaemon.get());
                assertTrue(threadName.get().contains(poolName));
            } finally {
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            }
        }
    }

    @Test
    public void testNewSchedulerThreadedExecutor_int_boolean() throws InterruptedException {
        for (boolean daemon: Arrays.asList(false, true)) {
            ScheduledThreadPoolExecutor executor = ExecutorsEx.newSchedulerThreadedExecutor(10, daemon);
            try {
                assertEquals(10, executor.getCorePoolSize());

                final AtomicBoolean executingIsDaemon = new AtomicBoolean(!daemon);
                final CountDownLatch doneLatch = new CountDownLatch(1);
                executor.execute(() -> {
                    executingIsDaemon.set(Thread.currentThread().isDaemon());
                    doneLatch.countDown();
                });
                doneLatch.await();

                assertEquals(daemon, executingIsDaemon.get());
            } finally {
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            }
        }
    }

    @Test
    public void testNewSchedulerThreadedExecutor_3args() throws InterruptedException {
        String poolName = "ExecutorsExTest-Pool";
        for (boolean daemon: Arrays.asList(false, true)) {
            ScheduledThreadPoolExecutor executor = ExecutorsEx.newSchedulerThreadedExecutor(10, daemon, poolName);
            try {
                assertEquals(10, executor.getCorePoolSize());

                final AtomicReference<String> threadName = new AtomicReference<>(null);
                final AtomicBoolean executingIsDaemon = new AtomicBoolean(!daemon);
                final CountDownLatch doneLatch = new CountDownLatch(1);
                executor.execute(() -> {
                    threadName.set(Thread.currentThread().getName());
                    executingIsDaemon.set(Thread.currentThread().isDaemon());
                    doneLatch.countDown();
                });
                doneLatch.await();

                assertEquals(daemon, executingIsDaemon.get());
                assertTrue(threadName.get().contains(poolName));
            } finally {
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            }
        }
    }

    @Test
    public void testNamedThreadFactory() {
        String poolName = "ExecutorsExTest-Pool";
        for (boolean daemon: Arrays.asList(false, true)) {
            assertEquals(daemon, new ExecutorsEx.NamedThreadFactory(daemon).newThread(null).isDaemon());

            Thread thread2 = new ExecutorsEx.NamedThreadFactory(daemon, poolName).newThread(null);
            assertTrue(thread2.getName().indexOf(poolName) >= 0);
            assertEquals(daemon, thread2.isDaemon());
        }
    }

    @Test
    public void testDiscardPolicyPriorShutdown() {
        Runnable task = mock(Runnable.class);

        ThreadPoolExecutor executor = ExecutorsEx.newMultiThreadedExecutor(1, false);
        try {
            RejectedExecutionHandler handler = executor.getRejectedExecutionHandler();
            try {
                handler.rejectedExecution(task, executor);
                fail("Expected RejectedExecutionException.");
            } catch (RejectedExecutionException ex) {
            }

            verifyNoInteractions(task);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testDiscardPolicyAfterShutdown() {
        Runnable task = mock(Runnable.class);

        ThreadPoolExecutor executor = ExecutorsEx.newMultiThreadedExecutor(1, false);
        executor.shutdown();

        RejectedExecutionHandler handler = executor.getRejectedExecutionHandler();
        handler.rejectedExecution(task, executor);
        verifyNoInteractions(task);
    }
}
