package org.jtrim.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.junit.*;

import static org.junit.Assert.*;

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
        TaskExecutorService executor = new ThreadPoolTaskExecutor("TEST-POOL", threadCount);
        try {
            final CancellationSource secondPhaseCancel = Cancellation.createCancellationSource();

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
    }

    @Test(timeout = 10000)
    public void testAllowedConcurrency() throws InterruptedException {
        doTestAllowedConcurrency(4);
    }

    @Test(timeout = 10000)
    public void testConcurrentTasks() throws InterruptedException {
        doConcurrentTest(1000, 4);
    }
}
