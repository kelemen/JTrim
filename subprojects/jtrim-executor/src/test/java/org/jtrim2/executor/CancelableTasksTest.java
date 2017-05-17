package org.jtrim2.executor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.TestUtils;
import org.jtrim2.testutils.executor.MockFunction;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class CancelableTasksTest {
    @Before
    public void setUp() {
        // clear interrupted status
        Thread.interrupted();
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(CancelableTasks.class);
    }

    @Test
    public void testRunOnceCancelableTask() throws Exception {
        CancelableTask subTask = mock(CancelableTask.class);
        stub(subTask.toString()).toReturn("TEST");

        CancelableTask task = CancelableTasks.runOnceCancelableTask(subTask, false);
        assertNotNull(task.toString());

        task.execute(Cancellation.CANCELED_TOKEN);
        verify(subTask).execute(any(CancellationToken.class));
        assertNotNull(task.toString());

        task.execute(Cancellation.UNCANCELABLE_TOKEN);
        verify(subTask).execute(any(CancellationToken.class));
        assertNotNull(task.toString());

        task.execute(Cancellation.UNCANCELABLE_TOKEN);
        verify(subTask).execute(any(CancellationToken.class));
        assertNotNull(task.toString());
    }

    @Test(expected = IllegalStateException.class)
    public void testRunOnceCancelableTaskFailOnReRun() throws Exception {
        CancelableTask subTask = mock(CancelableTask.class);
        CancelableTask task = CancelableTasks.runOnceCancelableTask(subTask, true);

        try {
            try {
                task.execute(Cancellation.UNCANCELABLE_TOKEN);
            } catch (IllegalStateException ex) {
                throw new RuntimeException(ex);
            }
            task.execute(Cancellation.UNCANCELABLE_TOKEN);
        } finally {
            verify(subTask).execute(any(CancellationToken.class));
        }
    }

    @Test
    public void testComplete() throws Exception {
        Object testResult = "Test-Result-43253";
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        MockFunction<Object> function = MockFunction.mock(testResult);
        CompletableFuture<Object> future = new CompletableFuture<>();

        CancelableTasks.complete(cancelToken, MockFunction.toFunction(function), future);

        verify(function).execute(false);
        assertSame(testResult, future.getNow(null));
    }

    private static void verifyResult(
            String errorMessage,
            CompletableFuture<?> future,
            Predicate<? super Throwable> exceptionTest) {

        try {
            future.getNow(null);
        } catch (CompletionException ex) {
            if (exceptionTest.test(ex.getCause())) {
                return;
            }
            throw ex;
        }

        fail(errorMessage);
    }

    @Test
    public void testCompletePreCanceled() throws Exception {
        Object testResult = "Test-Result-53443643";
        CancellationToken cancelToken = Cancellation.CANCELED_TOKEN;
        MockFunction<Object> function = MockFunction.mock(testResult);
        CompletableFuture<Object> future = new CompletableFuture<>();

        CancelableTasks.complete(cancelToken, MockFunction.toFunction(function), future);

        verifyZeroInteractions(function);
        verifyResult("Expected completion with OperationCanceledException",
                future,
                ex -> ex instanceof OperationCanceledException);
    }

    private void testCompleteExceptionInTask(Throwable exception) throws Exception {
        Object testResult = "Test-Result-43253";
        CancellationToken cancelToken = Cancellation.UNCANCELABLE_TOKEN;
        MockFunction<Object> function = MockFunction.mock(testResult);
        CompletableFuture<Object> future = new CompletableFuture<>();

        doThrow(exception)
                .when(function)
                .execute(anyBoolean());

        CancelableTasks.complete(cancelToken, MockFunction.toFunction(function), future);

        verify(function).execute(false);
        verifyResult("Expected completion with " + exception,
                future,
                ex -> ex == exception);
    }

    @Test
    public void testCompleteTaskIsCanceled() throws Exception {
        testCompleteExceptionInTask(new OperationCanceledException());
    }

    @Test
    public void testCompleteExceptionInTask() throws Exception {
        testCompleteExceptionInTask(new Exception());
    }

    @Test
    public void testExecuteAndLogErrorNormal() throws Exception {
        Runnable task = mock(Runnable.class);
        CancelableTasks.executeAndLogError(task);
        verify(task).run();
    }

    @Test
    public void testExecuteAndLogErrorCanceled() throws Exception {
        OperationCanceledException expected = new OperationCanceledException();
        Runnable called = mock(Runnable.class);
        Runnable task = () -> {
            called.run();
            throw expected;
        };

        CancelableTasks.executeAndLogError(task);
        verify(called).run();
    }

    @Test
    public void testExecuteAndLogErrorFails() throws Exception {
        TestException expected = new TestException();
        Runnable task = () -> {
            throw expected;
        };

        try (LogCollector logs = LogTests.startCollecting()) {
            CancelableTasks.executeAndLogError(task);
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }
    }

    @Test
    public void testExecuteAndLogErrorNullTask() throws Exception {
        try (LogCollector logs = LogTests.startCollecting()) {
            CancelableTasks.executeAndLogError(null);
            LogTests.verifyLogCount(NullPointerException.class, Level.SEVERE, 1, logs);
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
