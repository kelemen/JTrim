package org.jtrim2.testutils.executor;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.testutils.JTrimTests;
import org.jtrim2.testutils.UnsafeFunction;

public abstract class AbstractExecutorTests<E extends TaskExecutor>
extends
        JTrimTests<TestExecutorFactory<? extends E>> {

    public AbstractExecutorTests(Collection<? extends TestExecutorFactory<? extends E>> factories) {
        super(factories);
    }

    public static <E extends TaskExecutor, W extends TaskExecutorService> TestExecutorFactory<E> wrappedExecutor(
            Supplier<? extends W> wrappedFactory,
            Function<? super W, ? extends E> wrapperFactory) {
        Objects.requireNonNull(wrappedFactory, "wrapped");
        Objects.requireNonNull(wrapperFactory, "wrapperFactory");

        return new TestExecutorFactory<>(() -> {
            W wrappedExecutor = wrappedFactory.get();
            E executor = wrapperFactory.apply(wrappedExecutor);
            return new TestExecutorFactory.ExecutorRef<>(executor, () -> {
                GenericExecutorServiceTests.shutdownTestExecutor(wrappedExecutor);
            });
        });
    }

    public static <V> MockFunction<V> mockFunction(V result) {
        return MockFunction.mock(result);
    }

    public static CancelableTask toTask(MockTask mockTask) {
        return MockTask.toTask(mockTask);
    }

    public static <V> CancelableFunction<V> toFunction(MockFunction<V> mockFunction) {
        return MockFunction.toFunction(mockFunction);
    }

    public static <V> BiConsumer<V, Throwable> toCleanupTask(MockCleanup mockCleanup) {
        return MockCleanup.toCleanupTask(mockCleanup);
    }

    public static <V> V waitResult(CompletionStage<V> future) {
        return CompletionStages.get(future.toCompletableFuture(), 10, TimeUnit.SECONDS);
    }

    public static <V> V waitResultWithCallback(CompletionStage<V> future) {
        CompletableFuture<V> waitableFuture = CompletionStages.toSafeWaitable(future);
        return CompletionStages.get(waitableFuture, 10, TimeUnit.SECONDS);
    }

    protected final void testAllCreated(
            UnsafeFunction<? super E, ? extends AfterTerminate> testMethod) throws Exception {

        testAll((factory) -> {
            AfterTerminate afterTerminateVerification = factory.runTest(testMethod);
            if (afterTerminateVerification != null) {
                afterTerminateVerification.verifyAfterTerminate();
            }
        });
    }
}
