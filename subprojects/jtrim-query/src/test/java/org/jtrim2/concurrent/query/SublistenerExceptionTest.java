package org.jtrim2.concurrent.query;

import org.junit.Test;

import static org.junit.Assert.*;

public class SublistenerExceptionTest {
    @Test
    public void testConstructor1() {
        SublistenerException exception = new SublistenerException();
        assertNull(exception.getCause());
        assertNull(exception.getMessage());
    }

    @Test
    public void testConstructor2() {
        Throwable cause = new Exception("SublistenerExceptionTest.testConstructor2");
        SublistenerException exception = new SublistenerException(cause);
        assertSame(cause, exception.getCause());
        assertEquals(cause.toString(), exception.getMessage());
    }

    @Test
    public void testConstructor3() {
        String message = "SublistenerExceptionTest.testConstructor3";
        SublistenerException exception = new SublistenerException(message);
        assertNull(exception.getCause());
        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testConstructor4() {
        Throwable cause = new Exception("SublistenerExceptionTest.testConstructor4_cause");
        String message = "SublistenerExceptionTest.testConstructor4_message";
        SublistenerException exception = new SublistenerException(message, cause);
        assertSame(cause, exception.getCause());
        assertEquals(message, exception.getMessage());
    }
}
