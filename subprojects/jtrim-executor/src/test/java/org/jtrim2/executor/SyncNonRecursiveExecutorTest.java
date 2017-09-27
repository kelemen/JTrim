package org.jtrim2.executor;

import java.util.Arrays;
import java.util.Collection;
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

    private static Collection<TestExecutorFactory<TaskExecutor>> testFactories() {
        return Arrays.asList(
                new TestExecutorFactory<>(SyncNonRecursiveExecutor::new, exexcutor -> { }),
                new TestExecutorFactory<>(TaskExecutors::syncNonRecursiveExecutor, exexcutor -> { })
        );
    }

    @Test
    public void testExecuteInProperOrder() {
        TaskExecutor executor = new SyncNonRecursiveExecutor();


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
        TaskExecutor executor = new SyncNonRecursiveExecutor();

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
        TaskExecutor executor = new SyncNonRecursiveExecutor();

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
}
