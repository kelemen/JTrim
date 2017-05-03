package org.jtrim2.concurrent.async;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class DataTransferExceptionTest {
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
        DataTransferException exception = new DataTransferException();
        assertNull(exception.getCause());
        assertNull(exception.getMessage());
    }

    @Test
    public void testConstructor2() {
        Throwable cause = new Exception("DataTransferExceptionTest.testConstructor2");
        DataTransferException exception = new DataTransferException(cause);
        assertSame(cause, exception.getCause());
        assertEquals(cause.toString(), exception.getMessage());
    }

    @Test
    public void testConstructor3() {
        String message = "DataTransferExceptionTest.testConstructor3";
        DataTransferException exception = new DataTransferException(message);
        assertNull(exception.getCause());
        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testConstructor4() {
        Throwable cause = new Exception("DataTransferExceptionTest.testConstructor4_cause");
        String message = "DataTransferExceptionTest.testConstructor4_message";
        DataTransferException exception = new DataTransferException(message, cause);
        assertSame(cause, exception.getCause());
        assertEquals(message, exception.getMessage());
    }
}
