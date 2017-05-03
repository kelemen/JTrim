package org.jtrim2.image.transform;

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
public class ImageProcessingExceptionTest {
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
        ImageProcessingException exception = new ImageProcessingException();
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructor2() {
        String message = "TEST MESSAGE";
        ImageProcessingException exception = new ImageProcessingException(message);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    public void testConstructor3() {
        String message = "TEST MESSAGE";
        Throwable cause = new RuntimeException();
        ImageProcessingException exception = new ImageProcessingException(message, cause);
        assertEquals(message, exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    public void testConstructor4() {
        String message = "TEST MESSAGE";
        Throwable cause = new RuntimeException(message);
        ImageProcessingException exception = new ImageProcessingException(cause);
        assertEquals(cause.toString(), exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
