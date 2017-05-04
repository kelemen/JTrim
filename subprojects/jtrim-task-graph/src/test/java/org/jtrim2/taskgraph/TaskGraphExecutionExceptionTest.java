package org.jtrim2.taskgraph;

import org.junit.Test;

import static org.junit.Assert.*;

public class TaskGraphExecutionExceptionTest {
    @Test
    public void testDefault() {
        TaskGraphExecutionException ex = new TaskGraphExecutionException();
        assertNull(ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void testMessageOnly() {
        String message = "My-Test-Message-1";
        TaskGraphExecutionException ex = new TaskGraphExecutionException(message);
        assertEquals(message, ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    public void testMessageAndCause() {
        String message = "My-Test-Message-1";
        RuntimeException cause = new RuntimeException("My-Test-Cause");
        TaskGraphExecutionException ex = new TaskGraphExecutionException(message, cause);
        assertEquals(message, ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testCauseOnly() {
        RuntimeException cause = new RuntimeException("My-Test-Cause");
        TaskGraphExecutionException ex = new TaskGraphExecutionException(cause);
        assertSame(cause, ex.getCause());

        assertTrue(ex.getMessage().contains(cause.getMessage()));
    }

    private static void verifyCannotSetStackTrace(Throwable ex) {
        StackTraceElement[] newTrace = new StackTraceElement[1];
        newTrace[0] = new StackTraceElement("MyClass", "MyMethod", "MyFile.java", 125);
        ex.setStackTrace(newTrace);
        assertArrayEquals(new StackTraceElement[0], ex.getStackTrace());
    }

    @Test
    public void testWithoutStackTrace() {
        String message = "My-Test-Message-1";
        RuntimeException cause = new RuntimeException("My-Test-Cause");
        TaskGraphExecutionException ex = TaskGraphExecutionException.withoutStackTrace(message, cause);
        assertEquals(message, ex.getMessage());
        assertSame(cause, ex.getCause());
        assertArrayEquals(new StackTraceElement[0], ex.getStackTrace());
        verifyCannotSetStackTrace(ex);
    }

    @Test
    public void testAllNullWithoutStackTrace() {
        TaskGraphExecutionException ex = TaskGraphExecutionException.withoutStackTrace(null, null);
        assertNull(ex.getMessage());
        assertNull(ex.getCause());
        assertArrayEquals(new StackTraceElement[0], ex.getStackTrace());
        verifyCannotSetStackTrace(ex);
    }
}
