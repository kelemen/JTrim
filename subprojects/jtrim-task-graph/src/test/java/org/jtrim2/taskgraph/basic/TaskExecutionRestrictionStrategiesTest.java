package org.jtrim2.taskgraph.basic;

import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

public class TaskExecutionRestrictionStrategiesTest {
    public static class EagerStrategyTest extends AbstractTaskExecutionRestrictionStrategyFactoryTest {
        public EagerStrategyTest() {
            super(() -> new TaskExecutionRestrictionStrategyFactory[]{
                TaskExecutionRestrictionStrategies.eagerStrategy()
            });
        }
    }

    public static class WeakLeafsStrategyTest extends AbstractTaskExecutionRestrictionStrategyFactoryTest {
        public WeakLeafsStrategyTest() {
            super(() -> new TaskExecutionRestrictionStrategyFactory[]{
                TaskExecutionRestrictionStrategies.weakLeafsOfEndNodeRestrictingStrategy(1),
                TaskExecutionRestrictionStrategies.weakLeafsOfEndNodeRestrictingStrategy(2),
                TaskExecutionRestrictionStrategies.weakLeafsOfEndNodeRestrictingStrategy(3),
                TaskExecutionRestrictionStrategies.weakLeafsOfEndNodeRestrictingStrategy(100)
            });
        }
    }

    @Test
    public void testUtility() {
        TestUtils.testUtilityClass(TaskExecutionRestrictionStrategies.class);
    }
}
