package org.jtrim.concurrent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.cancel.TestCancellationSource;
import org.jtrim.utils.LogCollector;
import org.jtrim.utils.LogCollectorTest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public final class BackgroundExecutorTests {
    // Waits until the specified executor terminates and tests
    // if the terminate listener has been called.
    private static void waitTerminateAndTest(final TaskExecutorService executor) throws InterruptedException {
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

    @GenericTest
    public static void testSubmitTaskNoCleanup(Factory<?> factory) throws InterruptedException {
        TaskExecutorService executor = factory.create("");
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

    @GenericTest
    public static void testSubmitTaskWithCleanup(Factory<?> factory) throws InterruptedException {
        TaskExecutorService executor = factory.create("");
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

    @GenericTest
    public static void testShutdownWithCleanups(Factory<?> factory) {
        int taskCount = 100;

        TaskExecutorService executor = factory.create("TEST-POOL");
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

    private static void doTestCanceledShutdownWithCleanups(Factory<?> factory) {
        int taskCount = 100;

        TaskExecutorService executor = factory.create("TEST-POOL");
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
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    @GenericTest
    public static void testCanceledShutdownWithCleanups(Factory<?> factory) {
        for (int i = 0; i < 100; i++) {
            doTestCanceledShutdownWithCleanups(factory);
        }
    }

    private static void doTestCancellationWithCleanups(Factory<?> factory) {
        int taskCount = 100;

        TestCancellationSource cancelSource = newCancellationSource();
        TaskExecutorService executor = factory.create("TEST-POOL");
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

    @GenericTest
    public static void testCancellationWithCleanups(Factory<?> factory) {
        for (int i = 0; i < 100; i++) {
            doTestCancellationWithCleanups(factory);
        }
    }

    @GenericTest
    public static void testContextAwarenessInTask(Factory<?> factory) throws InterruptedException {
        final TaskExecutorService executor = factory.create("", 1);
        assertFalse("ExecutingInThis", ((MonitorableTaskExecutor)executor).isExecutingInThis());

        try {
            final WaitableSignal taskSignal = new WaitableSignal();
            final AtomicBoolean inContext = new AtomicBoolean();

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    inContext.set(((MonitorableTaskExecutor)executor).isExecutingInThis());
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

    @GenericTest
    public static void testContextAwarenessInCleanup(Factory<?> factory) throws InterruptedException {
        final TaskExecutorService executor = factory.create("");
        try {
            final WaitableSignal taskSignal = new WaitableSignal();
            final AtomicBoolean inContext = new AtomicBoolean();

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), new CleanupTask() {
                @Override
                public void cleanup(boolean canceled, Throwable error) {
                    inContext.set(((MonitorableTaskExecutor)executor).isExecutingInThis());
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

    @GenericTest
    public static void testToString(Factory<?> factory) {
        TaskExecutorService executor = factory.create("");
        assertNotNull(executor.toString());
    }

    @GenericTest
    public static void testShutdownAndCancel(Factory<?> factory) throws Exception {
        final TaskExecutorService executor = factory.create("");
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

    @GenericTest
    public static void testAwaitTerminationTimeout(Factory<?> factory) {
        TaskExecutorService executor = factory.create("");
        try {
            assertFalse(executor.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, 100, TimeUnit.NANOSECONDS));
        } finally {
            executor.shutdown();
        }
    }

    @GenericTest
    public static void testPlainTaskWithError(Factory<?> factory) throws Exception {
        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        doThrow(new TestException())
                .when(task1)
                .execute(any(CancellationToken.class));

        TaskExecutorService executor = factory.create("");
        try (LogCollector logs = LogCollectorTest.startCollecting()) {
            try {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1, null);
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2, null);
            } finally {
                executor.shutdown();
                waitTerminateAndTest(executor);
                LogCollectorTest.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
            }
        }

        verify(task1).execute(any(CancellationToken.class));
        verify(task2).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(task1, task2);
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

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public interface Factory<T extends TaskExecutorService & MonitorableTaskExecutor> {
        public T create(String poolName);
        public T create(String poolName, int maxQueueSize);
        public T create(
                String poolName,
                int maxQueueSize,
                long idleTimeout,
                TimeUnit timeUnit);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    private @interface GenericTest {
    }

    private BackgroundExecutorTests() {
        throw new AssertionError();
    }
}
