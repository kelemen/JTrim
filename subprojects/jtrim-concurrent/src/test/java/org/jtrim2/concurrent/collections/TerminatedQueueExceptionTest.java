package org.jtrim2.concurrent.collections;

import org.junit.Test;

import static org.junit.Assert.*;

public class TerminatedQueueExceptionTest {
    @Test
    public void testConstructor1() {
        TerminatedQueueException exception = new TerminatedQueueException();
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructor2() {
        String message = "TEST MESSAGE";
        TerminatedQueueException exception = new TerminatedQueueException(message);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructor3() {
        String message = "TEST MESSAGE";
        Throwable cause = new RuntimeException();
        TerminatedQueueException exception = new TerminatedQueueException(message, cause);
        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testConstructor4() {
        String message = "TEST MESSAGE";
        Throwable cause = new RuntimeException(message);
        TerminatedQueueException exception = new TerminatedQueueException(cause);
        assertEquals(cause.toString(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    private static void verifyCannotSetStackTrace(Throwable ex) {
        StackTraceElement[] newTrace = new StackTraceElement[1];
        newTrace[0] = new StackTraceElement("MyClass", "MyMethod", "MyFile.java", 125);
        ex.setStackTrace(newTrace);
        assertArrayEquals(new StackTraceElement[0], ex.getStackTrace());
    }

    private static void verifyWithoutStackTrace(Throwable ex) {
        assertArrayEquals(new StackTraceElement[0], ex.getStackTrace());
        verifyCannotSetStackTrace(ex);
    }

    @Test
    public void testWithoutStackTraceNoArg() {
        TerminatedQueueException ex = TerminatedQueueException.withoutStackTrace();
        assertNotNull(ex.getMessage());
        assertNull(ex.getCause());
        verifyWithoutStackTrace(ex);
    }

    @Test
    public void testWithoutStackTrace() {
        String message = "My-Test-Message-1";
        RuntimeException cause = new RuntimeException("My-Test-Cause");
        TerminatedQueueException ex = TerminatedQueueException.withoutStackTrace(message, cause);
        assertEquals(message, ex.getMessage());
        assertSame(cause, ex.getCause());
        verifyWithoutStackTrace(ex);
    }

    @Test
    public void testAllNullWithoutStackTrace() {
        TerminatedQueueException ex = TerminatedQueueException.withoutStackTrace(null, null);
        assertNull(ex.getMessage());
        assertNull(ex.getCause());
        verifyWithoutStackTrace(ex);
    }
}
