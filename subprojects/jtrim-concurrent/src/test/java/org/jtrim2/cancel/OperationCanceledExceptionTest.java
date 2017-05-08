package org.jtrim2.cancel;

import org.junit.Test;

import static org.junit.Assert.*;

public class OperationCanceledExceptionTest {
    @Test
    public void testConstructor1() {
        OperationCanceledException exception = new OperationCanceledException();
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructor2() {
        String message = "TEST MESSAGE";
        OperationCanceledException exception = new OperationCanceledException(message);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructor3() {
        String message = "TEST MESSAGE";
        Throwable cause = new RuntimeException();
        OperationCanceledException exception = new OperationCanceledException(message, cause);
        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testConstructor4() {
        String message = "TEST MESSAGE";
        Throwable cause = new RuntimeException(message);
        OperationCanceledException exception = new OperationCanceledException(cause);
        assertEquals(cause.toString(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
