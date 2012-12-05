package org.jtrim.concurrent;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.cancel.TestCancellationSource;
import org.junit.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class ThreadPoolTaskExecutorTest {

    public ThreadPoolTaskExecutorTest() {
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

    // Waits until the specified executor terminates and tests
    // if the terminate listener has been called.
    private void waitTerminateAndTest(final TaskExecutorService executor) throws InterruptedException {
        final CountDownLatch listener1Latch = new CountDownLatch(1);
        executor.addTerminateListener(new Runnable() {
            @Override
            public void run() {
                listener1Latch.countDown();
            }
        });
        executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue(executor.isTerminated());
        listener1Latch.await();

        final AtomicReference<Thread> callingThread = new AtomicReference<>(null);
        executor.addTerminateListener(new Runnable() {
            @Override
            public void run() {
                callingThread.set(Thread.currentThread());
            }
        });
        assertSame(Thread.currentThread(), callingThread.get());
    }

    @Test(timeout = 5000)
    public void testSubmitTaskNoCleanup() throws InterruptedException {
        TaskExecutorService executor = new ThreadPoolTaskExecutor("", 1);
        try {
            final Object taskResult = "TASK-RESULT";

            TaskFuture<?> future = executor.submit(
                    Cancellation.UNCANCELABLE_TOKEN,
                    new CancelableFunction<Object>() {
                @Override
                public Object execute(CancellationToken cancelToken) {
                    return taskResult;
                }
            }, null);

            Object result = future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
            assertSame(taskResult, result);
            assertEquals(TaskState.DONE_COMPLETED, future.getTaskState());
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 5000)
    public void testSubmitTaskWithCleanup() throws InterruptedException {
        TaskExecutorService executor = new ThreadPoolTaskExecutor("", 1);
        try {
            final Object taskResult = "TASK-RESULT";
            final CountDownLatch cleanupLatch = new CountDownLatch(1);

            TaskFuture<?> future = executor.submit(
                    Cancellation.UNCANCELABLE_TOKEN,
                    new CancelableFunction<Object>() {
                @Override
                public Object execute(CancellationToken cancelToken) {
                    return taskResult;
                }
            },
                    new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) throws Exception {
                    cleanupLatch.countDown();
                }
            });

            Object result = future.waitAndGet(Cancellation.UNCANCELABLE_TOKEN);
            assertSame(taskResult, result);
            assertEquals(TaskState.DONE_COMPLETED, future.getTaskState());
            cleanupLatch.await();
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    private void doConcurrentTest(int taskCount, int threadCount) throws InterruptedException {
        final AtomicInteger executedTasks = new AtomicInteger(0);
        final CountDownLatch executedCleanups = new CountDownLatch(taskCount);

        TaskExecutorService executor = new ThreadPoolTaskExecutor("TEST-POOL", threadCount);
        try {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(Cancellation.UNCANCELABLE_TOKEN,
                        new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        executedTasks.incrementAndGet();
                    }
                },
                        new CleanupTask() {
                    @Override
                    public void cleanup(boolean canceled, Throwable error) throws Exception {
                        executedCleanups.countDown();
                    }
                });
            }
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }

        assertEquals(taskCount, executedTasks.get());
        executedCleanups.await();
    }

    public void doTestAllowedConcurrency(int threadCount) throws InterruptedException {
        final AtomicInteger executedTasks = new AtomicInteger(0);

        final TestCancellationSource secondPhaseCancel = newCancellationSource();
        TaskExecutorService executor = new ThreadPoolTaskExecutor("TEST-POOL", threadCount);
        try {
            final CountDownLatch phase1Latch = new CountDownLatch(threadCount);
            final CountDownLatch phase2Latch = new CountDownLatch(1);
            for (int i = 0; i < threadCount; i++) {
                executor.submit(Cancellation.UNCANCELABLE_TOKEN,
                        new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        try {
                            phase1Latch.countDown();
                            phase1Latch.await();

                            phase2Latch.await();
                            secondPhaseCancel.getController().cancel();
                            executedTasks.incrementAndGet();
                        } catch (InterruptedException ex) {
                            Thread.interrupted();
                        }
                    }
                }, null);
            }

            for (int i = 0; i < 10; i++) {
                executor.submit(secondPhaseCancel.getToken(),
                        new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        executedTasks.incrementAndGet();
                    }
                }, null);
            }
            phase2Latch.countDown();
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
        assertEquals(threadCount, executedTasks.get());
        secondPhaseCancel.checkNoRegistration();
    }

    @Test(timeout = 10000)
    public void testAllowedConcurrency() throws InterruptedException {
        doTestAllowedConcurrency(4);
    }

    @Test(timeout = 10000)
    public void testConcurrentTasks() throws InterruptedException {
        doConcurrentTest(1000, 4);
    }

    @Test(timeout = 10000)
    public void testShutdownWithCleanups() {
        int threadCount = 1;
        int taskCount = 100;

        TaskExecutorService executor = new ThreadPoolTaskExecutor("TEST-POOL", threadCount);
        try {
            final AtomicInteger execCount = new AtomicInteger(0);
            CleanupTask cleanupTask = new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) {
                    execCount.incrementAndGet();
                }
            };

            for (int i = 0; i < taskCount; i++) {
                executor.execute(
                        Cancellation.UNCANCELABLE_TOKEN,
                        Tasks.noOpCancelableTask(),
                        cleanupTask);
            }
            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
            assertEquals(taskCount, execCount.get());
        } finally {
            executor.shutdown();
        }
    }

    private void doTestCanceledShutdownWithCleanups() {
        int threadCount = 1;
        int taskCount = 100;

        TaskExecutorService executor = new ThreadPoolTaskExecutor("TEST-POOL", threadCount);
        try {
            final AtomicInteger execCount = new AtomicInteger(0);
            CleanupTask cleanupTask = new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) {
                    execCount.incrementAndGet();
                }
            };

            TestCancellationSource cancelSource = newCancellationSource();
            for (int i = 0; i < taskCount; i++) {
                executor.execute(
                        cancelSource.getToken(),
                        Tasks.noOpCancelableTask(),
                        cleanupTask);
            }
            cancelSource.getController().cancel();
            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
            assertEquals(taskCount, execCount.get());
            cancelSource.checkNoRegistration();
        } finally {
            executor.shutdown();
        }
    }

    @Test(timeout = 10000)
    public void testCanceledShutdownWithCleanups() {
        for (int i = 0; i < 100; i++) {
            doTestCanceledShutdownWithCleanups();
        }
    }

    private void doTestCancellationWithCleanups() {
        int threadCount = 1;
        int taskCount = 100;

        TestCancellationSource cancelSource = newCancellationSource();
        TaskExecutorService executor = new ThreadPoolTaskExecutor("TEST-POOL", threadCount);
        try {
            final CountDownLatch latch = new CountDownLatch(taskCount);
            CleanupTask cleanupTask = new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) {
                    latch.countDown();
                }
            };

            for (int i = 0; i < taskCount; i++) {
                executor.execute(
                        cancelSource.getToken(),
                        Tasks.noOpCancelableTask(),
                        cleanupTask);
            }
            cancelSource.getController().cancel();

            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OperationCanceledException(ex);
        } finally {
            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
        cancelSource.checkNoRegistration();
    }

    @Test(timeout = 10000)
    public void testCancellationWithCleanups() {
        for (int i = 0; i < 100; i++) {
            doTestCancellationWithCleanups();
        }
    }

    private void createUnreferenced(Runnable shutdownTask) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(
                "unreferenced-pool",
                1,
                Integer.MAX_VALUE,
                Long.MAX_VALUE,
                TimeUnit.NANOSECONDS);

        executor.addTerminateListener(shutdownTask);

        // To ensure that a thread is started.
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
            }
        }, null);
    }

    /**
     * Tests if ThreadPoolTaskExecutor automatically shutdowns itself when
     * no longer referenced. Note that it is still an error to forget to
     * shutdown a ThreadPoolTaskExecutor.
     */
    @Test(timeout = 10000)
    public void testAutoFinalize() {
        final WaitableSignal shutdownSignal = new WaitableSignal();
        createUnreferenced(new Runnable() {
            @Override
            public void run() {
                shutdownSignal.signal();
            }
        });
        System.gc();
        System.gc();
        Runtime.getRuntime().runFinalization();
        shutdownSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
    }

    @Test(timeout = 10000)
    public void testContextAwarenessInTask() throws InterruptedException {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("", 1);
        assertFalse("ExecutingInThis", executor.isExecutingInThis());

        try {
            final WaitableSignal taskSignal = new WaitableSignal();
            final AtomicBoolean inContext = new AtomicBoolean();

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    inContext.set(executor.isExecutingInThis());
                    taskSignal.signal();
                }
            }, null);

            taskSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            assertTrue("ExecutingInThis", inContext.get());
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 10000)
    public void testContextAwarenessInCleanup() throws InterruptedException {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("", 1);
        try {
            final WaitableSignal taskSignal = new WaitableSignal();
            final AtomicBoolean inContext = new AtomicBoolean();

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) {
                    inContext.set(executor.isExecutingInThis());
                    taskSignal.signal();
                }
            });

            taskSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            assertTrue("ExecutingInThis", inContext.get());
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    @Test(timeout = 10000)
    public void testToString() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("", 1);
        assertNotNull(executor.toString());
    }

    @Test(timeout = 10000)
    public void testShutdownAndCancel() throws Exception {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("", 1);
        try {
            CancelableTask task2 = mock(CancelableTask.class);
            CleanupTask cleanup1 = mock(CleanupTask.class);
            CleanupTask cleanup2 = mock(CleanupTask.class);

            final List<Boolean> cancellation = new LinkedList<>();
            TaskFuture<?> future1 = executor.submit(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) throws Exception {
                    cancellation.add(cancelToken.isCanceled());
                    executor.shutdownAndCancel();
                    cancellation.add(cancelToken.isCanceled());
                }
            }, cleanup1);
            TaskFuture<?> future2 = executor.submit(Cancellation.UNCANCELABLE_TOKEN, task2, cleanup2);

            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);

            verify(cleanup1).cleanup(false, null);
            verify(cleanup2).cleanup(true, null);
            verifyNoMoreInteractions(cleanup1, cleanup2);
            verifyZeroInteractions(task2);

            assertEquals(TaskState.DONE_COMPLETED, future1.getTaskState());
            assertEquals(TaskState.DONE_CANCELED, future2.getTaskState());
            assertEquals(Arrays.asList(false, true), cancellation);
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }

    private static void submitConcurrentTasksAndWait(
            TaskExecutor executor,
            int numberOfTasks,
            final CancelableTask postTask) throws InterruptedException {

        final CountDownLatch allSubmittedLatch = new CountDownLatch(numberOfTasks);
        final CountDownLatch allExecutedLatch = new CountDownLatch(numberOfTasks);

        for (int i = 0; i < numberOfTasks; i++) {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) throws Exception {
                    allSubmittedLatch.countDown();
                    allSubmittedLatch.await();
                    allExecutedLatch.countDown();

                    if (postTask != null) {
                        postTask.execute(cancelToken);
                    }
                }
            }, null);
        }

        allExecutedLatch.await();
    }

    private void doTestGoingIdle(
            int threadCount,
            TimeoutChangeType timeoutChange,
            final boolean doThreadInterrupts) throws InterruptedException {

        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor(
                "", threadCount, Integer.MAX_VALUE, 60, TimeUnit.SECONDS);
        try {
            submitConcurrentTasksAndWait(executor, threadCount, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    if (doThreadInterrupts) {
                        Thread.currentThread().interrupt();
                    }
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

            final CountDownLatch afterIdleExecuted = new CountDownLatch(threadCount);
            AtomicInteger[] executeCounts = new AtomicInteger[threadCount];
            for (int i = 0; i < threadCount; i++) {
                final AtomicInteger counter = new AtomicInteger(0);
                executeCounts[i] = counter;

                executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) throws Exception {
                        counter.incrementAndGet();
                        afterIdleExecuted.countDown();
                    }
                }, null);
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

    @Test(timeout = 10000)
    public void testQueuedTasks() throws Exception {
        int maxQueueSize = 2;
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("", 1, maxQueueSize);
        final WaitableSignal releaseSignal = new WaitableSignal();

        try {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) throws InterruptedException {
                    releaseSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
                }
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
            final Thread cancelThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        // Terminate thread
                    } finally {
                        cancelSource.getController().cancel();
                    }
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

            final Thread unblockThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        // Terminate thread
                    } finally {
                        releaseSignal.signal();
                    }
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
    public void testAwaitTerminationTimeout() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("");
        try {
            assertFalse(executor.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, 100, TimeUnit.NANOSECONDS));
        } finally {
            executor.shutdown();
        }
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
    public void testMonitoredValues() throws InterruptedException, Exception {
        int threadCount = 3;

        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("", threadCount);
        try {
            int addToQueue = 2;

            final Collection<Long> numberOfQueuedTasks = new ConcurrentLinkedQueue<>();
            final Collection<Long> numberOfExecutingTasks = new ConcurrentLinkedQueue<>();

            final CountDownLatch startLatch = new CountDownLatch(threadCount + 1);
            final CountDownLatch doneLatch = new CountDownLatch(threadCount);
            for (int i = 0; i < threadCount; i++) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) throws Exception {
                        startLatch.countDown();
                        startLatch.await();

                        numberOfQueuedTasks.add(executor.getNumberOfQueuedTasks());
                        numberOfExecutingTasks.add(executor.getNumberOfExecutingTasks());

                        doneLatch.countDown();
                        doneLatch.await();
                    }
                }, null);
            }

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

                    queueingThreads[i] = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            addLatch.countDown();
                            executor.execute(Cancellation.UNCANCELABLE_TOKEN, queuedTask, queuedCleanup);
                        }
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

    private enum TimeoutChangeType {
        NO_CHANGE,
        INCREASE,
        DECREASE,
        ZERO_TIMEOUT
    }
}
