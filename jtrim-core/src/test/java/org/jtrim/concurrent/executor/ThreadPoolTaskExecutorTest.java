/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.concurrent.executor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

    private void waitTerminateAndTest(final TaskExecutorService executor) {
        final AtomicBoolean listener1Called = new AtomicBoolean(false);
        executor.addTerminateListener(new Runnable() {
            @Override
            public void run() {
                listener1Called.set(true);
            }
        });
        executor.awaitTermination(CancellationSource.UNCANCELABLE_TOKEN);
        assertTrue(executor.isTerminated());
        assertTrue(listener1Called.get());

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
    public void testSubmitTaskNoCleanup() {
        TaskExecutorService executor = new ThreadPoolTaskExecutor("", 1);
        try {
            final Object taskResult = "TASK-RESULT";

            TaskFuture<?> future = executor.submit(
                    CancellationSource.UNCANCELABLE_TOKEN,
                    new CancelableFunction<Object>() {
                @Override
                public Object execute(CancellationToken cancelToken) {
                    return taskResult;
                }
            }, null);

            Object result = future.waitAndGet(CancellationSource.UNCANCELABLE_TOKEN);
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
                    CancellationSource.UNCANCELABLE_TOKEN,
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

            Object result = future.waitAndGet(CancellationSource.UNCANCELABLE_TOKEN);
            assertSame(taskResult, result);
            assertEquals(TaskState.DONE_COMPLETED, future.getTaskState());
            cleanupLatch.await();
        } finally {
            executor.shutdown();
            waitTerminateAndTest(executor);
        }
    }
}
