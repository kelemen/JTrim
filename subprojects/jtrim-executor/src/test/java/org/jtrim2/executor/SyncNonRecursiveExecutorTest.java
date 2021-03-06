package org.jtrim2.executor;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executor;
import org.jtrim2.testutils.executor.GenericExecutorTests;
import org.jtrim2.testutils.executor.TestExecutorFactory;
import org.junit.Test;

import static org.junit.Assert.*;

public class SyncNonRecursiveExecutorTest {
    public static class GenericTest extends GenericExecutorTests<TaskExecutor> {
        public GenericTest() {
            super(testFactories());
        }
    }

    private static TaskExecutor wrappedDirectFactory() {
        return ExecutorConverter.asTaskExecutor(new SyncNonRecursiveExecutor());
    }

    private static TaskExecutor wrappedIndirectFactory() {
        return ExecutorConverter.asTaskExecutor(ExecutorsEx.syncNonRecursiveExecutor());
    }

    private static Collection<TestExecutorFactory<TaskExecutor>> testFactories() {
        return Arrays.asList(
                new TestExecutorFactory<>(SyncNonRecursiveExecutorTest::wrappedDirectFactory, executor -> { }),
                new TestExecutorFactory<>(SyncNonRecursiveExecutorTest::wrappedIndirectFactory, executor -> { })
        );
    }

    @Test
    public void testExecuteWithNestedFailure() {
        Executor executor = new SyncNonRecursiveExecutor();

        StringBuilder result = new StringBuilder();
        try {
            executor.execute(() -> {
                result.append("a");
                executor.execute(() -> {
                    throw new TestException();
                });
                executor.execute(() -> result.append("d"));
                result.append("b");
            });
        } catch (TestException ex) {
            assertEquals("ab", result.toString());
            return;
        }

        throw new AssertionError("Expected TestException.");
    }

    @Test
    public void testExecuteInProperOrder() {
        Executor executor = new SyncNonRecursiveExecutor();

        StringBuilder result = new StringBuilder();
        executor.execute(() -> {
            result.append("a");
            executor.execute(() -> result.append("c"));
            result.append("b");
        });

        assertEquals("abc", result.toString());
    }

    @Test
    public void testExecuteTwice() {
        Executor executor = new SyncNonRecursiveExecutor();

        StringBuilder result = new StringBuilder();
        executor.execute(() -> {
            result.append("a");
            executor.execute(() -> result.append("c"));
            result.append("b");
        });
        executor.execute(() -> {
            result.append("d");
            executor.execute(() -> result.append("f"));
            result.append("e");
        });

        assertEquals("abcdef", result.toString());
    }

    @Test
    public void testExecuteInProperOrderDeep() {
        Executor executor = new SyncNonRecursiveExecutor();

        StringBuilder result = new StringBuilder();
        executor.execute(() -> {
            result.append("a");
            executor.execute(() -> {
                result.append("c");
                executor.execute(() -> result.append("e"));
                result.append("d");
            });
            result.append("b");
        });

        assertEquals("abcde", result.toString());
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
