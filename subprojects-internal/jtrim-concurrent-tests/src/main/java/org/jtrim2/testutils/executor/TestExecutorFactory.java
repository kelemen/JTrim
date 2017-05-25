package org.jtrim2.testutils.executor;

import java.util.Objects;
import java.util.function.Supplier;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.testutils.UnsafeConsumer;
import org.jtrim2.testutils.UnsafeFunction;

public final class TestExecutorFactory<E extends TaskExecutor> {
    private final Supplier<? extends E> executorFactory;
    private final UnsafeConsumer<? super E> shutdown;

    public TestExecutorFactory(
            Supplier<? extends E> executorFactory,
            UnsafeConsumer<? super E> shutdown) {

        this.executorFactory = Objects.requireNonNull(executorFactory, "executorFactory");
        this.shutdown = Objects.requireNonNull(shutdown, "shutdown");
    }

    public <R> R runTest(UnsafeFunction<? super E, R> testMethod) throws Exception {
        E executor = executorFactory.get();
        try {
            return testMethod.apply(executor);
        } finally {
            shutdown.accept(executor);
        }
    }
}
