package org.jtrim2.taskgraph;

import java.util.Arrays;
import org.jtrim2.taskgraph.basic.TaskExecutionRestrictionStrategies;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

public class TaskGraphExecutorsTest {
    // These tests are very weak, they are just testing that the factories actually
    // return a reasonable graph executor. There are better tests are for the actual implementations.

    public static class EagerExecutorTest extends AbstractGraphExecutorTest {
        public EagerExecutorTest() {
            super(Arrays.asList(() -> TaskGraphExecutors.newEagerExecutor()));
        }
    }

    public static class WeakLeafExecutorTest extends AbstractGraphExecutorTest {
        public WeakLeafExecutorTest() {
            super(Arrays.asList(
                    () -> TaskGraphExecutors.newWeakLeafRestricterExecutor(1),
                    () -> TaskGraphExecutors.newWeakLeafRestricterExecutor(2),
                    () -> TaskGraphExecutors.newWeakLeafRestricterExecutor(3),
                    () -> TaskGraphExecutors.newWeakLeafRestricterExecutor(100)));
        }
    }

    public static class RestricatbleExecutorTest extends AbstractGraphExecutorTest {
        public RestricatbleExecutorTest() {
            super(Arrays.asList(
                    () -> TaskGraphExecutors.newRestrictableExecutor(
                            TaskExecutionRestrictionStrategies.eagerStrategy()),
                    () -> TaskGraphExecutors.newRestrictableExecutor(
                            TaskExecutionRestrictionStrategies.weakLeafsOfEndNodeRestrictingStrategy(1)),
                    () -> TaskGraphExecutors.newRestrictableExecutor(
                            TaskExecutionRestrictionStrategies.weakLeafsOfEndNodeRestrictingStrategy(10))));
        }
    }

    @Test
    public void testUtility() {
        TestUtils.testUtilityClass(TaskGraphExecutors.class);
    }
}