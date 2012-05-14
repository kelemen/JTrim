/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jtrim.cancel;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class CancellationTest {

    public CancellationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testUncancelableToken() {
        assertFalse(Cancellation.UNCANCELABLE_TOKEN.isCanceled());

        final AtomicInteger invoked = new AtomicInteger(0);
        Cancellation.UNCANCELABLE_TOKEN.addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        assertEquals(0, invoked.get());

        Cancellation.UNCANCELABLE_TOKEN.checkCanceled();
    }

    @Test
    public void testCanceledToken() {
        assertTrue(Cancellation.CANCELED_TOKEN.isCanceled());

        final AtomicInteger invoked = new AtomicInteger(0);
        Cancellation.CANCELED_TOKEN.addCancellationListener(new Runnable() {
            @Override
            public void run() {
                invoked.incrementAndGet();
            }
        });
        assertEquals(1, invoked.get());
    }

    @Test(expected = OperationCanceledException.class)
    public void testCanceledTokenCheckCancel() {
        Cancellation.CANCELED_TOKEN.checkCanceled();
    }
}
