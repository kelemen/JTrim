package org.jtrim2.taskgraph;

import org.jtrim2.taskgraph.basic.TaskExecutionRestrictionStrategies;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

public class TaskGraphExecutorsTest {
    // These tests are very weak, they are just testing that the factories actually
    // return a reasonable graph executor. There are better tests are for the actual implementations.

    public static class EagerExecutorTest extends AbstractGraphExecutorTest {
        public EagerExecutorTest() {
            super(() -> TaskGraphExecutors.newEagerExecutor());
        }
    }

    public abstract static class WeakLeafExecutorTest extends AbstractGraphExecutorTest {
        public WeakLeafExecutorTest(int maxLeafCount) {
            super(() -> TaskGraphExecutors.newWeakLeafRestricterExecutor(maxLeafCount));
        }
    }

    public static class WeakLeafExecutorTest1 extends WeakLeafExecutorTest {
        public WeakLeafExecutorTest1() {
            super(1);
        }
    }

    public static class WeakLeafExecutorTest2 extends WeakLeafExecutorTest {
        public WeakLeafExecutorTest2() {
            super(2);
        }
    }

    public static class WeakLeafExecutorTest3 extends WeakLeafExecutorTest {
        public WeakLeafExecutorTest3() {
            super(3);
        }
    }

    public static class WeakLeafExecutorTest100 extends WeakLeafExecutorTest {
        public WeakLeafExecutorTest100() {
            super(100);
        }
    }

    public static class RestricatbleExecutorTestEager extends AbstractGraphExecutorTest {
        public RestricatbleExecutorTestEager() {
            super(() -> TaskGraphExecutors.newRestrictableExecutor(
                    TaskExecutionRestrictionStrategies.eagerStrategy()));
        }
    }

    public static class RestricatbleExecutorTest1 extends AbstractGraphExecutorTest {
        public RestricatbleExecutorTest1() {
            super(() -> TaskGraphExecutors.newRestrictableExecutor(
                    TaskExecutionRestrictionStrategies.weakLeafsOfEndNodeRestrictingStrategy(1)));
        }
    }

    public static class RestricatbleExecutorTest10 extends AbstractGraphExecutorTest {
        public RestricatbleExecutorTest10() {
            super(() -> TaskGraphExecutors.newRestrictableExecutor(
                    TaskExecutionRestrictionStrategies.weakLeafsOfEndNodeRestrictingStrategy(10)));
        }
    }

    @Test
    public void testUtility() {
        TestUtils.testUtilityClass(TaskGraphExecutors.class);
    }
}
