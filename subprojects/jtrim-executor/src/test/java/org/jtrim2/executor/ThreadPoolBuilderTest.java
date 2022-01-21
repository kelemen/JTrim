package org.jtrim2.executor;

import java.util.Arrays;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.testutils.TestParameterRule;
import org.jtrim2.utils.TimeDuration;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

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

    private <E extends MonitorableTaskExecutorService> void test(
            Class<? extends E> type,
            String poolName,
            Consumer<? super ThreadPoolBuilder> config,
            Consumer<? super E> testAction) {

        E executor = factory.create(type, poolName, config);
        try {
            testAction.accept(executor);
        } finally {
            executor.shutdown();
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
        test(SingleThreadedExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setMaxQueueSize(10);
                    builder.setIdleTimeout(TimeDuration.nanos(239));
                    builder.setThreadFactory(threadFactory);
                    builder.setMaxThreadCount(1);
                    builder.setManualShutdownRequired(false);
                },
                executor -> {
                    assertTrue("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxQueueSize", 10, executor.getMaxQueueSize());
                    assertEquals("idleTimeout", 239L, executor.getIdleTimeout(TimeUnit.NANOSECONDS));
                    assertSame(threadFactory, executor.getThreadFactory());
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
                }
        );
    }

    @Test
    public void testSetValuesForNonTimeout1() {
        TestThreadFactory threadFactory = new TestThreadFactory();
        test(SimpleThreadPoolTaskExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setMaxThreadCount(3);
                    builder.setIdleTimeout(null);
                    builder.setMaxQueueSize(12);
                    builder.setThreadFactory(threadFactory);
                    builder.setManualShutdownRequired(false);
                },
                executor -> {
                    assertTrue("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxThreadCount", 3, executor.getMaxThreadCount());
                    assertEquals("maxQueueSize", 12, executor.getMaxQueueSize());
                    assertSame(threadFactory, executor.getThreadFactory());
                }
        );
    }

    @Test
    public void testSetValuesForNonTimeout2() {
        TestThreadFactory threadFactory = new TestThreadFactory();
        test(SimpleThreadPoolTaskExecutor.class, "MY-TEST-POOL",
                builder -> {
                    builder.setMaxThreadCount(1);
                    builder.setIdleTimeout(TimeDuration.nanos(Long.MAX_VALUE));
                    builder.setMaxQueueSize(12);
                    builder.setThreadFactory(threadFactory);
                    builder.setManualShutdownRequired(false);
                },
                executor -> {
                    assertTrue("finalized", executor.isFinalized());
                    assertEquals("MY-TEST-POOL", executor.getPoolName());
                    assertEquals("maxThreadCount", 1, executor.getMaxThreadCount());
                    assertEquals("maxQueueSize", 12, executor.getMaxQueueSize());
                    assertSame(threadFactory, executor.getThreadFactory());
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

        public default MonitorableTaskExecutorService create(String poolName) {
            return create(poolName, Tasks.noOpConsumer());
        }

        public default <E> E create(
                Class<? extends E> type,
                String poolName,
                Consumer<? super ThreadPoolBuilder> config) {

            return verifyType(type, create(poolName, config));
        }
    }
}
