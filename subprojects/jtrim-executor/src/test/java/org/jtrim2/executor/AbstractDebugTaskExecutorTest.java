package org.jtrim2.executor;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public abstract class AbstractDebugTaskExecutorTest {
    private final Supplier<TaskExecutor> factory;

    public AbstractDebugTaskExecutorTest(Supplier<TaskExecutor> factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    private static LogCollector startCollecting() {
        return LogCollector.startCollecting(DebugTaskExecutor.class.getPackage().getName());
    }

    private static void testExpectLog(TestMethod task) throws Exception {
        try (LogCollector logs = startCollecting()) {
            task.run();
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }
    }

    private static void testNotExpectLog(TestMethod task) throws Exception {
        try (LogCollector logs = startCollecting()) {
            task.run();
            assertEquals(0, logs.getNumberOfLogs());
        }
    }

    private static Runnable mockRunnable(Throwable exception) throws Exception {
        Runnable task = mock(Runnable.class);
        doThrow(exception).when(task).run();
        return task;
    }

    private static CancelableTask mockTask(Throwable exception) throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        doThrow(exception).when(task).execute(any(CancellationToken.class));
        return task;
    }

    private static CancelableFunction<?> mockFunction(Throwable exception) throws Exception {
        CancelableFunction<?> task = mock(CancelableFunction.class);
        when(task.execute(any(CancellationToken.class))).thenThrow(exception);
        return task;
    }

    @Test
    public void testExecuteTaskWithException1() throws Exception {
        TaskExecutor executor = factory.get();

        testExpectLog(() -> {
            Runnable task = mockRunnable(new TestException());
            executor.execute(task);
            verify(task).run();
        });
    }

    @Test
    public void testExecuteTaskWithException2() throws Exception {
        TaskExecutor executor = factory.get();

        testExpectLog(() -> {
            CancelableTask task = mockTask(new TestException());
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testExecuteTaskWithException3() throws Exception {
        TaskExecutor executor = factory.get();

        testExpectLog(() -> {
            CancelableFunction<?> function = mockFunction(new TestException());
            executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, function);
            verify(function).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testExecuteTaskWithException4() throws Exception {
        TaskExecutor executor = factory.get();

        testExpectLog(() -> {
            Runnable task = mockRunnable(new TestException());
            executor.executeStaged(task);
            verify(task).run();
        });
    }

    @Test
    public void testExecuteTaskWithCanceledException1() throws Exception {
        TaskExecutor executor = factory.get();

        testNotExpectLog(() -> {
            Runnable task = mockRunnable(new OperationCanceledException());
            executor.execute(task);
            verify(task).run();
        });
    }

    @Test
    public void testExecuteTaskWithCanceledException2() throws Exception {
        TaskExecutor executor = factory.get();

        testNotExpectLog(() -> {
            CancelableTask task = mockTask(new OperationCanceledException());
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testExecuteTaskWithCanceledException3() throws Exception {
        TaskExecutor executor = factory.get();

        testNotExpectLog(() -> {
            CancelableFunction<?> function = mockFunction(new OperationCanceledException());
            executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, function);
            verify(function).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testExecuteTaskWithCanceledException4() throws Exception {
        TaskExecutor executor = factory.get();

        testNotExpectLog(() -> {
            Runnable task = mockRunnable(new OperationCanceledException());
            executor.executeStaged(task);
            verify(task).run();
        });
    }

    @Test
    public void testExecuteTaskWithoutException1() throws Exception {
        TaskExecutor executor = factory.get();

        testNotExpectLog(() -> {
            Runnable task = mock(Runnable.class);
            executor.execute(task);
            verify(task).run();
        });
    }

    @Test
    public void testExecuteTaskWithoutException2() throws Exception {
        TaskExecutor executor = factory.get();

        testNotExpectLog(() -> {
            CancelableTask task = mock(CancelableTask.class);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
            verify(task).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testExecuteTaskWithoutException3() throws Exception {
        TaskExecutor executor = factory.get();

        testNotExpectLog(() -> {
            CancelableFunction<?> function = mock(CancelableFunction.class);
            executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, function);
            verify(function).execute(any(CancellationToken.class));
        });
    }

    @Test
    public void testExecuteTaskWithoutException4() throws Exception {
        TaskExecutor executor = factory.get();

        testNotExpectLog(() -> {
            Runnable task = mock(Runnable.class);
            executor.executeStaged(task);
            verify(task).run();
        });
    }

    private static interface TestMethod {
        public void run() throws Exception;
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
