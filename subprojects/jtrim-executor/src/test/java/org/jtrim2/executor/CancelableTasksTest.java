package org.jtrim2.executor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.AsyncFunction;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.TestObj;
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

        CancelableTask task = CancelableTasks.runOnceCancelableTask(subTask);
        assertNotNull(task.toString());

        CancellationToken token = Cancellation.createCancellationSource().getToken();

        task.execute(token);
        verify(subTask).execute(same(token));
        assertNotNull(task.toString());

        task.execute(Cancellation.createCancellationSource().getToken());
        verify(subTask).execute(same(token));
        assertNotNull(task.toString());

        task.execute(Cancellation.createCancellationSource().getToken());
        verify(subTask).execute(same(token));
        assertNotNull(task.toString());
    }

    @Test
    public void testRunOnceCancelableTaskFailOnReRun() throws Exception {
        CancelableTask subTask = mock(CancelableTask.class);
        CancelableTask task = CancelableTasks.runOnceCancelableTaskStrict(subTask);

        CancellationToken token = Cancellation.createCancellationSource().getToken();

        task.execute(token);
        try {
            task.execute(Cancellation.UNCANCELABLE_TOKEN);
        } catch (IllegalStateException ex) {
            verify(subTask).execute(same(token));
            return;
        }
        throw new AssertionError("");
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

    @Test
    public void testToAsync() {
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(false);
        ContextAwareTaskExecutor contextAwareExecutor = TaskExecutors.contextAware(manualExecutor);

        TestObj result = new TestObj("testToAsync");
        BoolConsumer consumer = mock(BoolConsumer.class);
        AsyncFunction<TestObj> asyncFunction = CancelableTasks.toAsync(contextAwareExecutor, cancelToken -> {
            consumer.accept(contextAwareExecutor.isExecutingInThis());
            return result;
        });

        asyncFunction.executeAsync(Cancellation.UNCANCELABLE_TOKEN);

        verifyZeroInteractions(consumer);
        manualExecutor.tryExecuteOne();
        verify(consumer).accept(true);
    }

    @Test
    public void testToAsyncCancellation() {
        ManualTaskExecutor manualExecutor = new ManualTaskExecutor(false);
        ContextAwareTaskExecutor contextAwareExecutor = TaskExecutors.contextAware(manualExecutor);

        CancellationSource cancel = Cancellation.createCancellationSource();

        TestObj result = new TestObj("testToAsync");
        BoolConsumer preCancel = mock(BoolConsumer.class);
        BoolConsumer postCancel = mock(BoolConsumer.class);
        AsyncFunction<TestObj> asyncFunction = CancelableTasks.toAsync(contextAwareExecutor, cancelToken -> {
            preCancel.accept(cancelToken.isCanceled());
            cancel.getController().cancel();
            postCancel.accept(cancelToken.isCanceled());
            return result;
        });

        asyncFunction.executeAsync(cancel.getToken());

        verifyZeroInteractions(preCancel, postCancel);
        manualExecutor.tryExecuteOne();
        verify(preCancel).accept(false);
        verify(postCancel).accept(true);
    }

    private interface BoolConsumer {
        public void accept(boolean arg);
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
