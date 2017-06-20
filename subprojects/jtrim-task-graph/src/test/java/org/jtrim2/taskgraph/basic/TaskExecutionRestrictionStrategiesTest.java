package org.jtrim2.taskgraph.basic;

import java.util.ArrayList;
import java.util.List;
import org.jtrim2.taskgraph.TaskNodeKey;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class TaskExecutionRestrictionStrategiesTest {
    public static class EagerStrategyTest extends AbstractTaskExecutionRestrictionStrategyFactoryTest {
        public EagerStrategyTest() {
            super(() -> new TaskExecutionRestrictionStrategyFactory[]{
                TaskExecutionRestrictionStrategies.eagerStrategy()
            });
        }

        @Test
        public void testValues() {
            DirectedGraph.Builder<TaskNodeKey<?, ?>> builder = new DirectedGraph.Builder<>();
            DependencyDag<TaskNodeKey<?, ?>> dag = new DependencyDag<>(builder.build());

            List<Runnable> actions = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                actions.add(mock(Runnable.class));
            }

            List<RestrictableNode> nodes = new ArrayList<>(actions.size());
            for (int i = 0; i < actions.size(); i++) {
                nodes.add(new RestrictableNode(TestNodes.node("key" + i), actions.get(i)));
            }

            TaskExecutionRestrictionStrategy strategy
                    = TaskExecutionRestrictionStrategies.eagerStrategy().buildStrategy(dag, nodes);

            actions.forEach((action) -> {
                verify(action).run();
            });

            // Just call them to verify that they are callable without failure.
            for (RestrictableNode node : nodes) {
                strategy.setNodeComputed(node.getNodeKey());
            }
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
