package org.jtrim2.concurrent;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class AsyncTasksTest {
    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(AsyncTasks.class);
    }

    private static LogCollector startCollecting() {
        return LogCollector.startCollecting(AsyncTasks.class.getName());
    }

    @Test
    public void testExpectNoErrorNull() {
        try (LogCollector logs = startCollecting()) {
            AsyncTasks.expectNoError(null);
            assertEquals(0, logs.getNumberOfLogs());
        }
    }

    @Test
    public void testExpectNoErrorOperationCanceledException() {
        try (LogCollector logs = startCollecting()) {
            AsyncTasks.expectNoError(new OperationCanceledException());
            assertEquals(0, logs.getNumberOfLogs());
        }
    }

    @Test
    public void testExpectNoErrorException() {
        try (LogCollector logs = startCollecting()) {
            AsyncTasks.expectNoError(new TestException());
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }
    }

    private static void expectError(CompletableFuture<?> future, Throwable expected) {
        try {
            future.getNow(null);
        } catch (CompletionException ex) {
            assertSame(expected, ex.getCause());
            return;
        }

        fail("Expected error: " + expected);
    }

    @Test
    public void testCompleteNormally() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Object result = "test-result-5475423";
        AsyncTasks.complete(result, null, future);
        assertSame(result, future.getNow(null));
    }

    @Test
    public void testCompleteExceptionally() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Exception error = new Exception("Test-Exception-435345");
        AsyncTasks.complete(null, error, future);
        expectError(future, error);
    }

    @Test
    public void testCompleteExceptionallyWithBogusResult() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Exception error = new Exception("Test-Exception-657563");
        AsyncTasks.complete("bogus-result-34534", error, future);
        expectError(future, error);
    }

    @Test
    public void testCompleteForwarderNormally() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Object result = "test-result-5475423";
        AsyncTasks.completeForwarder(future).accept(result, null);
        assertSame(result, future.getNow(null));
    }

    @Test
    public void testCompleteForwarderExceptionally() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Exception error = new Exception("Test-Exception-435345");
        AsyncTasks.completeForwarder(future).accept(null, error);
        expectError(future, error);
    }

    @Test
    public void testCompleteForwarderExceptionallyWithBogusResult() {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Exception error = new Exception("Test-Exception-657563");
        AsyncTasks.completeForwarder(future).accept("bogus-result-34534", error);
        expectError(future, error);
    }

    @Test
    public void testIsCanceledNull() {
        assertFalse(AsyncTasks.isCanceled(null));
    }

    @Test
    public void testIsCanceledError1() {
        assertFalse(AsyncTasks.isCanceled(new Throwable()));
    }

    @Test
    public void testIsCanceledError2() {
        assertFalse(AsyncTasks.isCanceled(new IOException()));
    }

    @Test
    public void testIsCanceledError3() {
        assertFalse(AsyncTasks.isCanceled(new TestCompletionException()));
    }

    @Test
    public void testIsCanceledError4() {
        assertFalse(AsyncTasks.isCanceled(new TestCompletionException(new IOException())));
    }

    @Test
    public void testIsCanceledTrue() {
        assertTrue(AsyncTasks.isCanceled(new OperationCanceledException()));
    }

    @Test
    public void testIsCanceledTrueWrapped() {
        assertTrue(AsyncTasks.isCanceled(new CompletionException(new OperationCanceledException())));
    }

    @Test
    public void testIsErrorNull() {
        assertFalse(AsyncTasks.isError(null));
    }

    @Test
    public void testIsErrorError1() {
        assertTrue(AsyncTasks.isError(new Throwable()));
    }

    @Test
    public void testIsErrorError2() {
        assertTrue(AsyncTasks.isError(new IOException()));
    }

    @Test
    public void testIsErrorCanceled() {
        assertFalse(AsyncTasks.isError(new OperationCanceledException()));
    }

    @SuppressWarnings("unchecked")
    private static Consumer<Throwable> mockErrorHandler() {
        return mock(Consumer.class);
    }

    @Test
    public void testHandleErrorResultBothNulls() {
        Consumer<Throwable> errorHandler = mockErrorHandler();
        AsyncTasks.handleErrorResult(null, null, errorHandler);
        verifyZeroInteractions(errorHandler);
    }

    @Test
    public void testHandleErrorResultNullError() {
        Exception result = new Exception("test-result-1234");
        Consumer<Throwable> errorHandler = mockErrorHandler();
        AsyncTasks.handleErrorResult(result, null, errorHandler);
        verify(errorHandler).accept(same(result));
    }

    @Test
    public void testHandleErrorResultNullResult() {
        Exception error = new Exception("test-error-1234");
        Consumer<Throwable> errorHandler = mockErrorHandler();
        AsyncTasks.handleErrorResult(null, error, errorHandler);
        verify(errorHandler).accept(same(error));
    }

    @Test
    public void testHandleErrorResultNoNulls() {
        Exception result = new Exception("test-result-1234");
        Exception error = new Exception("test-result-1234");
        Consumer<Throwable> errorHandler = mockErrorHandler();
        AsyncTasks.handleErrorResult(result, error, errorHandler);
        verify(errorHandler).accept(same(result));

        assertEquals(Collections.singletonList(error), Arrays.asList(result.getSuppressed()));
    }

    @Test
    public void testErrorResultHandlerBothNulls() {
        Consumer<Throwable> errorHandler = mockErrorHandler();
        AsyncTasks.errorResultHandler(errorHandler).accept(null, null);
        verifyZeroInteractions(errorHandler);
    }

    @Test
    public void testErrorResultHandlerNullError() {
        Exception result = new Exception("test-result-1234");
        Consumer<Throwable> errorHandler = mockErrorHandler();
        AsyncTasks.errorResultHandler(errorHandler).accept(result, null);
        verify(errorHandler).accept(same(result));
    }

    @Test
    public void testErrorResultHandlerNullResult() {
        Exception error = new Exception("test-error-1234");
        Consumer<Throwable> errorHandler = mockErrorHandler();
        AsyncTasks.errorResultHandler(errorHandler).accept(null, error);
        verify(errorHandler).accept(same(error));
    }

    @Test
    public void testErrorResultHandlerNoNulls() {
        Exception result = new Exception("test-result-1234");
        Exception error = new Exception("test-result-1234");
        Consumer<Throwable> errorHandler = mockErrorHandler();
        AsyncTasks.errorResultHandler(errorHandler).accept(result, error);
        verify(errorHandler).accept(same(result));

        assertEquals(Collections.singletonList(error), Arrays.asList(result.getSuppressed()));
    }

    @Test
    public void testUnwrapNull() {
        assertNull(AsyncTasks.unwrap(null));
    }

    @Test
    public void testUnwrapUnknownWithoutCause() {
        Exception ex = new TestException();
        assertSame(ex, AsyncTasks.unwrap(ex));
    }

    @Test
    public void testUnwrapUnknownWithCause() {
        Exception ex = new TestException(new IOException());
        assertSame(ex, AsyncTasks.unwrap(ex));
    }

    @Test
    public void testUnwrapCompletionWithoutCause() {
        Exception ex = new TestCompletionException();
        assertSame(ex, AsyncTasks.unwrap(ex));
    }

    @Test
    public void testUnwrapCompletionWithCause() {
        TestException wrapped = new TestException();
        Exception ex = new TestCompletionException(wrapped);
        assertSame(wrapped, AsyncTasks.unwrap(ex));
    }

    @Test
    public void testUnwrapWithCompletableFuture() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        TestException wrapped = new TestException();
        future.completeExceptionally(wrapped);

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        future.thenAccept(Tasks.noOpConsumer())
                .whenComplete((arg, error) -> { })
                .whenComplete((arg, error) -> {
                    errorRef.set(error);
                });
        assertSame(wrapped, AsyncTasks.unwrap(errorRef.get()));
    }

    private static class TestException extends Exception {
        private static final long serialVersionUID = 1L;

        public TestException() {
        }

        public TestException(Throwable cause) {
            super(cause);
        }
    }

    private static class TestCompletionException extends CompletionException {
        private static final long serialVersionUID = 1L;

        public TestCompletionException() {
        }

        public TestCompletionException(Throwable cause) {
            super(cause);
        }
    }
}
