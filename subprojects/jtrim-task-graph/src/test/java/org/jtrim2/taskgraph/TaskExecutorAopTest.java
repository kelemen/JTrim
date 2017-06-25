package org.jtrim2.taskgraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TaskExecutorAopTest {
    @Test
    public void testUtility() {
        TestUtils.testUtilityClass(TaskExecutorAop.class);
    }

    private static TaskFactoryKey<TestOutput, TestInput> factoryKey(Object customKey) {
        return new TaskFactoryKey<>(TestOutput.class, TestInput.class, customKey);
    }

    @Test
    public void testWrapFactory() throws Exception {
        TestTaskFactoryDefiner definer = new TestTaskFactoryDefiner();
        TestTaskNodeWrapper testTaskNodeWrapper = new TestTaskNodeWrapper();
        TaskFactoryDefiner wrappedDefiner = TaskExecutorAop.wrapNode(definer, testTaskNodeWrapper);

        TaskFactoryKey<TestOutput, TestInput> factoryKey = factoryKey("Test-Factory");
        TestOutput expectedResult = new TestOutput("Test-Result");

        TaskInputBinder expectedInputBinder = mock(TaskInputBinder.class);
        TestInput expectedFactoryArg = new TestInput("Test-Input");
        CancellationToken expectedCancelToken = Cancellation.createCancellationSource().getToken();

        Runnable factoryCalled = mock(Runnable.class);
        wrappedDefiner.defineSimpleFactory(factoryKey, (cancelToken, nodeDef) -> {
            assertSame("cancelToken", expectedCancelToken, cancelToken);
            assertSame("factoryArg", expectedFactoryArg, nodeDef.factoryArg());
            assertSame("inputs", expectedInputBinder, nodeDef.inputs());
            factoryCalled.run();
            return taskCancelToken -> expectedResult;
        });

        TaskFactoryConfig<TestOutput, TestInput> addedConfig = definer.getSingleConfig();

        verifyZeroInteractions(factoryCalled);

        TestOutput actualOutput = invokeFactory(
                addedConfig,
                expectedCancelToken,
                new TaskNodeKey<>(factoryKey, expectedFactoryArg),
                expectedInputBinder);

        verify(factoryCalled).run();

        assertEquals(Arrays.asList(factoryKey), testTaskNodeWrapper.getKeys());
        assertEquals(expectedResult, actualOutput);
    }

    private static TaskFactoryProperties createProperties(TaskFactoryConfig<TestOutput, TestInput> config) {
        TaskFactoryProperties.Builder result = new TaskFactoryProperties.Builder();
        config.getConfigurer().setup(result);
        return result.build();
    }

    private static TestOutput invokeFactory(
            TaskFactoryConfig<TestOutput, TestInput> config,
            CancellationToken cancelToken,
            TaskNodeKey<TestOutput, TestInput> nodeKey,
            TaskInputBinder inputs) throws Exception {

        TaskFactoryProperties properties = createProperties(config);
        TaskFactory<TestOutput, TestInput> factory = config.getSetup().setup(properties);

        TaskNodeCreateArgs<TestOutput, TestInput> createArgs = new TaskNodeCreateArgs<>(
                nodeKey,
                new TaskNodeProperties.Builder(properties.getDefaultNodeProperties()).build(),
                inputs);

        CancelableFunction<TestOutput> node = factory.createTaskNode(cancelToken, createArgs);

        return node.execute(cancelToken);
    }

    private static final class TestOutput {
        private final String str;

        public TestOutput(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return "TestOutput{" + str + '}';
        }
    }

    private static final class TestInput {
        private final String str;

        public TestInput(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return "TestInput{" + str + '}';
        }
    }

    private static class TestTaskFactoryDefiner implements TaskFactoryDefiner {
        private final TaskFactoryGroupConfigurer groupConfigurer;
        private final List<TaskFactoryConfig<?, ?>> configs;

        public TestTaskFactoryDefiner() {
            this(properties -> { });
        }

        public TestTaskFactoryDefiner(TaskFactoryGroupConfigurer groupConfigurer) {
            this.groupConfigurer = groupConfigurer;
            this.configs = new ArrayList<>();
        }

        @SuppressWarnings("unchecked")
        public TaskFactoryConfig<TestOutput, TestInput> getSingleConfig() {
            assertEquals("configs.size()", 1, configs.size());
            return (TaskFactoryConfig<TestOutput, TestInput>) configs.get(0);
        }

        @Override
        public <R, I> TaskFactoryConfig<R, I> defineFactory(TaskFactoryKey<R, I> defKey, TaskFactorySetup<R, I> setup) {
            TaskFactoryConfig<R, I> config = new TaskFactoryConfig<>(defKey, groupConfigurer, setup);
            configs.add(config);
            return config;
        }
    }

    private static class TestTaskNodeWrapper implements TaskNodeWrapper {
        private final List<TaskFactoryKey<?, ?>> keys;

        public TestTaskNodeWrapper() {
            this.keys = new ArrayList<>();
        }

        public List<TaskFactoryKey<?, ?>> getKeys() {
            return keys;
        }

        @Override
        public <R, I> CancelableFunction<R> createTaskNode(
                CancellationToken cancelToken,
                TaskNodeCreateArgs<R, I> nodeDef,
                TaskFactoryKey<R, I> factoryKey,
                TaskFactory<R, I> wrappedFactory) throws Exception {
            keys.add(factoryKey);
            return wrappedFactory.createTaskNode(cancelToken, nodeDef);
        }
    }
}
