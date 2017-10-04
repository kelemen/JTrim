package org.jtrim2.executor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.testutils.executor.GenericExecutorServiceTests;
import org.jtrim2.testutils.executor.TestExecutorFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExecutorServiceAsTaskExecutorServiceTest {
    public static class GenericTest extends GenericExecutorServiceTests {
        public GenericTest() {
            super(testFactories());
        }
    }

    private static Collection<TestExecutorFactory<TaskExecutorService>> testFactories() {
        return Arrays.asList(
                wrappedLegacyExecutor(() -> Executors.newSingleThreadExecutor(), executor -> {
                    return new ExecutorServiceAsTaskExecutorService(executor, true);
                }),
                wrappedLegacyExecutor(() -> Executors.newSingleThreadExecutor(), executor -> {
                    return new ExecutorServiceAsTaskExecutorService(executor, false);
                })
        );
    }

    public static <W extends ExecutorService, E extends TaskExecutor> TestExecutorFactory<E> wrappedLegacyExecutor(
            Supplier<? extends W> wrappedFactory,
            Function<? super W, ? extends E> executorFactory) {
        Objects.requireNonNull(wrappedFactory, "wrappedFactory");
        Objects.requireNonNull(executorFactory, "executorFactory");

        return new TestExecutorFactory<>(() -> {
            W wrapped = wrappedFactory.get();
            E executor = executorFactory.apply(wrapped);
            return new TestExecutorFactory.ExecutorRef<>(executor, () -> shutdownAndWait(wrapped));
        });
    }

    private static void shutdownAndWait(ExecutorService executor) throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
            throw new AssertionError("Timeout: Failed to terminate executor.");
        }
    }

    @Test
    public void testShutdownIsPropagated() throws Exception {
        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutorService executor = new ExecutorServiceAsTaskExecutorService(wrapped, false);

            assertFalse(wrapped.isShutdown());
            executor.shutdown();
            assertTrue(wrapped.isShutdown());
        } finally {
            shutdownAndWait(wrapped);
        }
    }

    @Test
    public void testShutdownAndCancelIsPropagated() throws Exception {
        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutorService executor = new ExecutorServiceAsTaskExecutorService(wrapped, false);

            assertFalse(wrapped.isShutdown());
            executor.shutdownAndCancel();
            assertTrue(wrapped.isShutdown());
        } finally {
            shutdownAndWait(wrapped);
        }
    }
}
