package org.jtrim2.taskgraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jtrim2.cancel.Cancellation;
import org.junit.Test;

import static org.junit.Assert.*;

public abstract class AbstractGraphExecutorTest {
    private final Collection<Supplier<TaskGraphDefConfigurer>> graphConfigurers;

    public AbstractGraphExecutorTest(Collection<Supplier<TaskGraphDefConfigurer>> graphConfigurers) {
        this.graphConfigurers = new ArrayList<>(graphConfigurers);
    }

    private void test(Consumer<TaskGraphDefConfigurer> graphConfigurerAction) {
        graphConfigurers.forEach((graphConfigurerFactory) -> {
            graphConfigurerAction.accept(graphConfigurerFactory.get());
        });
    }

    private static <R, I> TaskNodeKey<R, I> nodeKey(Class<R> outputType, Class<I> argType, I arg) {
        return new TaskNodeKey<>(new TaskFactoryKey<>(outputType, argType), arg);
    }

    @Test
    public void testDoubleSplitGraph() {
        test((configurer) -> {
            TaskFactoryDefiner factoryGroup1 = configurer.factoryGroupDefiner((properties) -> {
            });
            factoryGroup1.defineSimpleFactory(TestOutput1.class, String.class, (cancelToken, nodeDef) -> {
                return (taskCancelToken) -> new TestOutput1(nodeDef.factoryArg());
            });

            factoryGroup1.defineSimpleFactory(TestOutput2.class, String.class, (cancelToken, nodeDef) -> {
                String factoryArg = nodeDef.factoryArg();

                TaskInputRef<TestOutput1> inputRef1 = nodeDef.inputs()
                        .bindInput(TestOutput1.class, String.class, factoryArg + ".a");
                TaskInputRef<TestOutput1> inputRef2 = nodeDef.inputs()
                        .bindInput(TestOutput1.class, String.class, factoryArg + ".b");

                return (taskCancelToken) -> {
                    return new TestOutput2(nodeDef.factoryArg(), inputRef1.consumeInput(), inputRef2.consumeInput());
                };
            });

            TaskNodeKey<TestOutput2, String> nodeKey1 = nodeKey(TestOutput2.class, String.class, "x");
            TaskNodeKey<TestOutput2, String> nodeKey2 = nodeKey(TestOutput2.class, String.class, "y");

            TaskGraphBuilder graphBuilder = configurer.build();
            graphBuilder.addNode(nodeKey1);
            graphBuilder.addNode(nodeKey2);

            AtomicReference<TaskGraphExecutionResult> resultRef = new AtomicReference<>();
            CompletionStage<TaskGraphExecutor> buildFuture = graphBuilder.buildGraph(Cancellation.UNCANCELABLE_TOKEN);
            buildFuture
                    .thenCompose((executor) -> {
                        executor.properties().addResultNodeKey(nodeKey1);
                        executor.properties().addResultNodeKey(nodeKey2);
                        return executor.execute(Cancellation.UNCANCELABLE_TOKEN);
                    })
                    .thenAccept(resultRef::set);

            TaskGraphExecutionResult result = resultRef.get();
            assertNotNull(result);

            TestOutput2 node1 = result.getResult(nodeKey1);
            assertEquals(new TestOutput2("x", new TestOutput1("x.a"), new TestOutput1("x.b")), node1);

            TestOutput2 node2 = result.getResult(nodeKey2);
            assertEquals(new TestOutput2("y", new TestOutput1("y.a"), new TestOutput1("y.b")), node2);
        });
    }

    private abstract static class AbstractTestPojo {
        private final Object[] content;

        public AbstractTestPojo(Object... content) {
            this.content = content.clone();
        }

        @Override
        public final int hashCode() {
            int hash = 5;
            hash = 97 * hash + Arrays.hashCode(content);
            return hash;
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final AbstractTestPojo other = (AbstractTestPojo)obj;
            return Arrays.equals(this.content, other.content);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + Arrays.toString(content);
        }
    }

    private static final class TestOutput1 extends AbstractTestPojo {
        public TestOutput1(Object... content) {
            super(content);
        }
    }

    private static final class TestOutput2 extends AbstractTestPojo {
        public TestOutput2(Object... content) {
            super(content);
        }
    }
}
