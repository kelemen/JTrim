package org.jtrim2.executor;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.testutils.TestParameterRule;
import org.jtrim2.testutils.executor.GenericExecutorServiceTests;
import org.jtrim2.utils.TimeDuration;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ThreadPoolBuilderTest {
    @Rule
    public final TestParameterRule managedExecutorRule = new TestParameterRule(
            Arrays.asList(factoryMethodTestFactory(), builderTestFactory()),
            this::setFactory
    );

    private TestThreadPoolFactory factory;

    public ThreadPoolBuilderTest() {
        this.factory = factoryMethodTestFactory();
    }

    private static TestThreadPoolFactory factoryMethodTestFactory() {
        return ThreadPoolBuilder::create;
    }

    private static TestThreadPoolFactory builderTestFactory() {
        return ThreadPoolBuilder::create;
    }

    private void setFactory(TestThreadPoolFactory factory) {
        this.factory = factory;
    }

    private static void shutdownTestExecutor(TaskExecutorService executor) {
        try {
            GenericExecutorServiceTests.shutdownTestExecutor(executor);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    private <E extends MonitorableTaskExecutorService> void test(
            Class<? extends E> type,
            String poolName,
            Consumer<? super ThreadPoolBuilder> config,
            Consumer<? super E> testAction) {

        E executor = factory.create(type, poolName, config);
        try {
            testAction.accept(executor);
        } finally {
            shutdownTestExecutor(executor);
        }
    }

    private static TaskExecutorService unwrap(TaskExecutorService executor) {
        if (executor instanceof DelegatedTaskExecutorService) {
            return ((DelegatedTaskExecutorService) executor).wrappedExecutor;
        } else {
            return executor;
        }
    }

    private <E extends MonitorableTaskExecutorService> void testUnwrapped(
            Class<? extends E> type,
            String poolName,
            Consumer<? super ThreadPoolBuilder> config,
            BiConsumer<? super E, ? super MonitorableTaskExecutor> testAction) {

        MonitorableTaskExecutorService wrapper = factory.create(poolName, config);
        try {
            E executor = verifyType(type, unwrap(wrapper));
            testAction.accept(executor, wrapper);
        } finally {
            shutdownTestExecutor(wrapper);
        }
    }

    private void verifyDefaultThreadFactory(
            ThreadFactory factory,
            boolean expectedDaemon,
            String expectedPoolName) {

        ExecutorsEx.NamedThreadFactory namedFactory = verifyType(ExecutorsEx.NamedThreadFactory.class, factory);
        assertEquals("daemon", expectedDaemon, namedFactory.isDaemon());
        if (!namedFactory.getNamePrefix().startsWith(expectedPoolName)) {
            throw new AssertionError("Expected thread factory for pool: \""
                    + expectedPoolName + "\", but received \"" + namedFactory.getNamePrefix() + "\"");
        }
    }

    @Test
    public void testDefaultValues() {
        test(SingleThreadedExecutor.class, "MY-DEFAULT-TEST-POOL", Tasks.noOpConsumer(), executor -> {
            assertFalse("finalized", executor.isFinalized());
            assertEquals("MY-DEFAULT-TEST-POOL", executor.getPoolName());
            assertEquals("maxQueueSize", Integer.MAX_VALUE, executor.getMaxQueueSize());
            assertEquals("idleTimeout", 5L, executor.getIdleTimeout(TimeUnit.SECONDS));
            verifyDefaultThreadFactory(executor.getThreadFactory(), false, "MY-DEFAULT-TEST-POOL");
            assertNull("fullQueueHandler", executor.getFullQueueHandler());
        });
    }

    private void testSetBuiltThreadFactory(boolean daemon) {
        test(SingleThreadedExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setThreadFactoryWithConfig(threadFactory -> {
                        threadFactory.setDaemon(daemon);
                    });
                },
                executor -> {
                    assertFalse("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxQueueSize", Integer.MAX_VALUE, executor.getMaxQueueSize());
                    assertEquals("idleTimeout", 5L, executor.getIdleTimeout(TimeUnit.SECONDS));
                    verifyDefaultThreadFactory(executor.getThreadFactory(), daemon, "MY-TEST-POOL");
                    assertNull("fullQueueHandler", executor.getFullQueueHandler());
                }
        );
    }

    @Test
    public void testSetBuiltThreadFactory() {
        testSetBuiltThreadFactory(false);
        testSetBuiltThreadFactory(true);
    }

    @Test
    public void testSetValuesForSingleThreaded() {
        TestThreadFactory threadFactory = new TestThreadFactory();
        TestFullQueueHandler fullQueueHandler = new TestFullQueueHandler();
        test(SingleThreadedExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setMaxQueueSize(10);
                    builder.setIdleTimeout(TimeDuration.nanos(239));
                    builder.setThreadFactory(threadFactory);
                    builder.setMaxThreadCount(1);
                    builder.setManualShutdownRequired(false);
                    builder.setFullQueueHandler(fullQueueHandler);
                },
                executor -> {
                    assertTrue("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxQueueSize", 10, executor.getMaxQueueSize());
                    assertEquals("idleTimeout", 239L, executor.getIdleTimeout(TimeUnit.NANOSECONDS));
                    assertSame(threadFactory, executor.getThreadFactory());
                    assertSame(fullQueueHandler, executor.getFullQueueHandler());
                }
        );
    }

    private static ContextAwareTaskExecutor newTestFallbackExecutor() {
        return TaskExecutors.contextAware(SyncTaskExecutor.getSimpleExecutor());
    }

    private static void verifyFallback(
            int blockerTaskCount,
            MonitorableTaskExecutor executor,
            ContextAwareTaskExecutor fallback) {

        // This test just verifies proper configuration.
        // The main test for the fallback mechanism is in FallbackExecutorTest

        WaitableSignal unblockSignal = new WaitableSignal();
        CountDownLatch allBlockerReady = new CountDownLatch(blockerTaskCount);
        Runnable blockerTask = () -> {
            allBlockerReady.countDown();
            unblockSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        };
        Runnable queueHogTask = mock(Runnable.class);

        AtomicBoolean inContextWrong = new AtomicBoolean(true);
        AtomicBoolean inContext = new AtomicBoolean(false);
        Runnable fallbackTestAction = mock(Runnable.class);
        Runnable fallbackTest = () -> {
            inContextWrong.set(executor.isExecutingInThis());
            inContext.set(fallback.isExecutingInThis());
            fallbackTestAction.run();
        };

        try {
            for (int i = 0; i < blockerTaskCount; i++) {
                executor.execute(blockerTask);
            }
            allBlockerReady.await();
            executor.execute(queueHogTask);
            executor.execute(fallbackTest);

            verify(fallbackTestAction).run();
            assertTrue("inContext", inContext.get());
            assertFalse("inContextWrong", inContextWrong.get());

            verifyNoInteractions(queueHogTask);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        } finally {
            unblockSignal.signal();
        }
    }

    @Test(timeout = 10000)
    public void testFallbackSingleThreadedExecutor() {
        TestThreadFactory threadFactory = new TestThreadFactory();
        ContextAwareTaskExecutor fallback = newTestFallbackExecutor();
        testUnwrapped(SingleThreadedExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setMaxQueueSize(1);
                    builder.setIdleTimeout(TimeDuration.nanos(239));
                    builder.setThreadFactory(threadFactory);
                    builder.setMaxThreadCount(1);
                    builder.setManualShutdownRequired(false);
                    builder.setFullQueueHandlerToFallback(fallback);
                },
                (executor, wrapper) -> {
                    assertTrue("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxQueueSize", 1, executor.getMaxQueueSize());
                    assertEquals("idleTimeout", 239L, executor.getIdleTimeout(TimeUnit.NANOSECONDS));
                    assertSame(threadFactory, executor.getThreadFactory());
                    verifyFallback(1, wrapper, fallback);
                }
        );
    }

    @Test
    public void testSetMaxThreadCount() {
        test(ThreadPoolTaskExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setMaxThreadCount(2);
                },
                executor -> {
                    assertFalse("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxThreadCount", 2, executor.getMaxThreadCount());
                    assertEquals("maxQueueSize", Integer.MAX_VALUE, executor.getMaxQueueSize());
                    assertEquals("idleTimeout", 5L, executor.getIdleTimeout(TimeUnit.SECONDS));
                    verifyDefaultThreadFactory(executor.getThreadFactory(), false, "MY-TEST-POOL");
                    assertNull("fullQueueHandler", executor.getFullQueueHandler());
                }
        );
    }

    @Test
    public void testSetValuesForGeneric() {
        TestThreadFactory threadFactory = new TestThreadFactory();
        TestFullQueueHandler fullQueueHandler = new TestFullQueueHandler();
        test(ThreadPoolTaskExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setMaxThreadCount(3);
                    builder.setIdleTimeout(TimeDuration.nanos(534));
                    builder.setMaxQueueSize(12);
                    builder.setThreadFactory(threadFactory);
                    builder.setManualShutdownRequired(false);
                    builder.setFullQueueHandler(fullQueueHandler);
                },
                executor -> {
                    assertTrue("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxThreadCount", 3, executor.getMaxThreadCount());
                    assertEquals("maxQueueSize", 12, executor.getMaxQueueSize());
                    assertEquals("idleTimeout", 534L, executor.getIdleTimeout(TimeUnit.NANOSECONDS));
                    assertSame(threadFactory, executor.getThreadFactory());
                    assertSame(fullQueueHandler, executor.getFullQueueHandler());
                }
        );
    }

    @Test(timeout = 10000)
    public void testFallbackGenericExecutor() {
        TestThreadFactory threadFactory = new TestThreadFactory();
        ContextAwareTaskExecutor fallback = newTestFallbackExecutor();
        testUnwrapped(ThreadPoolTaskExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setMaxThreadCount(3);
                    builder.setIdleTimeout(TimeDuration.nanos(534));
                    builder.setMaxQueueSize(1);
                    builder.setThreadFactory(threadFactory);
                    builder.setManualShutdownRequired(false);
                    builder.setFullQueueHandlerToFallback(fallback);
                },
                (executor, wrapper) -> {
                    assertTrue("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxThreadCount", 3, executor.getMaxThreadCount());
                    assertEquals("maxQueueSize", 1, executor.getMaxQueueSize());
                    assertEquals("idleTimeout", 534L, executor.getIdleTimeout(TimeUnit.NANOSECONDS));
                    assertSame(threadFactory, executor.getThreadFactory());
                    verifyFallback(3, wrapper, fallback);
                }
        );
    }

    @Test
    public void testDisableTimeout() {
        test(SimpleThreadPoolTaskExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setIdleTimeout(null);
                },
                executor -> {
                    assertFalse("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxThreadCount", 1, executor.getMaxThreadCount());
                    assertEquals("maxQueueSize", Integer.MAX_VALUE, executor.getMaxQueueSize());
                    verifyDefaultThreadFactory(executor.getThreadFactory(), false, "MY-TEST-POOL");
                    assertNull("fullQueueHandler", executor.getFullQueueHandler());
                }
        );
    }

    @Test
    public void testSetValuesForNonTimeout1() {
        TestThreadFactory threadFactory = new TestThreadFactory();
        TestFullQueueHandler fullQueueHandler = new TestFullQueueHandler();
        test(SimpleThreadPoolTaskExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setMaxThreadCount(3);
                    builder.setIdleTimeout(null);
                    builder.setMaxQueueSize(12);
                    builder.setThreadFactory(threadFactory);
                    builder.setManualShutdownRequired(false);
                    builder.setFullQueueHandler(fullQueueHandler);
                },
                executor -> {
                    assertTrue("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxThreadCount", 3, executor.getMaxThreadCount());
                    assertEquals("maxQueueSize", 12, executor.getMaxQueueSize());
                    assertSame(threadFactory, executor.getThreadFactory());
                    assertSame(fullQueueHandler, executor.getFullQueueHandler());
                }
        );
    }

    @Test
    public void testSetValuesForNonTimeout2() {
        TestThreadFactory threadFactory = new TestThreadFactory();
        TestFullQueueHandler fullQueueHandler = new TestFullQueueHandler();
        test(SimpleThreadPoolTaskExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setMaxThreadCount(1);
                    builder.setIdleTimeout(TimeDuration.nanos(Long.MAX_VALUE));
                    builder.setMaxQueueSize(12);
                    builder.setThreadFactory(threadFactory);
                    builder.setManualShutdownRequired(false);
                    builder.setFullQueueHandler(fullQueueHandler);
                },
                executor -> {
                    assertTrue("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxThreadCount", 1, executor.getMaxThreadCount());
                    assertEquals("maxQueueSize", 12, executor.getMaxQueueSize());
                    assertSame(threadFactory, executor.getThreadFactory());
                    assertSame(fullQueueHandler, executor.getFullQueueHandler());
                }
        );
    }

    @Test(timeout = 10000)
    public void testFallbackNoTimeoutExecutor() {
        TestThreadFactory threadFactory = new TestThreadFactory();
        ContextAwareTaskExecutor fallback = newTestFallbackExecutor();

        testUnwrapped(SimpleThreadPoolTaskExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setMaxThreadCount(1);
                    builder.setIdleTimeout(TimeDuration.nanos(Long.MAX_VALUE));
                    builder.setMaxQueueSize(1);
                    builder.setThreadFactory(threadFactory);
                    builder.setManualShutdownRequired(false);
                    builder.setFullQueueHandlerToFallback(fallback);
                },
                (executor, wrapper) -> {
                    assertTrue("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxThreadCount", 1, executor.getMaxThreadCount());
                    assertEquals("maxQueueSize", 1, executor.getMaxQueueSize());
                    assertSame(threadFactory, executor.getThreadFactory());
                    verifyFallback(1, wrapper, fallback);
                }
        );
    }


    private static <E> E verifyType(Class<? extends E> type, Object obj) {
        if (!type.isInstance(obj)) {
            throw new AssertionError(obj + " is not of type " + type.getName());
        }
        return type.cast(obj);
    }

    private static final class TestThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }

    private static final class TestFullQueueHandler implements FullQueueHandler {
        @Override
        public RuntimeException tryGetFullQueueException(CancellationToken cancelToken) {
            throw new AssertionError("Not expected to be called.");
        }
    }

    private interface TestThreadPoolFactory {
        public MonitorableTaskExecutorService create(String poolName, Consumer<? super ThreadPoolBuilder> config);

        public default <E> E create(
                Class<? extends E> type,
                String poolName,
                Consumer<? super ThreadPoolBuilder> config) {

            return verifyType(type, create(poolName, config));
        }
    }
}
