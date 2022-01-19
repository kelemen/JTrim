package org.jtrim2.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.cancel.TestCancellationSource;
import org.jtrim2.testutils.executor.MockCleanup;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public abstract class CommonThreadPoolTest {
    private final CommonThreadPoolFactory threadPoolFactory;

    public CommonThreadPoolTest(CommonThreadPoolFactory threadPoolFactory) {
        this.threadPoolFactory = Objects.requireNonNull(threadPoolFactory, "threadPoolFactory");
    }

    // Waits until the specified executor terminates and tests
    // if the terminate listener has been called.
    private static void waitTerminateAndTest(TaskExecutorService executor) throws InterruptedException {
        BackgroundExecutorTests.waitTerminateAndTest(executor);
    }

    private void doConcurrentTest(int taskCount, int threadCount) throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        TaskExecutorService executor = threadPoolFactory.create("TEST-POOL", threadCount);
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

    private void doTestAllowedConcurrency(int threadCount) throws Exception {
        doTestAllowedConcurrency(threadCount, () -> threadPoolFactory.create("TEST-POOL", threadCount));
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

            if (maxQueueSetter != null) {
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
            } else {
                queueTasks = Arrays.copyOfRange(queueTasks, 0, queueTasks.length - 1);
                queueCleanups = Arrays.copyOfRange(queueCleanups, 0, queueCleanups.length - 1);
            }

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
    public void testMonitoredValues() throws Exception {
        testMonitoredValues(3, (threadCount) -> threadPoolFactory.create("", threadCount));
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
                assertEquals((long) threadCount, count.longValue());
            }
            for (Long count: numberOfQueuedTasks) {
                assertEquals((long) addToQueue, count.longValue());
            }

        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 20000)
    public void testManyConcurrentSubmitsWithCancellation() throws Exception {
        int threadCount = 2 * Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("Test-pool", threadCount, 1);
        try {
            for (int i = 0; i < 100; i++) {
                CancellationSource cancellation = Cancellation.createCancellationSource();
                List<Runnable> submitActions = new ArrayList<>();
                List<CancelableTask> normalTasks = new ArrayList<>();
                CountDownLatch finishedLatch = new CountDownLatch(2 * threadCount);
                BiConsumer<Void, Throwable> onComplete = (result, failure) -> {
                    finishedLatch.countDown();
                    cancellation.getController().cancel();
                };
                for (int j = 0; j < threadCount; j++) {
                    CancelableTask task = mock(CancelableTask.class);
                    normalTasks.add(task);

                    submitActions.add(() -> {
                        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                                .whenComplete(onComplete);
                    });
                }

                for (int j = 0; j < threadCount; j++) {
                    submitActions.add(() -> {
                        executor.execute(cancellation.getToken(), mock(CancelableTask.class))
                                .whenComplete(onComplete);
                    });
                }

                Tasks.runConcurrently(submitActions);
                finishedLatch.await();
                for (CancelableTask task : normalTasks) {
                    verify(task).execute(any(CancellationToken.class));
                }
            }
        } finally {
            executor.shutdown();
        }

        executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test(timeout = 5000)
    public void testThreadFactory() throws Exception {
        final Runnable startThreadMock = mock(Runnable.class);
        final WaitableSignal endThreadSignal = new WaitableSignal();

        ThreadFactory threadFactory = (final Runnable r) -> {
            Objects.requireNonNull(r, "r");

            return new Thread(() -> {
                startThreadMock.run();
                r.run();
                endThreadSignal.signal();
            });
        };

        MonitorableTaskExecutorService executor = threadPoolFactory.create("TEST-POOL", 1, threadFactory);
        try {
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
        Runnable exceptionOk = mock(Runnable.class);
        ThreadFactory threadFactory = (final Runnable r) -> {
            try {
                r.run();
                fail("Expected: IllegalStateException");
            } catch (IllegalStateException ex) {
                exceptionOk.run();
            }
            return new Thread(r);
        };

        MonitorableTaskExecutorService executor = threadPoolFactory.create("TEST-POOL", 1, threadFactory);
        try (LogCollector logs = LogTests.startCollecting()) {
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
        WaitableSignal exceptionOk = new WaitableSignal();
        ThreadFactory threadFactory = (final Runnable r) -> new Thread(() -> {
            r.run();
            try {
                r.run();
                fail("Expected: IllegalStateException");
            } catch (IllegalStateException ex) {
                exceptionOk.signal();
            }
        });

        MonitorableTaskExecutorService executor = threadPoolFactory.create("TEST-POOL", 1, threadFactory);
        try (LogCollector logs = LogTests.startCollecting()) {
            try {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask());
            } finally {
                executor.shutdown();
                waitTerminateAndTest(executor);
            }

            exceptionOk.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            assertEquals(1, logs.getNumberOfLogs(Level.SEVERE));
        }
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
            return threadPoolFactory.create("TEST-POOL", 1, Integer.MAX_VALUE);
        });
    }

    private <E extends TaskExecutorService> void createUnreferenced(
            Runnable onShutdownTask,
            boolean needAutoShutdown,
            Consumer<? super E> shutdownTask,
            Consumer<? super E> shutdownDisabler,
            Supplier<? extends E> factory) {

        E executor = factory.get();

        if (!needAutoShutdown) {
            shutdownDisabler.accept(executor);
        }

        executor.addTerminateListener(onShutdownTask);

        // To ensure that a thread is started.
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
        });

        if (shutdownTask != null) {
            shutdownTask.accept(executor);
        }
    }

    private static void gc1() {
        System.gc();
        System.gc();
        Runtime.getRuntime().runFinalization();
    }

    private static void gc() {
        gc1();
        gc1();
    }

    @SuppressWarnings("BusyWait")
    private static void waitForLogs(LogCollector logs) {
        while (logs.getNumberOfLogs() == 0) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Tests if ThreadPoolTaskExecutor automatically shutdowns itself when
     * no longer referenced. Note that it is still an error to forget to
     * shutdown a ThreadPoolTaskExecutor.
     */
    public <E extends TaskExecutorService> void testAutoFinalize(
            Consumer<? super E> shutdownDisabler,
            Supplier<? extends E> factory) {

        WaitableSignal shutdownSignal = new WaitableSignal();
        createUnreferenced(shutdownSignal::signal, true, null, shutdownDisabler, factory);
        try (LogCollector logs = LogCollector.startCollecting("org.jtrim2")) {
            gc();

            shutdownSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            waitForLogs(logs);
            assertEquals("SEVERE", 1, logs.getNumberOfLogs(Level.SEVERE));
        }
    }

    /**
     * Tests that ThreadPoolTaskExecutor does not automatically shutdowns itself
     * when no longer referenced and marked as not required to be shutted down.
     */
    private <E extends TaskExecutorService> void testNotAutoFinalizeIfAsked(
            Consumer<? super E> shutdownDisabler,
            Supplier<? extends E> factory) {

        final WaitableSignal shutdownSignal = new WaitableSignal();

        createUnreferenced(shutdownSignal::signal, false, null, shutdownDisabler, factory);
        try (LogCollector logs = LogCollector.startCollecting("org.jtrim2")) {
            gc();

            assertFalse(shutdownSignal.isSignaled());
            assertEquals("WARNING", 0, logs.getNumberOfLogs(Level.WARNING));
            assertEquals("SEVERE", 0, logs.getNumberOfLogs(Level.SEVERE));
        }
    }

    private <E extends TaskExecutorService> void testNoComplaintAfterShutdown(
            Consumer<? super E> shutdownMethod,
            Consumer<? super E> shutdownDisabler,
            Supplier<? extends E> factory) {

        createUnreferenced(Tasks.noOpTask(), true, shutdownMethod, shutdownDisabler, factory);
        try (LogCollector logs = LogCollector.startCollecting("org.jtrim2")) {
            gc();

            assertEquals("WARNING", 0, logs.getNumberOfLogs(Level.WARNING));
            assertEquals("SEVERE", 0, logs.getNumberOfLogs(Level.SEVERE));
        }
    }

    public <E extends TaskExecutorService> void testNotAutoFinalize(
            Consumer<? super E> shutdownDisabler,
            Supplier<? extends E> factory) {

        testNotAutoFinalizeIfAsked(shutdownDisabler, factory);
    }

    public <E extends TaskExecutorService> void testNoComplaintAfterShutdown(
            Consumer<? super E> shutdownDisabler,
            Supplier<? extends E> factory) {

        testNoComplaintAfterShutdown(TaskExecutorService::shutdown, shutdownDisabler, factory);
        testNoComplaintAfterShutdown(TaskExecutorService::shutdownAndCancel, shutdownDisabler, factory);
    }

    private static TestCancellationSource newCancellationSource() {
        return new TestCancellationSource();
    }

    public interface CommonThreadPoolFactory {
        public MonitorableTaskExecutorService create(
                String poolName,
                int maxThreadCount,
                int maxQueueSize,
                ThreadFactory threadFactory
        );

        public default MonitorableTaskExecutorService create(
                String poolName,
                int maxThreadCount,
                int maxQueueSize) {

            return create(poolName, maxThreadCount, maxQueueSize, Thread::new);
        }

        public default MonitorableTaskExecutorService create(
                String poolName,
                int maxThreadCount,
                ThreadFactory threadFactory) {

            return create(poolName, maxThreadCount, Integer.MAX_VALUE, threadFactory);
        }

        public default MonitorableTaskExecutorService create(String poolName, int maxThreadCount) {
            return create(poolName, maxThreadCount, Integer.MAX_VALUE, Thread::new);
        }
    }

    public interface MaxQueueSetter<E> {
        public void setMaxQueueSize(E executor, int newQueueSize);
    }
}
