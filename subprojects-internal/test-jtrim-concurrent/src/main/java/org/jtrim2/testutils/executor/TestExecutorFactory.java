package org.jtrim2.testutils.executor;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.testutils.UnsafeConsumer;
import org.jtrim2.testutils.UnsafeFunction;
import org.jtrim2.testutils.UnsafeRunnable;

public final class TestExecutorFactory<E extends TaskExecutor> {
    private final Supplier<? extends ExecutorRef<? extends E>> executorRefFactory;

    public TestExecutorFactory(
            Supplier<? extends E> executorFactory,
            UnsafeConsumer<? super E> shutdown) {
        this(() -> {
            E executor = executorFactory.get();
            return new ExecutorRef<>(executor, () -> {
                shutdown.accept(executor);
            });
        });

        Objects.requireNonNull(executorFactory, "executorFactory");
        Objects.requireNonNull(shutdown, "shutdown");
    }

    public TestExecutorFactory(Supplier<? extends ExecutorRef<? extends E>> executorRefFactory) {
        this.executorRefFactory = Objects.requireNonNull(executorRefFactory, "executorRefFactory");
    }

    public static <E extends TaskExecutorService> TestExecutorFactory<E> executorService(
            Supplier<? extends E> wrappedFactory) {

        return wrappedExecutor(wrappedFactory, Function.identity());
    }

    public static <W extends TaskExecutorService, E extends TaskExecutor> TestExecutorFactory<E> wrappedExecutor(
            Supplier<? extends W> wrappedFactory,
            Function<? super W, ? extends E> executorFactory) {
        Objects.requireNonNull(wrappedFactory, "wrappedFactory");
        Objects.requireNonNull(executorFactory, "executorFactory");

        return new TestExecutorFactory<>(() -> {
            W wrapped = wrappedFactory.get();
            E executor = executorFactory.apply(wrapped);
            return new ExecutorRef<>(executor, () -> {
                GenericExecutorServiceTests.shutdownTestExecutor(wrapped);
            });
        });
    }

    public <R> R runTest(UnsafeFunction<? super E, R> testMethod) throws Exception {
        ExecutorRef<? extends E> executorRef = executorRefFactory.get();
        return executorRef.runTest(testMethod);
    }

    public static final class ExecutorRef<E extends TaskExecutor> {
        private final E executor;
        private final UnsafeRunnable shutdown;

        public ExecutorRef(E executor, UnsafeRunnable shutdown) {
            this.executor = Objects.requireNonNull(executor, "executor");
            this.shutdown = Objects.requireNonNull(shutdown, "shutdown");
        }

        private <R> R runTest(UnsafeFunction<? super E, ? extends R> testMethod) throws Exception {
            try {
                return testMethod.apply(executor);
            } finally {
                shutdown.run();
            }
        }
    }
}
