package org.jtrim2.executor;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.testutils.executor.ContextAwareExecutorTests;
import org.jtrim2.testutils.executor.GenericExecutorServiceTests;
import org.jtrim2.testutils.executor.TestExecutorFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SingleThreadedExecutorTest extends CommonThreadPoolTest<SingleThreadedExecutor> {
    public SingleThreadedExecutorTest() {
        super(1, properties -> {
            // This would be a test configuration error.
            assertEquals("maxThreadCount", 1, properties.getMaxThreadCount());

            SingleThreadedExecutor executor = new SingleThreadedExecutor(
                    properties.getPoolName(),
                    properties.getMaxQueueSize()
            );
            executor.setThreadFactory(properties.getThreadFactory());
            executor.setFullQueueHandler(properties.getFullQueueHandler());
            if (!properties.isNeedShutdown()) {
                executor.dontNeedShutdown();
            }
            return executor;
        });
    }

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
        testQueuedTasks(SingleThreadedExecutor::setMaxQueueSize);
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

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
