package org.jtrim2.executor;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.testutils.executor.ContextAwareExecutorTests;
import org.jtrim2.testutils.executor.GenericExecutorServiceTests;
import org.jtrim2.testutils.executor.MockCleanup;
import org.jtrim2.testutils.executor.TestExecutorFactory;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ThreadPoolTaskExecutorTest extends CommonThreadPoolTest<ThreadPoolTaskExecutor> {
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

    public ThreadPoolTaskExecutorTest() {
        super(properties -> {
            ThreadPoolTaskExecutor result = new ThreadPoolTaskExecutor(
                    properties.getPoolName(),
                    properties.getMaxThreadCount(),
                    properties.getMaxQueueSize()
            );

            result.setThreadFactory(properties.getThreadFactory());
            result.setFullQueueHandler(properties.getFullQueueHandler());
            if (!properties.isNeedShutdown()) {
                result.dontNeedShutdown();
            }
            return result;
        });
    }

    // Waits until the specified executor terminates and tests
    // if the terminate listener has been called.
    private static void waitTerminateAndTest(TaskExecutorService executor) throws InterruptedException {
        BackgroundExecutorTests.waitTerminateAndTest(executor);
    }

    private static int getThreadCount() {
        return BackgroundExecutorTests.getThreadCount();
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

    @Test(timeout = 10000)
    public void testQueuedTasks() throws Exception {
        testQueuedTasks(ThreadPoolTaskExecutor::setMaxQueueSize);
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
