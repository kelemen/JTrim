package org.jtrim2.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.cancel.TestCancellationSource;
import org.jtrim2.utils.ExceptionHelper;
import org.jtrim2.utils.LogCollector;
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
public class SingleThreadedExecutorTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        // Clear interrupt because JUnit preserves the interrupted status
        // between tests.
        Thread.interrupted();
    }

    @After
    public void tearDown() {
    }

    // Waits until the specified executor terminates and tests
    // if the terminate listener has been called.
    private void waitTerminateAndTest(final TaskExecutorService executor) throws InterruptedException {
        final CountDownLatch listener1Latch = new CountDownLatch(1);
        executor.addTerminateListener(listener1Latch::countDown);
        executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue(executor.isTerminated());
        listener1Latch.await();

        final AtomicReference<Thread> callingThread = new AtomicReference<>(null);
        executor.addTerminateListener(() -> {
            callingThread.set(Thread.currentThread());
        });
        assertSame(Thread.currentThread(), callingThread.get());
    }

    @Test(timeout = 60000)
    public void testShutdownAllowsPreviouslySubmittedTasks1() throws InterruptedException {
        BackgroundExecutorTests.testShutdownAllowsPreviouslySubmittedTasks1(Factory.INSTANCE);
    }

    @Test(timeout = 60000)
    public void testShutdownAllowsPreviouslySubmittedTasks2() throws InterruptedException {
        BackgroundExecutorTests.testShutdownAllowsPreviouslySubmittedTasks2(Factory.INSTANCE);
    }

    @Test(timeout = 60000)
    public void testShutdownAllowsPreviouslySubmittedTasks3() throws InterruptedException {
        BackgroundExecutorTests.testShutdownAllowsPreviouslySubmittedTasks3(Factory.INSTANCE);
    }

    @Test(timeout = 60000)
    public void testShutdownAllowsPreviouslySubmittedTasks4() throws InterruptedException {
        BackgroundExecutorTests.testShutdownAllowsPreviouslySubmittedTasks4(Factory.INSTANCE);
    }

    @Test(timeout = 60000)
    public void testShutdownAllowsPreviouslySubmittedCleanupTasks1() throws InterruptedException {
        BackgroundExecutorTests.testShutdownAllowsPreviouslySubmittedCleanupTasks1(Factory.INSTANCE);
    }

    @Test(timeout = 60000)
    public void testShutdownAllowsPreviouslySubmittedCleanupTasks2() throws InterruptedException {
        BackgroundExecutorTests.testShutdownAllowsPreviouslySubmittedCleanupTasks2(Factory.INSTANCE);
    }

    @Test(timeout = 60000)
    public void testShutdownAllowsPreviouslySubmittedCleanupTasks3() throws InterruptedException {
        BackgroundExecutorTests.testShutdownAllowsPreviouslySubmittedCleanupTasks3(Factory.INSTANCE);
    }

    @Test(timeout = 60000)
    public void testShutdownAllowsPreviouslySubmittedCleanupTasks4() throws InterruptedException {
        BackgroundExecutorTests.testShutdownAllowsPreviouslySubmittedCleanupTasks4(Factory.INSTANCE);
    }

    @Test(timeout = 20000)
    public void testInterruptDoesntBreakExecutor() throws Exception {
        BackgroundExecutorTests.testInterruptDoesntBreakExecutor(Factory.INSTANCE);
    }

    @Test(timeout = 5000)
    public void testSubmitTaskNoCleanup() throws InterruptedException {
        BackgroundExecutorTests.testSubmitTaskNoCleanup(Factory.INSTANCE);
    }

    @Test(timeout = 5000)
    public void testSubmitTaskWithCleanup() throws InterruptedException {
        BackgroundExecutorTests.testSubmitTaskWithCleanup(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testShutdownWithCleanups() {
        BackgroundExecutorTests.testShutdownWithCleanups(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testDoesntTerminateBeforeTaskCompletes1() throws Exception {
        BackgroundExecutorTests.testDoesntTerminateBeforeTaskCompletes1(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testDoesntTerminateBeforeTaskCompletes2() throws Exception {
        BackgroundExecutorTests.testDoesntTerminateBeforeTaskCompletes2(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testCanceledShutdownWithCleanups() throws Exception {
        BackgroundExecutorTests.testCanceledShutdownWithCleanups(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testCancellationWithCleanups() {
        BackgroundExecutorTests.testCancellationWithCleanups(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testContextAwarenessInTask() throws InterruptedException {
        BackgroundExecutorTests.testContextAwarenessInTask(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testContextAwarenessInCleanup() throws InterruptedException {
        BackgroundExecutorTests.testContextAwarenessInCleanup(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testToString() {
        BackgroundExecutorTests.testToString(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testShutdownAndCancel() throws Exception {
        BackgroundExecutorTests.testShutdownAndCancel(Factory.INSTANCE);
    }

    @Test(timeout = 5000)
    public void testAwaitTerminationTimeout() {
        BackgroundExecutorTests.testAwaitTerminationTimeout(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testPlainTaskWithError() throws Exception {
        BackgroundExecutorTests.testPlainTaskWithError(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testSubmitAfterShutdown() throws Exception {
        BackgroundExecutorTests.testSubmitTasksAfterShutdown(Factory.INSTANCE);
    }

    @Test(timeout = 30000)
    public void testTerminatedAfterAwaitTermination() {
        BackgroundExecutorTests.testTerminatedAfterAwaitTermination(Factory.INSTANCE);
    }

    @Test(timeout = 10000)
    public void testAllowedConcurrency() throws Exception {
        final int secondPhaseNoCleanupCount = 10;
        final int secondPhaseWithCleanupCount = 10;

        final AtomicInteger executedTasks = new AtomicInteger(0);

        CleanupTask secondPhaseCleanup = mock(CleanupTask.class);
        final TestCancellationSource secondPhaseCancel = newCancellationSource();
        TaskExecutorService executor = new SingleThreadedExecutor("TEST-POOL");
        try {
            final CountDownLatch phase1Latch = new CountDownLatch(1);
            final CountDownLatch phase2Latch = new CountDownLatch(1);

            executor.submit(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                try {
                    phase1Latch.countDown();
                    phase1Latch.await();

                    phase2Latch.await();
                    secondPhaseCancel.getController().cancel();
                    executedTasks.incrementAndGet();
                } catch (InterruptedException ex) {
                    Thread.interrupted();
                }
            }, null);

            for (int i = 0; i < secondPhaseNoCleanupCount; i++) {
                executor.submit(secondPhaseCancel.getToken(), (CancellationToken cancelToken) -> {
                    executedTasks.incrementAndGet();
                }, null);
            }
            for (int i = 0; i < secondPhaseWithCleanupCount; i++) {
                executor.submit(secondPhaseCancel.getToken(),
                        Tasks.noOpCancelableTask(), secondPhaseCleanup);
            }
            phase2Latch.countDown();
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
        assertEquals(1, executedTasks.get());
        verify(secondPhaseCleanup, times(secondPhaseWithCleanupCount)).cleanup(true, null);
        secondPhaseCancel.checkNoRegistration();
    }

    private void createUnreferenced(Runnable shutdownTask, boolean needShutdown) {
        SingleThreadedExecutor executor = new SingleThreadedExecutor(
                "unreferenced-pool",
                Integer.MAX_VALUE,
                Long.MAX_VALUE,
                TimeUnit.NANOSECONDS);
        if (!needShutdown) {
            executor.dontNeedShutdown();
        }

        executor.addTerminateListener(shutdownTask);

        // To ensure that a thread is started.
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
        }, null);
    }

    /**
     * Tests if SingleThreadedExecutor automatically shutdowns itself when
     * no longer referenced. Note that it is still an error to forget to
     * shutdown a ThreadPoolTaskExecutor.
     */
    @Test(timeout = 10000)
    public void testAutoFinalize() {
        final WaitableSignal shutdownSignal = new WaitableSignal();
        createUnreferenced(shutdownSignal::signal, true);
        System.gc();
        System.gc();
        Runtime.getRuntime().runFinalization();
        System.gc();
        System.gc();
        Runtime.getRuntime().runFinalization();
        shutdownSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
    }

    /**
     * Tests that SingleThreadedExecutor does not automatically shutdowns itself
     * when no longer referenced and marked as not required to be shutted down.
     */
    @Test(timeout = 10000)
    public void testNotAutoFinalize() {
        final WaitableSignal shutdownSignal = new WaitableSignal();
        createUnreferenced(shutdownSignal::signal, false);
        System.gc();
        System.gc();
        Runtime.getRuntime().runFinalization();
        assertFalse(shutdownSignal.isSignaled());
    }

    private static void submitTaskAndWait(
            TaskExecutor executor,
            final CancelableTask postTask) throws InterruptedException {

        final CountDownLatch executedLatch = new CountDownLatch(1);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            executedLatch.countDown();

            if (postTask != null) {
                postTask.execute(cancelToken);
            }
        }, null);
        executedLatch.await();
    }

    private void doTestGoingIdle(
            TimeoutChangeType timeoutChange,
            final boolean doThreadInterrupts) throws InterruptedException {

        final SingleThreadedExecutor executor = new SingleThreadedExecutor(
                "", Integer.MAX_VALUE, 60, TimeUnit.SECONDS);
        try {
            submitTaskAndWait(executor, (CancellationToken cancelToken) -> {
                if (doThreadInterrupts) {
                    Thread.currentThread().interrupt();
                }
            });

            Thread.sleep(10);

            // Now the task have been executed and it is very likely
            // that the started thread is in idle wait.

            switch (timeoutChange) {
                case NO_CHANGE:
                    // Do nothing
                    break;
                case INCREASE:
                    executor.setIdleTimeout(Long.MAX_VALUE, TimeUnit.DAYS);
                    break;
                case DECREASE:
                    executor.setIdleTimeout(30, TimeUnit.SECONDS);
                    break;
                case ZERO_TIMEOUT:
                    executor.setIdleTimeout(0, TimeUnit.NANOSECONDS);
                    break;
                default:
                    throw new AssertionError(timeoutChange.name());
            }

            final CountDownLatch afterIdleExecuted = new CountDownLatch(1);

            final AtomicInteger executeCounts = new AtomicInteger(0);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                executeCounts.incrementAndGet();
                afterIdleExecuted.countDown();
            }, null);

            afterIdleExecuted.await();

            assertEquals(1, executeCounts.get());
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 20000)
    public void testGoingIdle() throws InterruptedException {
        for (boolean doThreadInterrupts: Arrays.asList(false, true)) {
            doTestGoingIdle(TimeoutChangeType.NO_CHANGE, doThreadInterrupts);
            doTestGoingIdle(TimeoutChangeType.INCREASE, doThreadInterrupts);
            doTestGoingIdle(TimeoutChangeType.DECREASE, doThreadInterrupts);
            doTestGoingIdle(TimeoutChangeType.ZERO_TIMEOUT, doThreadInterrupts);
        }
    }

    @Test(timeout = 10000)
    public void testQueuedTasks() throws Exception {
        int maxQueueSize = 2;
        SingleThreadedExecutor executor = new SingleThreadedExecutor("", maxQueueSize);
        final WaitableSignal releaseSignal = new WaitableSignal();

        try {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                releaseSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            }, null);

            // Fill the queue
            CancelableTask[] queueTasks = new CancelableTask[maxQueueSize + 1];
            CleanupTask[] queueCleanups = new CleanupTask[maxQueueSize + 1];

            for (int i = 0; i < maxQueueSize; i++) {
                queueTasks[i] = mock(CancelableTask.class);
                queueCleanups[i] = mock(CleanupTask.class);

                executor.execute(Cancellation.UNCANCELABLE_TOKEN, queueTasks[i], queueCleanups[i]);
            }

            // Now try to submit a task and cancel it before it is added to the
            // queue.
            CancelableTask canceledTask = mock(CancelableTask.class);

            final TestCancellationSource cancelSource = newCancellationSource();
            final Thread cancelThread = new Thread(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    // Terminate thread
                } finally {
                    cancelSource.getController().cancel();
                }
            });
            cancelThread.start();
            try {
                executor.execute(cancelSource.getToken(), canceledTask, null);
            } finally {
                cancelThread.interrupt();
                cancelThread.join();
            }

            verifyZeroInteractions(canceledTask);

            // Now increase the maximum queue size and verify that it can be
            // submitted without blocking
            executor.setMaxQueueSize(maxQueueSize + 1);

            queueTasks[maxQueueSize] = mock(CancelableTask.class);
            queueCleanups[maxQueueSize] = mock(CleanupTask.class);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN,
                    queueTasks[maxQueueSize],
                    queueCleanups[maxQueueSize]);

            executor.setMaxQueueSize(maxQueueSize);
            // decrease the maximum queue size to see that the executor still
            // remains functional.

            // Now submit another one but do not cancel this one
            CancelableTask blockedTask = mock(CancelableTask.class);
            CleanupTask blockedCleanup = mock(CleanupTask.class);

            final Thread unblockThread = new Thread(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                    // Terminate thread
                } finally {
                    releaseSignal.signal();
                }
            });
            unblockThread.start();
            try {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, blockedTask, blockedCleanup);
            } finally {
                unblockThread.interrupt();
                unblockThread.join();
            }

            // Now wait for all the tasks to complete and verify that they were
            // executed.
            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);

            verify(blockedTask).execute(any(CancellationToken.class));
            verify(blockedCleanup).cleanup(false, null);
            verifyNoMoreInteractions(blockedTask, blockedCleanup);

            for (int i = 0; i < queueTasks.length; i++) {
                CancelableTask task = queueTasks[i];
                CleanupTask cleanup = queueCleanups[i];

                verify(task).execute(any(CancellationToken.class));
                verify(cleanup).cleanup(false, null);
                verifyNoMoreInteractions(task, cleanup);
            }

        } finally {
            releaseSignal.signal();
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 5000)
    public void testThreadFactory() throws Exception {
        final Runnable startThreadMock = mock(Runnable.class);
        final WaitableSignal endThreadSignal = new WaitableSignal();

        SingleThreadedExecutor executor = new SingleThreadedExecutor("TEST-POOL");
        try {
            ThreadFactory threadFactory = (final Runnable r) -> {
                ExceptionHelper.checkNotNullArgument(r, "r");

                return new Thread(() -> {
                    startThreadMock.run();
                    r.run();
                    endThreadSignal.signal();
                });
            };
            executor.setThreadFactory(threadFactory);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), null);
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }

        verify(startThreadMock).run();
        endThreadSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test(timeout = 5000)
    public void testThreadFactoryCallsWorker() throws Exception {
        SingleThreadedExecutor executor = new SingleThreadedExecutor("TEST-POOL");
        try (LogCollector logs = LogTests.startCollecting()) {
            final Runnable exceptionOk = mock(Runnable.class);
            ThreadFactory threadFactory = (final Runnable r) -> {
                try {
                    r.run();
                    fail("Expected: IllegalStateException");
                } catch (IllegalStateException ex) {
                    exceptionOk.run();
                }
                return new Thread(r);
            };
            executor.setThreadFactory(threadFactory);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), null);

            verify(exceptionOk).run();
            assertEquals(1, logs.getNumberOfLogs(Level.SEVERE));
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 5000)
    public void testThreadFactoryMultipleWorkerRun() throws Exception {
        final WaitableSignal exceptionOk = new WaitableSignal();

        SingleThreadedExecutor executor = new SingleThreadedExecutor("TEST-POOL");
        try (LogCollector logs = LogTests.startCollecting()) {
            try {
                ThreadFactory threadFactory = (Runnable r) -> new Thread(() -> {
                    r.run();
                    try {
                        r.run();
                        fail("Expected: IllegalStateException");
                    } catch (IllegalStateException ex) {
                        exceptionOk.signal();
                    }
                });
                executor.setThreadFactory(threadFactory);
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), null);
            } finally {
                executor.shutdown();
                waitTerminateAndTest(executor);
            }

            exceptionOk.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            assertEquals(1, logs.getNumberOfLogs(Level.SEVERE));
        }
    }

    @Test(timeout = 50000)
    public void testRecoverFromThreadFactoryException() throws Exception {
        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        SingleThreadedExecutor executor = new SingleThreadedExecutor("TEST-POOL");
        try {
            executor.setThreadFactory((final Runnable r) -> {
                throw new TestException();
            });
            try {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1, null);
            } catch (TestException ex) {
                // This is expected because the factory throws this exception.
            }

            executor.setThreadFactory(new ExecutorsEx.NamedThreadFactory(false));
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2, null);
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }

        verify(task1).execute(any(CancellationToken.class));
        verify(task2).execute(any(CancellationToken.class));
    }

    @Test(timeout = 5000)
    public void testPoolName() {
        String expectedName = "POOL-NAME" + SingleThreadedExecutor.class.getName();
        SingleThreadedExecutor executor = new SingleThreadedExecutor(expectedName);
        try {
            assertEquals(expectedName, executor.getPoolName());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testGetterValues() {
        SingleThreadedExecutor executor = new SingleThreadedExecutor(
                "TEST-POOL-NAME",
                7,
                2,
                TimeUnit.DAYS);
        executor.dontNeedShutdown();

        assertEquals("TEST-POOL-NAME", executor.getPoolName());
        assertEquals(48L, executor.getIdleTimeout(TimeUnit.HOURS));
        assertEquals(7, executor.getMaxQueueSize());

        executor.setIdleTimeout(3, TimeUnit.DAYS);
        executor.setMaxQueueSize(9);

        assertEquals(72L, executor.getIdleTimeout(TimeUnit.HOURS));
        assertEquals(9, executor.getMaxQueueSize());
    }

    @Test(timeout = 10000)
    public void testMonitoredValues() throws InterruptedException, Exception {
        final SingleThreadedExecutor executor = new SingleThreadedExecutor("");
        try {
            int addToQueue = 2;

            final Collection<Long> numberOfQueuedTasks = new ConcurrentLinkedQueue<>();
            final Collection<Long> numberOfExecutingTasks = new ConcurrentLinkedQueue<>();

            final CountDownLatch startLatch = new CountDownLatch(2);
            final CountDownLatch doneLatch = new CountDownLatch(1);

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                startLatch.countDown();
                startLatch.await();

                numberOfQueuedTasks.add(executor.getNumberOfQueuedTasks());
                numberOfExecutingTasks.add(executor.getNumberOfExecutingTasks());

                doneLatch.countDown();
                doneLatch.await();
            }, null);

            Thread[] queueingThreads = new Thread[addToQueue];
            CancelableTask[] queuedTasks = new CancelableTask[addToQueue];
            CleanupTask[] queuedCleanups = new CleanupTask[addToQueue];

            try {
                final CountDownLatch addLatch = new CountDownLatch(addToQueue);
                for (int i = 0; i < addToQueue; i++) {
                    final CancelableTask queuedTask = mock(CancelableTask.class);
                    final CleanupTask queuedCleanup = mock(CleanupTask.class);
                    queuedTasks[i] = queuedTask;
                    queuedCleanups[i] = queuedCleanup;

                    queueingThreads[i] = new Thread(() -> {
                        addLatch.countDown();
                        executor.execute(Cancellation.UNCANCELABLE_TOKEN, queuedTask, queuedCleanup);
                    });
                    queueingThreads[i].start();
                }

                addLatch.await();
                Thread.sleep(1000);

                // Now the tasks should have all been added to the queue,
                // so test monitored values in the threads.
                startLatch.countDown();

            } finally {
                for (Thread thread: queueingThreads) {
                    thread.interrupt();
                    thread.join();
                }
            }

            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);

            for (int i = 0; i < addToQueue; i++) {
                verify(queuedTasks[i]).execute(any(CancellationToken.class));
                verify(queuedCleanups[i]).cleanup(false, null);
                verifyNoMoreInteractions(queuedTasks[i], queuedCleanups[i]);
            }

            assertEquals(1, numberOfExecutingTasks.size());
            assertEquals(1, numberOfQueuedTasks.size());

            for (Long count: numberOfExecutingTasks) {
                assertEquals((long)1, count.longValue());
            }
            for (Long count: numberOfQueuedTasks) {
                assertEquals((long)addToQueue, count.longValue());
            }

        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 10000)
    public void testClearInterruptForSecondTask() throws Exception {
        final TaskExecutorService executor = new SingleThreadedExecutor(
                "TEST-POOL", Integer.MAX_VALUE, Long.MAX_VALUE, TimeUnit.DAYS);
        try {
            final AtomicBoolean interrupted1 = new AtomicBoolean(true);
            final AtomicBoolean interrupted2 = new AtomicBoolean(true);
            final WaitableSignal doneSignal = new WaitableSignal();

            CancelableTask task1 = (CancellationToken cancelToken) -> {
                Thread.currentThread().interrupt();
            };

            CancelableTask task2 = (CancellationToken cancelToken) -> {
                interrupted2.set(Thread.currentThread().isInterrupted());
                Thread.currentThread().interrupt();
                doneSignal.signal();
            };

            CleanupTask cleanup1 = (boolean canceled, Throwable error) -> {
                interrupted1.set(Thread.currentThread().isInterrupted());
                Thread.currentThread().interrupt();
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2, null);
            };

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1, cleanup1);

            doneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            assertFalse("Interrupting a task must not affect the cleanup task.", interrupted1.get());
            assertFalse("Interrupting a task must not affect other tasks.", interrupted2.get());
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalMaxQueueSize() {
        SingleThreadedExecutor executor = new SingleThreadedExecutor("");
        try {
            executor.setMaxQueueSize(0);
        } finally {
            executor.shutdown();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTimeout1() {
        SingleThreadedExecutor executor = new SingleThreadedExecutor("");
        try {
            executor.setIdleTimeout(-1, TimeUnit.NANOSECONDS);
        } finally {
            executor.shutdown();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalTimeout2() {
        SingleThreadedExecutor executor = new SingleThreadedExecutor("");
        try {
            executor.setIdleTimeout(1, null);
        } finally {
            executor.shutdown();
        }
    }

    private static TestCancellationSource newCancellationSource() {
        return new TestCancellationSource();
    }

    private enum TimeoutChangeType {
        NO_CHANGE,
        INCREASE,
        DECREASE,
        ZERO_TIMEOUT
    }

    private enum Factory implements BackgroundExecutorTests.Factory<SingleThreadedExecutor> {
        INSTANCE;

        @Override
        public SingleThreadedExecutor create(String poolName) {
            return new SingleThreadedExecutor(poolName);
        }

        @Override
        public SingleThreadedExecutor create(String poolName, int maxQueueSize) {
            return new SingleThreadedExecutor(poolName, maxQueueSize);
        }

        @Override
        public SingleThreadedExecutor create(
                String poolName,
                int maxQueueSize,
                long idleTimeout,
                TimeUnit timeUnit) {
            return new SingleThreadedExecutor(poolName, maxQueueSize, idleTimeout, timeUnit);
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
