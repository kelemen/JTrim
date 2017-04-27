package org.jtrim.taskgraph.basic;

import java.util.ArrayList;
import java.util.List;
import org.jtrim.taskgraph.TaskNodeKey;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class EagerTaskExecutionRestrictionStrategyBuilderTest
extends
        AbstractTaskExecutionRestrictionStrategyFactoryTest {

    public EagerTaskExecutionRestrictionStrategyBuilderTest() {
        super(() -> new TaskExecutionRestrictionStrategyFactory[]{
            EagerTaskExecutionRestrictionStrategyBuilder.EAGER
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
                = EagerTaskExecutionRestrictionStrategyBuilder.EAGER.buildStrategy(dag, nodes);

        actions.forEach((action) -> {
            verify(action).run();
        });

        // Just call them to verify that they are callable without failure.
        for (RestrictableNode node : nodes) {
            strategy.setNodeComputed(node.getNodeKey());
        }
    }
}
