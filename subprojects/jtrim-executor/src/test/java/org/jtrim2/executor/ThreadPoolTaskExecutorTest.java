package org.jtrim2.executor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.cancel.TestCancellationSource;
import org.jtrim2.testutils.executor.ContextAwareExecutorTests;
import org.jtrim2.testutils.executor.GenericExecutorServiceTests;
import org.jtrim2.testutils.executor.MockCleanup;
import org.jtrim2.testutils.executor.TestExecutorFactory;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class ThreadPoolTaskExecutorTest {
    // Waits until the specified executor terminates and tests
    // if the terminate listener has been called.
    private static void waitTerminateAndTest(TaskExecutorService executor) throws InterruptedException {
        BackgroundExecutorTests.waitTerminateAndTest(executor);
    }

    private static int getThreadCount() {
        return BackgroundExecutorTests.getThreadCount();
    }

    public static class GenericTest extends BackgroundExecutorTests {
        public GenericTest() {
            super(testFactories());
        }
    }

    public static class ContextAwareTest extends ContextAwareExecutorTests<ContextAwareTaskExecutor> {
        public ContextAwareTest() {
            super(testFactories());
        }
    }

    private static Collection<TestExecutorFactory<ThreadPoolTaskExecutor>> testFactories() {
        return GenericExecutorServiceTests.executorServices(Arrays.asList(
                ThreadPoolTaskExecutorTest::create1,
                ThreadPoolTaskExecutorTest::create2,
                ThreadPoolTaskExecutorTest::create3,
                ThreadPoolTaskExecutorTest::create4
        ));
    }

    private static ThreadPoolTaskExecutor create1() {
        return new ThreadPoolTaskExecutor("ThreadPoolTaskExecutor-Single", 1);
    }

    private static ThreadPoolTaskExecutor create2() {
        return new ThreadPoolTaskExecutor("ThreadPoolTaskExecutor-Multi", getThreadCount());
    }

    private static ThreadPoolTaskExecutor create3() {
        return new ThreadPoolTaskExecutor(
                "ThreadPoolTaskExecutor-Multi-No-Timeout",
                getThreadCount(),
                Integer.MAX_VALUE,
                0,
                TimeUnit.NANOSECONDS);
    }

    private static ThreadPoolTaskExecutor create4() {
        return new ThreadPoolTaskExecutor(
                "ThreadPoolTaskExecutor-Multi-Short-Buffer",
                getThreadCount(),
                1,
                100,
                TimeUnit.MILLISECONDS);
    }

    private void doConcurrentTest(int taskCount, int threadCount) throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        TaskExecutorService executor = new ThreadPoolTaskExecutor("TEST-POOL", threadCount);
        try {
            for (int i = 0; i < taskCount; i++) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                        .whenComplete(MockCleanup.toCleanupTask(cleanup));
            }
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }

        verify(task, times(taskCount)).execute(any(CancellationToken.class));
        verify(cleanup, times(taskCount)).cleanup(null, null);
    }

    private static void doTestAllowedConcurrency(int threadCount) throws Exception {
        doTestAllowedConcurrency(threadCount, () -> new ThreadPoolTaskExecutor("TEST-POOL", threadCount));
    }

    public static void doTestAllowedConcurrency(
            int threadCount,
            Supplier<TaskExecutorService> factory) throws Exception {

        final int secondPhaseNoCleanupCount = 10;
        final int secondPhaseWithCleanupCount = 10;

        final AtomicInteger executedTasks = new AtomicInteger(0);

        MockCleanup secondPhaseCleanup = mock(MockCleanup.class);
        final TestCancellationSource secondPhaseCancel = newCancellationSource();
        TaskExecutorService executor = factory.get();
        try {
            final CountDownLatch phase1Latch = new CountDownLatch(threadCount);
            final CountDownLatch phase2Latch = new CountDownLatch(1);
            for (int i = 0; i < threadCount; i++) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                    try {
                        phase1Latch.countDown();
                        phase1Latch.await();

                        phase2Latch.await();
                        secondPhaseCancel.getController().cancel();
                        executedTasks.incrementAndGet();
                    } catch (InterruptedException ex) {
                        Thread.interrupted();
                    }
                });
            }

            for (int i = 0; i < secondPhaseNoCleanupCount; i++) {
                executor.execute(secondPhaseCancel.getToken(), (CancellationToken cancelToken) -> {
                    executedTasks.incrementAndGet();
                });
            }
            for (int i = 0; i < secondPhaseWithCleanupCount; i++) {
                executor.execute(secondPhaseCancel.getToken(), CancelableTasks.noOpCancelableTask())
                        .whenComplete(MockCleanup.toCleanupTask(secondPhaseCleanup));
            }
            phase2Latch.countDown();
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
        assertEquals(threadCount, executedTasks.get());
        verify(secondPhaseCleanup, times(secondPhaseWithCleanupCount))
                .cleanup(isNull(), isA(OperationCanceledException.class));
        secondPhaseCancel.checkNoRegistration();
    }

    @Test(timeout = 10000)
    public void testAllowedConcurrency() throws Exception {
        doTestAllowedConcurrency(4);
    }

    @Test(timeout = 10000)
    public void testConcurrentTasks() throws Exception {
        doConcurrentTest(1000, 4);
    }

    private void createUnreferenced(Runnable shutdownTask, boolean needShutdown) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(
                "unreferenced-pool",
                1,
                Integer.MAX_VALUE,
                Long.MAX_VALUE,
                TimeUnit.NANOSECONDS);
        if (!needShutdown) {
            executor.dontNeedShutdown();
        }

        executor.addTerminateListener(shutdownTask);

        // To ensure that a thread is started.
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
        });
    }

    /**
     * Tests if ThreadPoolTaskExecutor automatically shutdowns itself when
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
     * Tests that ThreadPoolTaskExecutor does not automatically shutdowns itself
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

    private static void submitConcurrentTasksAndWait(
            TaskExecutor executor,
            int numberOfTasks,
            final CancelableTask postTask) throws InterruptedException {

        final CountDownLatch allSubmittedLatch = new CountDownLatch(numberOfTasks);
        final CountDownLatch allExecutedLatch = new CountDownLatch(numberOfTasks);

        for (int i = 0; i < numberOfTasks; i++) {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                allSubmittedLatch.countDown();
                allSubmittedLatch.await();
                allExecutedLatch.countDown();

                if (postTask != null) {
                    postTask.execute(cancelToken);
                }
            });
        }

        allExecutedLatch.await();
    }

    private static ThreadPoolTaskExecutor createWithLongTimeout(int maxThreadCount) {
        return new ThreadPoolTaskExecutor("", maxThreadCount, Integer.MAX_VALUE, 60, TimeUnit.SECONDS);
    }

    private void doTestGoingIdle(
            int threadCount,
            TimeoutChangeType timeoutChange,
             boolean doThreadInterrupts) throws InterruptedException {
        doTestGoingIdle(
                threadCount,
                timeoutChange,
                doThreadInterrupts,
                ThreadPoolTaskExecutor::setIdleTimeout,
                ThreadPoolTaskExecutorTest::createWithLongTimeout);
    }

    public static <E extends TaskExecutorService> void doTestGoingIdle(
            int threadCount,
            TimeoutChangeType timeoutChange,
            boolean doThreadInterrupts,
            IdleTimeoutSetter<E> timeoutSetter,
            IntFunction<E> factory) throws InterruptedException {

        final E executor = factory.apply(threadCount);
        try {
            submitConcurrentTasksAndWait(executor, threadCount, (CancellationToken cancelToken) -> {
                if (doThreadInterrupts) {
                    Thread.currentThread().interrupt();
                }
            });

            Thread.sleep(10);

            // Now all the tasks have been executed and it is very likely
            // that the started threads are in idle wait.

            switch (timeoutChange) {
                case NO_CHANGE:
                    // Do nothing
                    break;
                case INCREASE:
                    timeoutSetter.setIdleTimeout(executor, Long.MAX_VALUE, TimeUnit.DAYS);
                    break;
                case DECREASE:
                    timeoutSetter.setIdleTimeout(executor, 30, TimeUnit.SECONDS);
                    break;
                case ZERO_TIMEOUT:
                    timeoutSetter.setIdleTimeout(executor, 0, TimeUnit.NANOSECONDS);
                    break;
                default:
                    throw new AssertionError(timeoutChange.name());
            }

            final CountDownLatch afterIdleExecuted = new CountDownLatch(threadCount);
            AtomicInteger[] executeCounts = new AtomicInteger[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final AtomicInteger counter = new AtomicInteger(0);
                executeCounts[i] = counter;

                executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                    counter.incrementAndGet();
                    afterIdleExecuted.countDown();
                });
            }
            afterIdleExecuted.await();

            for (int i = 0; i < threadCount; i++) {
                assertEquals(1, executeCounts[i].get());
            }
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 20000)
    public void testGoingIdle() throws InterruptedException {
        for (int i = 0; i < 25; i++) {
            int threadCount = i / 5 + 1;
            boolean doThreadInterrupts = i % 2 == 0;

            doTestGoingIdle(threadCount, TimeoutChangeType.NO_CHANGE, doThreadInterrupts);
            doTestGoingIdle(threadCount, TimeoutChangeType.INCREASE, doThreadInterrupts);
            doTestGoingIdle(threadCount, TimeoutChangeType.DECREASE, doThreadInterrupts);
            doTestGoingIdle(threadCount, TimeoutChangeType.ZERO_TIMEOUT, doThreadInterrupts);
        }
    }

    public static <E extends TaskExecutorService> void testQueuedTasks(
            MaxQueueSetter<E> maxQueueSetter,
            IntFunction<E> factory) throws Exception {
        int maxQueueSize = 2;
        E executor = factory.apply(maxQueueSize);
        final WaitableSignal releaseSignal = new WaitableSignal();

        try {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                releaseSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            });

            // Fill the queue
            CancelableTask[] queueTasks = new CancelableTask[maxQueueSize + 1];
            MockCleanup[] queueCleanups = new MockCleanup[maxQueueSize + 1];

            for (int i = 0; i < maxQueueSize; i++) {
                queueTasks[i] = mock(CancelableTask.class);
                queueCleanups[i] = mock(MockCleanup.class);

                executor.execute(Cancellation.UNCANCELABLE_TOKEN, queueTasks[i])
                        .whenComplete(MockCleanup.toCleanupTask(queueCleanups[i]));
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
                executor.execute(cancelSource.getToken(), canceledTask);
            } finally {
                cancelThread.interrupt();
                cancelThread.join();
            }

            verifyZeroInteractions(canceledTask);

            // Now increase the maximum queue size and verify that it can be
            // submitted without blocking
            maxQueueSetter.setMaxQueueSize(executor, maxQueueSize + 1);

            queueTasks[maxQueueSize] = mock(CancelableTask.class);
            queueCleanups[maxQueueSize] = mock(MockCleanup.class);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, queueTasks[maxQueueSize])
                    .whenComplete(MockCleanup.toCleanupTask(queueCleanups[maxQueueSize]));

            maxQueueSetter.setMaxQueueSize(executor, maxQueueSize);
            // decrease the maximum queue size to see that the executor still
            // remains functional.

            // Now submit another one but do not cancel this one
            CancelableTask blockedTask = mock(CancelableTask.class);
            MockCleanup blockedCleanup = mock(MockCleanup.class);

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
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, blockedTask)
                        .whenComplete(MockCleanup.toCleanupTask(blockedCleanup));
            } finally {
                unblockThread.interrupt();
                unblockThread.join();
            }

            // Now wait for all the tasks to complete and verify that they were
            // executed.
            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);

            verify(blockedTask).execute(any(CancellationToken.class));
            verify(blockedCleanup).cleanup(null, null);
            verifyNoMoreInteractions(blockedTask, blockedCleanup);

            for (int i = 0; i < queueTasks.length; i++) {
                CancelableTask task = queueTasks[i];
                MockCleanup cleanup = queueCleanups[i];

                verify(task).execute(any(CancellationToken.class));
                verify(cleanup).cleanup(null, null);
                verifyNoMoreInteractions(task, cleanup);
            }

        } finally {
            releaseSignal.signal();
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 10000)
    public void testQueuedTasks() throws Exception {
        testQueuedTasks(
                ThreadPoolTaskExecutor::setMaxQueueSize,
                (maxQueueSize) -> new ThreadPoolTaskExecutor("", 1, maxQueueSize));
    }

    @Test
    public void testGetterValues() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(
                "TEST-POOL-NAME",
                5,
                7,
                2,
                TimeUnit.DAYS);
        executor.dontNeedShutdown();

        assertEquals("TEST-POOL-NAME", executor.getPoolName());
        assertEquals(48L, executor.getIdleTimeout(TimeUnit.HOURS));
        assertEquals(7, executor.getMaxQueueSize());
        assertEquals(5, executor.getMaxThreadCount());

        executor.setIdleTimeout(3, TimeUnit.DAYS);
        executor.setMaxQueueSize(9);
        executor.setMaxThreadCount(11);

        assertEquals(72L, executor.getIdleTimeout(TimeUnit.HOURS));
        assertEquals(9, executor.getMaxQueueSize());
        assertEquals(11, executor.getMaxThreadCount());
    }

    @Test(timeout = 5000)
    public void testPoolName() {
        String expectedName = "POOL-NAME" + ThreadPoolTaskExecutorTest.class.getName();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(expectedName);
        try {
            assertEquals(expectedName, executor.getPoolName());
        } finally {
            executor.shutdown();
        }
    }

    @Test(timeout = 10000)
    public void testAdjustThreadCount() throws InterruptedException {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("", 1);
        try {
            submitConcurrentTasksAndWait(executor, 1, null);
            executor.setMaxThreadCount(3);
            submitConcurrentTasksAndWait(executor, 3, null);
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 10000)
    public void testMonitoredValues() throws Exception {
        testMonitoredValues(3, (threadCount) -> new ThreadPoolTaskExecutor("", threadCount));
    }

    public static void testMonitoredValues(
            int threadCount,
            IntFunction<? extends MonitorableTaskExecutorService> factory) throws Exception {
        MonitorableTaskExecutorService executor = factory.apply(threadCount);
        try {
            int addToQueue = 2;

            final Collection<Long> numberOfQueuedTasks = new ConcurrentLinkedQueue<>();
            final Collection<Long> numberOfExecutingTasks = new ConcurrentLinkedQueue<>();

            final CountDownLatch startLatch = new CountDownLatch(threadCount + 1);
            final CountDownLatch doneLatch = new CountDownLatch(threadCount);
            for (int i = 0; i < threadCount; i++) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                    startLatch.countDown();
                    startLatch.await();

                    numberOfQueuedTasks.add(executor.getNumberOfQueuedTasks());
                    numberOfExecutingTasks.add(executor.getNumberOfExecutingTasks());

                    doneLatch.countDown();
                    doneLatch.await();
                });
            }

            Thread[] queueingThreads = new Thread[addToQueue];
            CancelableTask[] queuedTasks = new CancelableTask[addToQueue];
            MockCleanup[] queuedCleanups = new MockCleanup[addToQueue];

            try {
                final CountDownLatch addLatch = new CountDownLatch(addToQueue);
                for (int i = 0; i < addToQueue; i++) {
                    final CancelableTask queuedTask = mock(CancelableTask.class);
                    final MockCleanup queuedCleanup = mock(MockCleanup.class);
                    queuedTasks[i] = queuedTask;
                    queuedCleanups[i] = queuedCleanup;

                    queueingThreads[i] = new Thread(() -> {
                        addLatch.countDown();
                        executor.execute(Cancellation.UNCANCELABLE_TOKEN, queuedTask)
                                .whenComplete(MockCleanup.toCleanupTask(queuedCleanup));
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
                verify(queuedCleanups[i]).cleanup(null, null);
                verifyNoMoreInteractions(queuedTasks[i], queuedCleanups[i]);
            }

            assertEquals(threadCount, numberOfExecutingTasks.size());
            assertEquals(threadCount, numberOfQueuedTasks.size());

            for (Long count: numberOfExecutingTasks) {
                assertEquals((long)threadCount, count.longValue());
            }
            for (Long count: numberOfQueuedTasks) {
                assertEquals((long)addToQueue, count.longValue());
            }

        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 5000)
    public void testThreadFactory() throws Exception {
        final Runnable startThreadMock = mock(Runnable.class);
        final WaitableSignal endThreadSignal = new WaitableSignal();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("TEST-POOL", 1);
        try {
            ThreadFactory threadFactory = (final Runnable r) -> {
                Objects.requireNonNull(r, "r");

                return new Thread(() -> {
                    startThreadMock.run();
                    r.run();
                    endThreadSignal.signal();
                });
            };
            executor.setThreadFactory(threadFactory);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask());
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }

        verify(startThreadMock).run();
        endThreadSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test(timeout = 5000)
    public void testThreadFactoryCallsWorker() throws Exception {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("TEST-POOL", 1);
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
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask());

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

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("TEST-POOL", 1);
        try (LogCollector logs = LogTests.startCollecting()) {
            try {
                ThreadFactory threadFactory = (final Runnable r) -> new Thread(() -> {
                    r.run();
                    try {
                        r.run();
                        fail("Expected: IllegalStateException");
                    } catch (IllegalStateException ex) {
                        exceptionOk.signal();
                    }
                });
                executor.setThreadFactory(threadFactory);
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask());
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
        CancelableTask task2 = mock(CancelableTask.class);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("TEST-POOL", 1);
        try {
            executor.setThreadFactory((final Runnable r) -> {
                throw new TestException();
            });
            try {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask());
            } catch (TestException ex) {
                // This is expected because the factory throws this exception.
            }

            executor.setThreadFactory(new ExecutorsEx.NamedThreadFactory(false));
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2);
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }

        verify(task2).execute(any(CancellationToken.class));
    }

    public static void testClearInterruptForSecondTask(Supplier<TaskExecutorService> factory) throws Exception {
        TaskExecutorService executor = factory.get();
        try {
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

            MockCleanup cleanup1 = (Object result, Throwable error) -> {
                Thread.currentThread().interrupt();
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2);
            };

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup1));

            doneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            assertFalse("Interrupting a task must not affect other tasks.", interrupted2.get());
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 10000)
    public void testClearInterruptForSecondTask() throws Exception {
        testClearInterruptForSecondTask(() -> {
            return new ThreadPoolTaskExecutor("TEST-POOL", 1, Integer.MAX_VALUE, Long.MAX_VALUE, TimeUnit.DAYS);
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalMaxQueueSize() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("");
        try {
            executor.setMaxQueueSize(0);
        } finally {
            executor.shutdown();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalMaxThreadCount() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("");
        try {
            executor.setMaxThreadCount(0);
        } finally {
            executor.shutdown();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTimeout1() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("");
        try {
            executor.setIdleTimeout(-1, TimeUnit.NANOSECONDS);
        } finally {
            executor.shutdown();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalTimeout2() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("");
        try {
            executor.setIdleTimeout(1, null);
        } finally {
            executor.shutdown();
        }
    }

    private static TestCancellationSource newCancellationSource() {
        return new TestCancellationSource();
    }

    public enum TimeoutChangeType {
        NO_CHANGE,
        INCREASE,
        DECREASE,
        ZERO_TIMEOUT
    }

    public interface MaxQueueSetter<E> {
        public void setMaxQueueSize(E executor, int newQueueSize);
    }

    public interface IdleTimeoutSetter<E> {
        public void setIdleTimeout(E executor, long timeout, TimeUnit unit);
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
