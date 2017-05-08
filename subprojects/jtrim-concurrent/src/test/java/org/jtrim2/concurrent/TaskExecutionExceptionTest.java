package org.jtrim2.concurrent;

import org.junit.Test;

import static org.junit.Assert.*;

public class TaskExecutionExceptionTest {
    @Test
    public void testConstructor1() {
        Exception cause = new Exception("MY-TEST-EXCEPTION");
        TaskExecutionException exception = new TaskExecutionException(cause);
        assertSame(cause, exception.getCause());
        assertEquals(cause.toString(), exception.getMessage());
    }

    @Test
    public void testConstructor2() {
        String message = "TEST-EXCEPTION-MESSAGE";
        Exception cause = new Exception("MY-TEST-EXCEPTION");
        TaskExecutionException exception = new TaskExecutionException(message, cause);
        assertSame(cause, exception.getCause());
        assertEquals(message, exception.getMessage());
    }

    private void rethrow(Throwable cause) {
        new TaskExecutionException(cause).rethrowCause();
    }

    private <T extends Throwable> void rethrow(Throwable cause, Class<? extends T> checkedType) throws T {
        new TaskExecutionException(cause).rethrowCause(checkedType);
    }

    @Test
    public void testThrowUncheckedException() {
        try {
            rethrow(new TestException());
            fail("Expected RuntimeException.");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof TestException);
        }
    }

    @Test(expected = TestRuntimeException.class)
    public void testThrowUncheckedRuntimException() {
        rethrow(new TestRuntimeException());
    }

    @Test(expected = TestError.class)
    public void testThrowUncheckedError() {
        rethrow(new TestError());
    }

    @Test(expected = NullPointerException.class)
    public void testThrowUncheckedNull() {
        rethrow(null);
    }

    @Test
    public void testThrowCheckedException() throws TestException2 {
        try {
            rethrow(new TestException(), TestException2.class);
            fail("Expected RuntimeException.");
        } catch (RuntimeException ex) {
            assertTrue(ex.getCause() instanceof TestException);
        }
    }

    @Test(expected = TestException.class)
    public void testThrowCheckedException2() throws TestException {
        rethrow(new TestException(), TestException.class);
    }

    @Test(expected = TestRuntimeException.class)
    public void testThrowCheckedRuntimException() throws TestException {
        rethrow(new TestRuntimeException(), TestException.class);
    }

    @Test(expected = TestError.class)
    public void testThrowCheckedError() throws TestException {
        rethrow(new TestError(), TestException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testThrowCheckedNull1() throws TestException {
        rethrow(null, TestException.class);
    }

    @Test(expected = NullPointerException.class)
    public void testThrowCheckedNull2() throws Throwable {
        rethrow(null, null);
    }

    @Test
    public void testThrowCheckedNull3() throws Throwable {
        try {
            rethrow(new RuntimeException(), null);
            fail("Expected TestException.");
        } catch (RuntimeException ex) {
            Throwable[] suppressed = ex.getSuppressed();
            assertTrue("Must have suppressed exception", suppressed.length == 1);
            assertTrue(suppressed[0] instanceof NullPointerException);
        }
    }

    private static class TestRuntimeException extends RuntimeException {
        private static final long serialVersionUID = -7461262938744477494L;
    }

    private static class TestException extends Exception {
        private static final long serialVersionUID = 4344550423252703187L;
    }

    private static class TestException2 extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static class TestError extends Error {
        private static final long serialVersionUID = -8295518361535183378L;
    }
}
