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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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
import org.jtrim2.testutils.executor.GenericExecutorServiceTests;
import org.jtrim2.testutils.executor.MockCleanup;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class CommonThreadPoolTest<E extends MonitorableTaskExecutorService> {
    private final int maxSupportedThreadCount;
    private final CommonThreadPoolFactory<? extends E> threadPoolFactory;

    public CommonThreadPoolTest(CommonThreadPoolFactory<? extends E> threadPoolFactory) {
        this(Integer.MAX_VALUE, threadPoolFactory);
    }

    public CommonThreadPoolTest(int maxSupportedThreadCount, CommonThreadPoolFactory<? extends E> threadPoolFactory) {
        this.maxSupportedThreadCount = ExceptionHelper.checkArgumentInRange(
                maxSupportedThreadCount,
                1,
                Integer.MAX_VALUE,
                "maxSupportedThreadCount"
        );
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

    @Test(timeout = 10000)
    public void testAllowedConcurrency() throws Exception {
        int threadCount = Math.min(maxSupportedThreadCount, 4);
        final int secondPhaseNoCleanupCount = 10;
        final int secondPhaseWithCleanupCount = 10;

        final AtomicInteger executedTasks = new AtomicInteger(0);

        MockCleanup secondPhaseCleanup = mock(MockCleanup.class);
        final TestCancellationSource secondPhaseCancel = newCancellationSource();
        TaskExecutorService executor = threadPoolFactory.create("testAllowedConcurrency-pool", config -> {
            config.setMaxThreadCount(threadCount);
        });
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
    public void testConcurrentTasks() throws Exception {
        doConcurrentTest(1000, Math.min(4, maxSupportedThreadCount));
    }

    protected final void testQueuedTasks(MaxQueueSetter<? super E> maxQueueSetter) throws Exception {
        int maxQueueSize = 2;
        E executor = threadPoolFactory.create("testQueuedTasks-pool", config -> {
            config.setMaxThreadCount(1);
            config.setMaxQueueSize(maxQueueSize);
        });
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

            verifyNoInteractions(canceledTask);

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
        int threadCount = Math.min(maxSupportedThreadCount, 3);
        MonitorableTaskExecutorService executor = threadPoolFactory.create("testMonitoredValues-pool", properties -> {
            properties.setMaxThreadCount(threadCount);
        });
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
                assertEquals(threadCount, count.longValue());
            }
            for (Long count: numberOfQueuedTasks) {
                assertEquals(addToQueue, count.longValue());
            }

        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 20000)
    public void testManyConcurrentSubmitsWithCancellation() throws Exception {
        int threadCount = 2 * Runtime.getRuntime().availableProcessors();

        MonitorableTaskExecutorService executor = threadPoolFactory.create(
                "testManyConcurrentSubmitsWithCancellation-pool",
                Math.min(maxSupportedThreadCount, threadCount),
                1
        );
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

    @Test(timeout = 10000)
    public void testClearInterruptForSecondTask() throws Exception {
        TaskExecutorService executor = threadPoolFactory.create("testClearInterruptForSecondTask-pool", config -> {
            config.setMaxThreadCount(1);
            config.setMaxQueueSize(Integer.MAX_VALUE);
        });
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

    @Test(timeout = 20000)
    public void testFullQueueHandler() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            testFullQueueHandler0();
        }
    }

    private void testFullQueueHandler0() throws InterruptedException {
        RuntimeException fullQueueException = new RuntimeException("fullQueueException");
        AtomicReference<RuntimeException> fullQueueHandlerResultRef = new AtomicReference<>(fullQueueException);

        Runnable failedTask = mock(Runnable.class);
        Runnable blockingTaskAction = mock(Runnable.class);
        Runnable finalTask = mock(Runnable.class);

        WaitableSignal blockingTasksMayExitSignal = new WaitableSignal();

        MonitorableTaskExecutorService executor = threadPoolFactory
                .create("testFailureConfiguredForFullQueue-pool", config -> {
                    config.setMaxThreadCount(1);
                    config.setMaxQueueSize(1);
                    config.setFullQueueHandler(cancelToken -> fullQueueHandlerResultRef.get());
                });

        try {
            WaitableSignal blockingTaskReady = new WaitableSignal();
            executor.execute(() -> {
                blockingTaskReady.signal();
                blockingTasksMayExitSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
                blockingTaskAction.run();
            });
            blockingTaskReady.waitSignal(Cancellation.UNCANCELABLE_TOKEN);

            // This task must be in the queue. Do something so that it will not get optimized away.
            executor.execute(blockingTasksMayExitSignal::signal);

            try {
                executor.execute(failedTask);
                fail("Expected failure due to full queue");
            } catch (RuntimeException ex) {
                assertSame(fullQueueException, ex);
            }

            fullQueueHandlerResultRef.set(null);

            Thread unblockThread = new Thread(() -> {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                    // Just exit, the test is about to fail.
                }
                blockingTasksMayExitSignal.signal();
            });
            unblockThread.start();
            try {
                executor.execute(finalTask);
            } finally {
                unblockThread.interrupt();
                unblockThread.join();
            }
        } finally {
            blockingTasksMayExitSignal.signal();
            GenericExecutorServiceTests.shutdownTestExecutor(executor);
        }

        verifyNoInteractions(failedTask);
        verify(blockingTaskAction).run();
        verify(finalTask).run();
    }

    private void createUnreferenced(
            Runnable onShutdownTask,
            boolean needAutoShutdown,
            Consumer<? super MonitorableTaskExecutorService> shutdownTask) {

        MonitorableTaskExecutorService executor = threadPoolFactory.create("unreferenced-pool", config -> {
            config.setMaxThreadCount(1);
            config.setMaxQueueSize(Integer.MAX_VALUE);
            config.setNeedShutdown(needAutoShutdown);
        });

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

    private void testNoComplaintAfterShutdown(
            Consumer<? super MonitorableTaskExecutorService> shutdownMethod) {

        createUnreferenced(Tasks.noOpTask(), true, shutdownMethod);
        try (LogCollector logs = LogCollector.startCollecting("org.jtrim2")) {
            gc();

            assertEquals("WARNING", 0, logs.getNumberOfLogs(Level.WARNING));
            assertEquals("SEVERE", 0, logs.getNumberOfLogs(Level.SEVERE));
        }
    }

    /**
     * Tests if ThreadPoolTaskExecutor automatically shutdowns itself when
     * no longer referenced. Note that it is still an error to forget to
     * shutdown a ThreadPoolTaskExecutor.
     */
    @Test(timeout = 10000)
    public void testAutoFinalize() {
        WaitableSignal shutdownSignal = new WaitableSignal();
        createUnreferenced(shutdownSignal::signal, true, null);
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
    @Test(timeout = 10000)
    public void testNotAutoFinalize() {
        final WaitableSignal shutdownSignal = new WaitableSignal();

        createUnreferenced(shutdownSignal::signal, false, null);
        try (LogCollector logs = LogCollector.startCollecting("org.jtrim2")) {
            gc();

            assertFalse(shutdownSignal.isSignaled());
            assertEquals("WARNING", 0, logs.getNumberOfLogs(Level.WARNING));
            assertEquals("SEVERE", 0, logs.getNumberOfLogs(Level.SEVERE));
        }
    }

    @Test(timeout = 10000)
    public void testNoComplaintAfterShutdown() {
        testNoComplaintAfterShutdown(TaskExecutorService::shutdown);
        testNoComplaintAfterShutdown(TaskExecutorService::shutdownAndCancel);
    }

    @Test(timeout = 10000)
    public void testFullQueueTaskRemovesFromQueue() throws Exception {
        WaitableSignal blockingSignal = new WaitableSignal();
        CancellationSource blockingCancellation = Cancellation.createCancellationSource();
        E executor = threadPoolFactory.create("testFullQueueTaskRemovesFromQueue-pool", config -> {
            config.setMaxThreadCount(1);
            config.setMaxQueueSize(1);
            config.setFullQueueHandler(cancelToken -> {
                blockingCancellation.getController().cancel();
                return null;
            });
        });

        CancelableTask queuedTask = mock(CancelableTask.class);
        CancelableTask afterBlockTask = mock(CancelableTask.class);

        try {
            WaitableSignal blockingReadySignal = new WaitableSignal();
            executor.execute(() -> {
                blockingReadySignal.signal();
                blockingSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            });
            blockingReadySignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            executor.execute(blockingCancellation.getToken(), queuedTask);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, afterBlockTask);
        } finally {
            blockingSignal.signal();
            GenericExecutorServiceTests.shutdownTestExecutor(executor);
        }

        verifyNoInteractions(queuedTask);
        verify(afterBlockTask).execute(any(CancellationToken.class));
    }

    private static TestCancellationSource newCancellationSource() {
        return new TestCancellationSource();
    }

    public interface CommonThreadPoolFactory<E extends MonitorableTaskExecutorService> {
        public E create(ThreadPoolProperties properties);

        public default E create(
                String poolName,
                Consumer<? super ThreadPoolProperties.Builder> config) {

            ThreadPoolProperties.Builder builder = new ThreadPoolProperties.Builder(poolName);
            config.accept(builder);
            return create(new ThreadPoolProperties(builder));
        }

        public default E create(
                String poolName,
                int maxThreadCount,
                int maxQueueSize) {

            return create(poolName, config -> {
                config.setMaxThreadCount(maxThreadCount);
                config.setMaxQueueSize(maxQueueSize);
            });
        }

        public default E create(
                String poolName,
                int maxThreadCount,
                ThreadFactory threadFactory) {

            return create(poolName, config -> {
                config.setMaxThreadCount(maxThreadCount);
                config.setThreadFactory(threadFactory);
            });
        }

        public default E create(String poolName, int maxThreadCount) {
            return create(poolName, config -> {
                config.setMaxThreadCount(maxThreadCount);
            });
        }
    }

    public static final class ThreadPoolProperties {
        private final String poolName;
        private final int maxThreadCount;
        private final int maxQueueSize;
        private final ThreadFactory threadFactory;
        private final FullQueueHandler fullQueueHandler;
        private final boolean needShutdown;

        private ThreadPoolProperties(Builder builder) {
            this.poolName = builder.poolName;
            this.maxThreadCount = builder.maxThreadCount;
            this.maxQueueSize = builder.maxQueueSize;
            this.threadFactory = builder.threadFactory;
            this.fullQueueHandler = builder.fullQueueHandler;
            this.needShutdown = builder.needShutdown;
        }

        public String getPoolName() {
            return poolName;
        }

        public int getMaxThreadCount() {
            return maxThreadCount;
        }

        public int getMaxQueueSize() {
            return maxQueueSize;
        }

        public ThreadFactory getThreadFactory() {
            return threadFactory;
        }

        public FullQueueHandler getFullQueueHandler() {
            return fullQueueHandler;
        }

        public boolean isNeedShutdown() {
            return needShutdown;
        }

        public static final class Builder {
            private final String poolName;
            private int maxThreadCount;
            private int maxQueueSize;
            private ThreadFactory threadFactory;
            private FullQueueHandler fullQueueHandler;
            private boolean needShutdown;

            public Builder(String poolName) {
                this.poolName = Objects.requireNonNull(poolName, "poolName");
                this.maxThreadCount = 1;
                this.maxQueueSize = Integer.MAX_VALUE;
                this.threadFactory = new ExecutorsEx.NamedThreadFactory(false, poolName);
                this.fullQueueHandler = null;
                this.needShutdown = true;
            }

            public void setMaxThreadCount(int maxThreadCount) {
                this.maxThreadCount = maxThreadCount;
            }

            public void setMaxQueueSize(int maxQueueSize) {
                this.maxQueueSize = maxQueueSize;
            }

            public void setThreadFactory(ThreadFactory threadFactory) {
                this.threadFactory = Objects.requireNonNull(threadFactory, "threadFactory");
            }

            public void setFullQueueHandler(FullQueueHandler fullQueueHandler) {
                this.fullQueueHandler = fullQueueHandler;
            }

            public void setNeedShutdown(boolean needShutdown) {
                this.needShutdown = needShutdown;
            }
        }
    }

    public interface MaxQueueSetter<E> {
        public void setMaxQueueSize(E executor, int newQueueSize);
    }
}
