package org.jtrim.taskgraph.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableFunction;
import org.jtrim.taskgraph.TaskFactory;
import org.jtrim.taskgraph.TaskFactoryConfig;
import org.jtrim.taskgraph.TaskFactoryDefiner;
import org.jtrim.taskgraph.TaskFactoryGroupConfigurer;
import org.jtrim.taskgraph.TaskFactoryKey;
import org.jtrim.taskgraph.TaskFactoryProperties;
import org.jtrim.taskgraph.TaskFactorySetup;
import org.jtrim.taskgraph.TaskGraphBuilder;
import org.jtrim.taskgraph.TaskGraphBuilderProperties;
import org.jtrim.taskgraph.TaskGraphExecutor;
import org.jtrim.taskgraph.TaskNodeCreateArgs;
import org.jtrim.taskgraph.TaskNodeKey;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CollectingTaskGraphDefConfigurerTest {
    private static final TaskFactoryProperties DUMMY_PROPERTIES = new TaskFactoryProperties.Builder().build();

    private static TaskFactoryKey<TestOutput, TestArg> testKey(Object customKey) {
        return new TaskFactoryKey<>(TestOutput.class, TestArg.class, customKey);
    }

    private Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> configureTest(
            Consumer<CollectingTaskGraphDefConfigurer> configurerAction) {

        TaskGraphBuilder graphBuilder = new TestTaskGraphBuilder();

        Runnable builderCall = mock(Runnable.class);
        AtomicReference<Collection<TaskFactoryConfig<?, ?>>> configsRef = new AtomicReference<>();
        TaskGraphBuilderFactory builderFactory = (Collection<? extends TaskFactoryConfig<?, ?>> configs) -> {
            builderCall.run();
            configsRef.set(new ArrayList<>(configs));
            return graphBuilder;
        };

        CollectingTaskGraphDefConfigurer configurer = new CollectingTaskGraphDefConfigurer(builderFactory);
        configurerAction.accept(configurer);

        TaskGraphBuilder configuredBuilder = configurer.build();
        assertSame(graphBuilder, configuredBuilder);

        verify(builderCall).run();

        Collection<TaskFactoryConfig<?, ?>> configs = configsRef.get();
        Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> result = new HashMap<>();
        configs.forEach((config) -> result.put(config.getDefKey(), config));

        assertEquals("TaskFactoryKey count", configs.size(), result.size());

        return result;
    }

    private static void verifyConfig(
            TaskFactoryConfig<?, ?> config,
            TaskFactoryGroupConfigurer groupConfigurer,
            TaskFactorySetup<?, ?> factorySetup) {
        assertNotNull(config);

        assertSame(groupConfigurer, config.getConfigurer());
        assertSame(factorySetup, config.getSetup());
    }

    private static void verifyConfig(
            TaskFactoryConfig<?, ?> config,
            TaskFactoryGroupConfigurer groupConfigurer,
            TaskFactory<TestOutput, TestArg> taskFactory) throws Exception {
        assertNotNull(config);

        assertSame(groupConfigurer, config.getConfigurer());
        assertSame(taskFactory, config.getSetup().setup(DUMMY_PROPERTIES));
    }

    @Test
    public void testAddOne() {
        TaskFactoryGroupConfigurer groupConfigurer = new TestTaskFactoryGroupConfigurer();
        TaskFactorySetup<TestOutput, TestArg> factorySetup = new TestTaskFactorySetup<>();

        Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> configs = configureTest((configurer) -> {
            TaskFactoryDefiner definer = configurer.factoryGroupDefiner(groupConfigurer);

            TaskFactoryConfig<TestOutput, TestArg> prev = definer.defineFactory(testKey("A"), factorySetup);
            assertNull(prev);
        });

        assertEquals("config count", 1, configs.size());
        verifyConfig(configs.get(testKey("A")), groupConfigurer, factorySetup);
    }

    @Test
    public void testAddOneSimple1() throws Exception {
        TaskFactoryGroupConfigurer groupConfigurer = new TestTaskFactoryGroupConfigurer();
        TaskFactory<TestOutput, TestArg> taskFactory = new TestTaskFactory<>();

        Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> configs = configureTest((configurer) -> {
            TaskFactoryDefiner definer = configurer.factoryGroupDefiner(groupConfigurer);

            TaskFactoryConfig<TestOutput, TestArg> prev = definer.defineSimpleFactory(testKey("A"), taskFactory);
            assertNull(prev);
        });

        assertEquals("config count", 1, configs.size());
        verifyConfig(configs.get(testKey("A")), groupConfigurer, taskFactory);
    }

    @Test
    public void testAddOneSimple2() throws Exception {
        TaskFactoryGroupConfigurer groupConfigurer = new TestTaskFactoryGroupConfigurer();
        TaskFactory<TestOutput, TestArg> taskFactory = new TestTaskFactory<>();

        Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> configs = configureTest((configurer) -> {
            TaskFactoryDefiner definer = configurer.factoryGroupDefiner(groupConfigurer);

            TaskFactoryConfig<TestOutput, TestArg> prev
                    = definer.defineSimpleFactory(TestOutput.class, TestArg.class, taskFactory);
            assertNull(prev);
        });

        assertEquals("config count", 1, configs.size());
        verifyConfig(configs.get(testKey(null)), groupConfigurer, taskFactory);
    }

    @Test
    public void testOverwriteFromSameGroup() {
        TaskFactoryGroupConfigurer groupConfigurer = new TestTaskFactoryGroupConfigurer();
        TaskFactorySetup<TestOutput, TestArg> factorySetup1 = new TestTaskFactorySetup<>();
        TaskFactorySetup<TestOutput, TestArg> factorySetup2 = new TestTaskFactorySetup<>();

        Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> configs = configureTest((configurer) -> {
            TaskFactoryDefiner definer = configurer.factoryGroupDefiner(groupConfigurer);

            definer.defineFactory(testKey("A"), factorySetup1);
            TaskFactoryConfig<TestOutput, TestArg> prev = definer.defineFactory(testKey("A"), factorySetup2);
            verifyConfig(prev, groupConfigurer, factorySetup1);
        });

        assertEquals("config count", 1, configs.size());
        verifyConfig(configs.get(testKey("A")), groupConfigurer, factorySetup2);
    }

    @Test
    public void testOverwriteFromDifferentGroup() {
        TaskFactoryGroupConfigurer groupConfigurer1 = new TestTaskFactoryGroupConfigurer();
        TaskFactoryGroupConfigurer groupConfigurer2 = new TestTaskFactoryGroupConfigurer();

        TaskFactorySetup<TestOutput, TestArg> factorySetup1 = new TestTaskFactorySetup<>();
        TaskFactorySetup<TestOutput, TestArg> factorySetup2 = new TestTaskFactorySetup<>();

        Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> configs = configureTest((configurer) -> {
            TaskFactoryDefiner definer = configurer.factoryGroupDefiner(groupConfigurer1);
            definer.defineFactory(testKey("A"), factorySetup1);

            definer = configurer.factoryGroupDefiner(groupConfigurer2);
            TaskFactoryConfig<TestOutput, TestArg> prev = definer.defineFactory(testKey("A"), factorySetup2);
            verifyConfig(prev, groupConfigurer1, factorySetup1);
        });

        assertEquals("config count", 1, configs.size());
        verifyConfig(configs.get(testKey("A")), groupConfigurer2, factorySetup2);
    }

    @Test
    public void testMixedSetup() {
        TaskFactoryGroupConfigurer groupConfigurer1 = new TestTaskFactoryGroupConfigurer();
        TaskFactoryGroupConfigurer groupConfigurer2 = new TestTaskFactoryGroupConfigurer();

        TaskFactorySetup<TestOutput, TestArg> factorySetupA = new TestTaskFactorySetup<>();
        TaskFactorySetup<TestOutput, TestArg> factorySetupB = new TestTaskFactorySetup<>();
        TaskFactorySetup<TestOutput, TestArg> factorySetupC = new TestTaskFactorySetup<>();
        TaskFactorySetup<TestOutput, TestArg> factorySetupD = new TestTaskFactorySetup<>();

        Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> configs = configureTest((configurer) -> {
            TaskFactoryDefiner definer1 = configurer.factoryGroupDefiner(groupConfigurer1);
            definer1.defineFactory(testKey("A"), factorySetupA);
            definer1.defineFactory(testKey("B"), factorySetupB);

            TaskFactoryDefiner definer2 = configurer.factoryGroupDefiner(groupConfigurer2);
            definer2.defineFactory(testKey("C"), factorySetupC);
            definer2.defineFactory(testKey("D"), factorySetupD);
        });

        assertEquals("config count", 4, configs.size());
        verifyConfig(configs.get(testKey("A")), groupConfigurer1, factorySetupA);
        verifyConfig(configs.get(testKey("B")), groupConfigurer1, factorySetupB);
        verifyConfig(configs.get(testKey("C")), groupConfigurer2, factorySetupC);
        verifyConfig(configs.get(testKey("D")), groupConfigurer2, factorySetupD);
    }

    public <R, I> void testNoOverwriteForDifferentType(Class<R> outputType, Class<I> argType) {
        TaskFactoryKey<TestOutput, TestArg> key1 = testKey("A");
        TaskFactoryKey<R, I> key2 = new TaskFactoryKey<>(outputType, argType, "A");

        TaskFactoryGroupConfigurer groupConfigurer1 = new TestTaskFactoryGroupConfigurer();

        TaskFactorySetup<TestOutput, TestArg> factorySetupA = new TestTaskFactorySetup<>();
        TaskFactorySetup<R, I> factorySetupB = new TestTaskFactorySetup<>();

        Map<TaskFactoryKey<?, ?>, TaskFactoryConfig<?, ?>> configs = configureTest((configurer) -> {
            TaskFactoryDefiner definer1 = configurer.factoryGroupDefiner(groupConfigurer1);
            definer1.defineFactory(key1, factorySetupA);
            TaskFactoryConfig<R, I> prev = definer1.defineFactory(key2, factorySetupB);
            assertNull(prev);
        });

        assertEquals("config count", 2, configs.size());
        verifyConfig(configs.get(key1), groupConfigurer1, factorySetupA);
        verifyConfig(configs.get(key2), groupConfigurer1, factorySetupB);
    }

    @Test
    public void testNoOverwriteForDifferentArgType() {
        testNoOverwriteForDifferentType(TestOutput.class, Object.class);
    }

    @Test
    public void testNoOverwriteForDifferentOutputType() {
        testNoOverwriteForDifferentType(Object.class, TestArg.class);
    }

    private static final class TestOutput {
    }

    private static final class TestArg {
    }

    private static class TestTaskFactoryGroupConfigurer implements TaskFactoryGroupConfigurer {
        public TestTaskFactoryGroupConfigurer() {
        }

        @Override
        public void setup(TaskFactoryProperties.Builder properties) {
        }
    }

    private static class TestTaskFactory<R, I> implements TaskFactory<R, I> {
        @Override
        public CancelableFunction<R> createTaskNode(
                CancellationToken cancelToken,
                TaskNodeCreateArgs<I> nodeDef) throws Exception {
            return (taskCancelToken) -> null;
        }
    }

    private static class TestTaskFactorySetup<R, I> implements TaskFactorySetup<R, I> {
        private final TaskFactory<R, I> testFactory;

        public TestTaskFactorySetup() {
            this.testFactory = new TestTaskFactory<>();
        }

        @Override
        public TaskFactory<R, I> setup(TaskFactoryProperties properties) throws Exception {
            return testFactory;
        }
    }

    private static class TestTaskGraphBuilder implements TaskGraphBuilder {
        private final TaskGraphBuilderProperties.Builder properties;

        public TestTaskGraphBuilder() {
            this.properties = new TaskGraphBuilderProperties.Builder();
        }

        @Override
        public void addNode(TaskNodeKey<?, ?> nodeKey) {
        }

        @Override
        public TaskGraphBuilderProperties.Builder properties() {
            return properties;
        }

        @Override
        public CompletionStage<TaskGraphExecutor> buildGraph(CancellationToken cancelToken) {
            return new CompletableFuture<>();
        }
    }
}
