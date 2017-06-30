package org.jtrim2.taskgraph;

import java.util.Objects;
import java.util.function.Supplier;
import org.junit.Assert;

public final class TestTaskInputRef<I> implements TaskInputRef<I> {
    private final TaskNodeKey<I, ?> defKey;
    private final Supplier<? extends I> inputRef;

    public TestTaskInputRef(TaskNodeKey<I, ?> defKey, Supplier<? extends I> inputRef) {
        this.defKey = Objects.requireNonNull(defKey, "defKey");
        this.inputRef = Objects.requireNonNull(inputRef, "inputRef");
    }

    public static void verifyEquals(TaskNodeKey<?, ?> expected, TaskInputRef<?> inputRef) {
        Assert.assertNotNull(inputRef);

        Object inputRefKey = ((TestTaskInputRef<?>) inputRef).defKey;
        Assert.assertEquals(expected, inputRefKey);
    }

    @Override
    public I consumeInput() {
        return inputRef.get();
    }
}
