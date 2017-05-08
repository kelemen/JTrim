package org.jtrim2.executor;

import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class DoneFutureTest {

    public DoneFutureTest() {
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

    /**
     * Test of cancel method, of class DoneFuture.
     */
    @Test
    public void testCancel() {
        DoneFuture<Object> future = new DoneFuture<>(null);
        assertFalse(future.cancel(false));
        assertFalse(future.cancel(true));
    }

    /**
     * Test of isCancelled method, of class DoneFuture.
     */
    @Test
    public void testIsCancelled() {
        DoneFuture<Object> future = new DoneFuture<>(null);
        assertFalse(future.isCancelled());
    }

    /**
     * Test of isDone method, of class DoneFuture.
     */
    @Test
    public void testIsDone() {
        DoneFuture<Object> future = new DoneFuture<>(null);
        assertTrue(future.isDone());
    }

    /**
     * Test of get method, of class DoneFuture.
     */
    @Test
    public void testGet_0args() {
        Object result = new Object();
        DoneFuture<Object> future = new DoneFuture<>(result);
        assertSame(result, future.get());

        assertNull(new DoneFuture<>(null).get());
    }

    /**
     * Test of get method, of class DoneFuture.
     */
    @Test
    public void testGet_long_TimeUnit() {
        Object result = new Object();
        DoneFuture<Object> future = new DoneFuture<>(result);
        assertSame(result, future.get(Long.MAX_VALUE, TimeUnit.DAYS));

        assertNull(new DoneFuture<>(null).get(Long.MAX_VALUE, TimeUnit.DAYS));
    }
}
