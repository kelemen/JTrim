package org.jtrim2.taskgraph;

import org.jtrim2.cancel.Cancellation;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
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

    @Test
    public void testWithCustomKeyNode() {
        Object oldCustomKey = "OldCustomKey-testWithCustomKeyNode";
        Object newCustomKey = "NewCustomKey-testWithCustomKeyNode";

        TestFactoryArg factoryArg = new TestFactoryArg();

        TaskNodeKey<TestOutput, TestFactoryArg> src = new TaskNodeKey<>(
                new TaskFactoryKey<>(TestOutput.class, TestFactoryArg.class, oldCustomKey),
                factoryArg);

        TaskNodeKey<TestOutput, TestFactoryArg> expected = new TaskNodeKey<>(
                new TaskFactoryKey<>(TestOutput.class, TestFactoryArg.class, newCustomKey),
                factoryArg);

        TaskNodeKey<TestOutput, TestFactoryArg> newNodeKey = TaskFactories.withCustomKey(src, newCustomKey);
        assertEquals(expected, newNodeKey);
    }

    @Test
    public void testWithCustomKeyFactory() {
        Object oldCustomKey = "OldCustomKey-testWithCustomKeyNode";
        Object newCustomKey = "NewCustomKey-testWithCustomKeyNode";

        TaskFactoryKey<TestOutput, TestFactoryArg> src
                = new TaskFactoryKey<>(TestOutput.class, TestFactoryArg.class, oldCustomKey);

        TaskFactoryKey<TestOutput, TestFactoryArg> expected
                = new TaskFactoryKey<>(TestOutput.class, TestFactoryArg.class, newCustomKey);

        TaskFactoryKey<TestOutput, TestFactoryArg> newNodeKey = TaskFactories.withCustomKey(src, newCustomKey);
        assertEquals(expected, newNodeKey);
    }

    public static TaskNodeKey<String, String> node(Object customKey, String factoryArg) {
        return new TaskNodeKey<>(new TaskFactoryKey<>(String.class, String.class, customKey), factoryArg);
    }

    private static final class TestOutput {
    }

    private static final class TestFactoryArg {
    }
}
