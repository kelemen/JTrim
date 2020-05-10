package org.jtrim2.executor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.executor.ContextAwareExecutorTests;
import org.jtrim2.testutils.executor.GenericExecutorServiceTests;
import org.jtrim2.testutils.executor.TestExecutorFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class SingleThreadedExecutorTest {
    @Before
    public void setUp() {
        // Clear interrupt because JUnit preserves the interrupted status
        // between tests.
        Thread.interrupted();
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

    private static Collection<TestExecutorFactory<SingleThreadedExecutor>> testFactories() {
        return GenericExecutorServiceTests.executorServices(Arrays.asList(
                SingleThreadedExecutorTest::create1,
                SingleThreadedExecutorTest::create2,
                SingleThreadedExecutorTest::create3
        ));
    }

    private static SingleThreadedExecutor create1() {
        return new SingleThreadedExecutor("ThreadPoolTaskExecutor-Single");
    }

    private static SingleThreadedExecutor create2() {
        return new SingleThreadedExecutor(
                "ThreadPoolTaskExecutor-Multi-No-Timeout",
                Integer.MAX_VALUE,
                0,
                TimeUnit.NANOSECONDS);
    }

    private static SingleThreadedExecutor create3() {
        return new SingleThreadedExecutor(
                "ThreadPoolTaskExecutor-Multi-Short-Buffer",
                1,
                100,
                TimeUnit.MILLISECONDS);
    }

    // Waits until the specified executor terminates and tests
    // if the terminate listener has been called.
    private void waitTerminateAndTest(TaskExecutorService executor) throws InterruptedException {
        BackgroundExecutorTests.waitTerminateAndTest(executor);
    }

    @Test(timeout = 10000)
    public void testAllowedConcurrency() throws Exception {
        ThreadPoolTaskExecutorTest.doTestAllowedConcurrency(1, () -> new SingleThreadedExecutor("TEST-POOL"));
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
        });
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
        });
        executedLatch.await();
    }

    private void doTestGoingIdle(
            ThreadPoolTaskExecutorTest.TimeoutChangeType timeoutChange,
            final boolean doThreadInterrupts) throws InterruptedException {
        ThreadPoolTaskExecutorTest.doTestGoingIdle(
                1,
                timeoutChange,
                doThreadInterrupts,
                SingleThreadedExecutor::setIdleTimeout,
                (threadCount) -> new SingleThreadedExecutor("", Integer.MAX_VALUE, 60, TimeUnit.SECONDS));
    }

    @Test(timeout = 20000)
    public void testGoingIdle() throws InterruptedException {
        for (boolean doThreadInterrupts: Arrays.asList(false, true)) {
            doTestGoingIdle(ThreadPoolTaskExecutorTest.TimeoutChangeType.NO_CHANGE, doThreadInterrupts);
            doTestGoingIdle(ThreadPoolTaskExecutorTest.TimeoutChangeType.INCREASE, doThreadInterrupts);
            doTestGoingIdle(ThreadPoolTaskExecutorTest.TimeoutChangeType.DECREASE, doThreadInterrupts);
            doTestGoingIdle(ThreadPoolTaskExecutorTest.TimeoutChangeType.ZERO_TIMEOUT, doThreadInterrupts);
        }
    }

    @Test(timeout = 10000)
    public void testQueuedTasks() throws Exception {
        ThreadPoolTaskExecutorTest.testQueuedTasks(
                SingleThreadedExecutor::setMaxQueueSize,
                (maxQueueSize) -> new SingleThreadedExecutor("", maxQueueSize));
    }

    @Test(timeout = 5000)
    public void testThreadFactory() throws Exception {
        final Runnable startThreadMock = mock(Runnable.class);
        final WaitableSignal endThreadSignal = new WaitableSignal();

        SingleThreadedExecutor executor = new SingleThreadedExecutor("TEST-POOL");
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
        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        SingleThreadedExecutor executor = new SingleThreadedExecutor("TEST-POOL");
        try {
            executor.setThreadFactory((final Runnable r) -> {
                throw new TestException();
            });
            try {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1);
            } catch (TestException ex) {
                // This is expected because the factory throws this exception.
            }

            executor.setThreadFactory(new ExecutorsEx.NamedThreadFactory(false));
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2);
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
    public void testMonitoredValues() throws Exception {
        ThreadPoolTaskExecutorTest.testMonitoredValues(1, (threadCount) -> new SingleThreadedExecutor(""));
    }

    @Test(timeout = 10000)
    public void testClearInterruptForSecondTask() throws Exception {
        ThreadPoolTaskExecutorTest.testClearInterruptForSecondTask(() -> {
            return new SingleThreadedExecutor("TEST-POOL", Integer.MAX_VALUE, Long.MAX_VALUE, TimeUnit.DAYS);
        });
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

    private enum TimeoutChangeType {
        NO_CHANGE,
        INCREASE,
        DECREASE,
        ZERO_TIMEOUT
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
