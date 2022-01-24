package org.jtrim2.executor;

import java.util.Arrays;
import java.util.Collection;
import org.jtrim2.testutils.executor.ContextAwareExecutorTests;
import org.jtrim2.testutils.executor.GenericExecutorServiceTests;
import org.jtrim2.testutils.executor.TestExecutorFactory;
import org.junit.Test;

public class SimpleThreadPoolTaskExecutorTest extends CommonThreadPoolTest<SimpleThreadPoolTaskExecutor> {
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

    public SimpleThreadPoolTaskExecutorTest() {
        super(properties -> {
            SimpleThreadPoolTaskExecutor result = new SimpleThreadPoolTaskExecutor(
                    properties.getPoolName(),
                    properties.getMaxThreadCount(),
                    properties.getMaxQueueSize(),
                    properties.getThreadFactory()
            );

            result.setFullQueueHandler(properties.getFullQueueHandler());
            if (!properties.isNeedShutdown()) {
                result.dontNeedShutdown();
            }
            return result;
        });
    }

    private static int getThreadCount() {
        return BackgroundExecutorTests.getThreadCount();
    }

    private static Collection<TestExecutorFactory<SimpleThreadPoolTaskExecutor>> testFactories() {
        return GenericExecutorServiceTests.executorServices(Arrays.asList(
                SimpleThreadPoolTaskExecutorTest::create1,
                SimpleThreadPoolTaskExecutorTest::create2,
                SimpleThreadPoolTaskExecutorTest::create3,
                SimpleThreadPoolTaskExecutorTest::create4
        ));
    }

    private static SimpleThreadPoolTaskExecutor create(String poolName, int maxThreadCount, int maxQueueSize) {
        return new SimpleThreadPoolTaskExecutor(
                poolName,
                maxThreadCount,
                maxQueueSize,
                new ExecutorsEx.NamedThreadFactory(false, poolName)
        );
    }

    private static SimpleThreadPoolTaskExecutor create1() {
        return create("SimpleThreadPoolTaskExecutor-Single", 1, Integer.MAX_VALUE);
    }

    private static SimpleThreadPoolTaskExecutor create2() {
        return create("SimpleThreadPoolTaskExecutor-Multi", getThreadCount(), Integer.MAX_VALUE);
    }

    private static SimpleThreadPoolTaskExecutor create3() {
        return create(
                "SimpleThreadPoolTaskExecutor-Multi-Large-Queue",
                getThreadCount(),
                Integer.MAX_VALUE
        );
    }

    private static SimpleThreadPoolTaskExecutor create4() {
        return create(
                "SimpleThreadPoolTaskExecutor-Multi-Short-Buffer",
                getThreadCount(),
                1
        );
    }

    @Test(timeout = 10000)
    public void testQueuedTasks() throws Exception {
        testQueuedTasks(null);
    }

    @Test
    @Override
    public void testNotAutoFinalize() {
        // Note: We are not running testNotAutoFinalize, because that would leave a garbage thread which we can't stop.
    }
}
