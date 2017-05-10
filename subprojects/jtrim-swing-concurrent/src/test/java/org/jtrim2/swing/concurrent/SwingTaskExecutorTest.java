package org.jtrim2.swing.concurrent;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.Cancellation;
import org.junit.Test;

import static org.junit.Assert.*;

public class SwingTaskExecutorTest {
    @Test(timeout = 5000)
    public void testIllegalEDT1() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                SwingTaskExecutor executor = new SwingTaskExecutor(true);
                executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
                fail("Expected: IllegalStateException");
            } catch (IllegalStateException ex) {
            }
        });
    }

    @Test(timeout = 5000)
    public void testIllegalEDT2() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                SwingTaskExecutor executor = new SwingTaskExecutor(false);
                executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
                fail("Expected: IllegalStateException");
            } catch (IllegalStateException ex) {
            }
        });
    }

    @Test(timeout = 5000)
    public void testIllegalEDT3() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                SwingTaskExecutor executor = new SwingTaskExecutor(true);
                executor.tryAwaitTermination(
                        Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
                fail("Expected: IllegalStateException");
            } catch (IllegalStateException ex) {
            }
        });
    }

    @Test(timeout = 5000)
    public void testIllegalEDT4() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                SwingTaskExecutor executor = new SwingTaskExecutor(false);
                executor.tryAwaitTermination(
                        Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS);
                fail("Expected: IllegalStateException");
            } catch (IllegalStateException ex) {
            }
        });
    }

    @Test(timeout = 5000)
    public void testAwaitTermination() {
        for (boolean lazy: Arrays.asList(false, true)) {
            SwingTaskExecutor executor = new SwingTaskExecutor(lazy);
            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    @Test(timeout = 5000)
    public void testTryAwaitTermination1() {
        for (boolean lazy: Arrays.asList(false, true)) {
            SwingTaskExecutor executor = new SwingTaskExecutor(lazy);
            executor.shutdown();
            assertTrue(executor.tryAwaitTermination(
                    Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS));
        }
    }

    @Test(timeout = 5000)
    public void testTryAwaitTermination2() {
        for (boolean lazy: Arrays.asList(false, true)) {
            SwingTaskExecutor executor = new SwingTaskExecutor(lazy);
            assertFalse(executor.tryAwaitTermination(
                    Cancellation.UNCANCELABLE_TOKEN, 1, TimeUnit.NANOSECONDS));
        }
    }
}
