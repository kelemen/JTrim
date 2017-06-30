package org.jtrim2.taskgraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.junit.Assert;

public final class TestTaskInputBinder implements TaskInputBinder {
    private final List<TaskNodeKey<?, ?>> received;
    private final Function<TaskNodeKey<?, ?>, ?> inputProvider;

    public TestTaskInputBinder() {
        this(nodeKey -> {
            throw new UnsupportedOperationException("Not supported in test.");
        });
    }

    public TestTaskInputBinder(Function<TaskNodeKey<?, ?>, ?> inputProvider) {
        this.inputProvider = Objects.requireNonNull(inputProvider, "inputProvider");
        this.received = new ArrayList<>();
    }

    @Override
    public <I, A> TaskInputRef<I> bindInput(TaskNodeKey<I, A> defKey) {
        received.add(defKey);
        return new TestTaskInputRef<>(defKey, () -> {
            Object result = inputProvider.apply(defKey);
            return defKey.getFactoryKey().getResultType().cast(result);
        });
    }

    public void verifyCalled(TaskNodeKey<?, ?>... expected) {
        Assert.assertEquals(Arrays.asList(expected), received);
    }
}
