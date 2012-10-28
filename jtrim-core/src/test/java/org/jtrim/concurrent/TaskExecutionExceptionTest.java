package org.jtrim.concurrent;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class TaskExecutionExceptionTest {

    public TaskExecutionExceptionTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

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
}