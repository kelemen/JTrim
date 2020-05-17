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
}
