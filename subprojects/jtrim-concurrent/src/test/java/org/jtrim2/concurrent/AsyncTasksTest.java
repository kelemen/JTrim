package org.jtrim2.concurrent;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;

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
    public void testIsCanceledTrue() {
        assertTrue(AsyncTasks.isCanceled(new OperationCanceledException()));
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

    private static class TestException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
