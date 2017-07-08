package org.jtrim2.taskgraph;

import org.jtrim2.cancel.Cancellation;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.testutils.TestObj;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class TaskFactoriesTest {
    @Test
    public void testUtility() {
        TestUtils.testUtilityClass(TaskFactories.class);
    }

    @Test
    public void testDelegateToCustomKey() throws Exception {
        String origCustomKey = "Orig-Custom-Key";
        String newCustomKey = "New-Custom-Key";

        TaskFactory<String, String> factory
                = TaskFactories.delegateToCustomKey(factoryArg -> newCustomKey + "-" + factoryArg);

        TestTaskInputBinder inputBinder = new TestTaskInputBinder(nodeKey -> {
            return nodeKey.getFactoryKey().getKey() + "-result";
        });
        TaskNodeCreateArgs<String, String> nodeDef = new TaskNodeCreateArgs<>(
                node(origCustomKey, "Test-Factory-Arg"),
                new TaskNodeProperties.Builder().build(),
                inputBinder);
        nodeDef.properties().setExecutor(new ManualTaskExecutor(true));

        CancelableFunction<String> node = factory.createTaskNode(Cancellation.UNCANCELABLE_TOKEN, nodeDef);
        assertSame(SyncTaskExecutor.getSimpleExecutor(), nodeDef.properties().build().getExecutor());

        String result = node.execute(Cancellation.UNCANCELABLE_TOKEN);
        assertEquals("New-Custom-Key-Test-Factory-Arg-result", result);

        inputBinder.verifyCalled(node("New-Custom-Key-Test-Factory-Arg", "Test-Factory-Arg"));
    }

    private static TaskNodeKey<TestOutput, TestFactoryArg> key1(Object origKey) {
        return new TaskNodeKey<>(
                new TaskFactoryKey<>(TestOutput.class, TestFactoryArg.class, "New-Custom-Key"),
                new TestFactoryArg(origKey));
    }

    @Test
    public void testDelegateTo() throws Exception {
        TaskFactory<TestOutput, TestFactoryArg2> factory = TaskFactories.delegateTo(TaskFactoriesTest::key1);

        TestTaskInputBinder inputBinder = new TestTaskInputBinder(TestOutput::new);

        TaskNodeKey<TestOutput, TestFactoryArg2> inputKey = new TaskNodeKey<>(
                new TaskFactoryKey<>(TestOutput.class, TestFactoryArg2.class, "Orig-Custom-Key"),
                new TestFactoryArg2("Orig-Factory-Arg"));

        TaskNodeCreateArgs<TestOutput, TestFactoryArg2> nodeDef = new TaskNodeCreateArgs<>(
                inputKey,
                new TaskNodeProperties.Builder().build(),
                inputBinder);
        nodeDef.properties().setExecutor(new ManualTaskExecutor(true));

        CancelableFunction<TestOutput> node = factory.createTaskNode(Cancellation.UNCANCELABLE_TOKEN, nodeDef);
        assertSame(SyncTaskExecutor.getSimpleExecutor(), nodeDef.properties().build().getExecutor());

        TestOutput result = node.execute(Cancellation.UNCANCELABLE_TOKEN);

        TaskNodeKey<TestOutput, TestFactoryArg> forwardedKey = key1(inputKey);

        inputBinder.verifyCalled(forwardedKey);
        assertEquals(new TestOutput(forwardedKey), result);
    }

    @Test
    public void testDelegateToFactoryArg() throws Exception {
        TaskFactory<TestOutput, TestFactoryArg2> factory = TaskFactories.delegateToFactoryArg(
                TestFactoryArg.class,
                TestFactoryArg::new);

        TestTaskInputBinder inputBinder = new TestTaskInputBinder(TestOutput::new);

        TaskNodeKey<TestOutput, TestFactoryArg2> inputKey = new TaskNodeKey<>(
                new TaskFactoryKey<>(TestOutput.class, TestFactoryArg2.class, "Orig-Custom-Key"),
                new TestFactoryArg2("Orig-Factory-Arg"));

        TaskNodeCreateArgs<TestOutput, TestFactoryArg2> nodeDef = new TaskNodeCreateArgs<>(
                inputKey,
                new TaskNodeProperties.Builder().build(),
                inputBinder);
        nodeDef.properties().setExecutor(new ManualTaskExecutor(true));

        CancelableFunction<TestOutput> node = factory.createTaskNode(Cancellation.UNCANCELABLE_TOKEN, nodeDef);
        assertSame(SyncTaskExecutor.getSimpleExecutor(), nodeDef.properties().build().getExecutor());

        TestOutput result = node.execute(Cancellation.UNCANCELABLE_TOKEN);

        TaskNodeKey<TestOutput, TestFactoryArg> forwardedKey = new TaskNodeKey<>(
                inputKey.getFactoryKey().withFactoryArgType(TestFactoryArg.class),
                new TestFactoryArg(inputKey.getFactoryArg()));

        inputBinder.verifyCalled(forwardedKey);
        assertEquals(new TestOutput(forwardedKey), result);
    }

    private static <R> TaskNodeKey<R, TestFactoryArg> testNodeWithOutput(Class<R> outputType) {
        return new TaskNodeKey<>(
                new TaskFactoryKey<>(outputType, TestFactoryArg.class, "Orig-Custom-Key"),
                new TestFactoryArg("Orig-Factory-Arg"));
    }

    @Test
    public void testTransformResult() throws Exception {
        TaskFactory<TestOutput, TestFactoryArg> factory = TaskFactories.transformResult(
                TestOutput2.class, TestOutput::new);

        TestTaskInputBinder inputBinder = new TestTaskInputBinder(TestOutput2::new);

        TaskNodeKey<TestOutput, TestFactoryArg> inputKey = testNodeWithOutput(TestOutput.class);

        TaskNodeCreateArgs<TestOutput, TestFactoryArg> nodeDef = new TaskNodeCreateArgs<>(
                inputKey,
                new TaskNodeProperties.Builder().build(),
                inputBinder);
        nodeDef.properties().setExecutor(new ManualTaskExecutor(true));

        CancelableFunction<TestOutput> node = factory.createTaskNode(Cancellation.UNCANCELABLE_TOKEN, nodeDef);
        TestOutput result = node.execute(Cancellation.UNCANCELABLE_TOKEN);

        TaskNodeKey<TestOutput2, TestFactoryArg> forwardedKey = testNodeWithOutput(TestOutput2.class);

        inputBinder.verifyCalled(forwardedKey);
        assertEquals(new TestOutput(new TestOutput2(forwardedKey)), result);
    }

    public static TaskNodeKey<String, String> node(Object customKey, String factoryArg) {
        return new TaskNodeKey<>(new TaskFactoryKey<>(String.class, String.class, customKey), factoryArg);
    }

    private static final class TestOutput extends TestObj {
        public TestOutput(Object strValue) {
            super(strValue);
        }
    }

    private static final class TestOutput2 extends TestObj {
        public TestOutput2(Object strValue) {
            super(strValue);
        }
    }

    private static final class TestFactoryArg extends TestObj {
        public TestFactoryArg(Object strValue) {
            super(strValue);
        }
    }

    private static final class TestFactoryArg2 extends TestObj {
        public TestFactoryArg2(Object strValue) {
            super(strValue);
        }
    }
}
