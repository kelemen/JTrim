package org.jtrim2.taskgraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

    private static class TestTaskInputRef<I> implements TaskInputRef<I> {
        private final TaskNodeKey<I, ?> defKey;

        public TestTaskInputRef(TaskNodeKey<I, ?> defKey) {
            this.defKey = defKey;
        }

        public static void verifyEquals(TaskNodeKey<?, ?> expected, TaskInputRef<?> inputRef) {
            assertNotNull(inputRef);
            assertEquals(expected, ((TestTaskInputRef<?>) inputRef).defKey);
        }

        @Override
        public I consumeInput() {
            throw new UnsupportedOperationException("Not supported in test.");
        }
    }

    private static class TestTaskInputBinder implements TaskInputBinder {
        private final List<TaskNodeKey<?, ?>> received;

        public TestTaskInputBinder() {
            this.received = new ArrayList<>();
        }

        public <I, A> TaskInputRef<I> bindInput(TaskNodeKey<I, A> defKey) {
            received.add(defKey);
            return new TestTaskInputRef<>(defKey);
        }

        public void verifyCalled(TaskNodeKey<?, ?>... expected) {
            assertEquals(Arrays.asList(expected), received);
        }
    }
}
