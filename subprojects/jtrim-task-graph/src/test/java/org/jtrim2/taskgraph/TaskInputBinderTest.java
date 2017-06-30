package org.jtrim2.taskgraph;

import org.junit.Test;

public class TaskInputBinderTest {
    @Test
    public void testBindInput2Args() {
        TestTaskInputBinder binder = new TestTaskInputBinder();

        Object customKey = new Object();
        TaskFactoryKey<?, TestFactoryArg> factoryKey
                = new TaskFactoryKey<>(TestOutput.class, TestFactoryArg.class, customKey);

        TestFactoryArg factoryArg = new TestFactoryArg();
        TaskInputRef<?> inputRef = binder.bindInput(factoryKey, factoryArg);

        TaskNodeKey<?, ?> expected = new TaskNodeKey<>(factoryKey, factoryArg);
        binder.verifyCalled(expected);
        TestTaskInputRef.verifyEquals(expected, inputRef);
    }

    @Test
    public void testBindInput3Args() {
        TestTaskInputBinder binder = new TestTaskInputBinder();

        TestFactoryArg factoryArg = new TestFactoryArg();
        TaskInputRef<?> inputRef = binder.bindInput(TestOutput.class, TestFactoryArg.class, factoryArg);

        TaskFactoryKey<?, TestFactoryArg> factoryKey = new TaskFactoryKey<>(TestOutput.class, TestFactoryArg.class);
        TaskNodeKey<?, ?> expected = new TaskNodeKey<>(factoryKey, factoryArg);
        binder.verifyCalled(expected);
        TestTaskInputRef.verifyEquals(expected, inputRef);
    }

    private static final class TestOutput {
    }

    private static final class TestFactoryArg {
    }
}
