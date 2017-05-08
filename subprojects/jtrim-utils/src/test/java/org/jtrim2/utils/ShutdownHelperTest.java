package org.jtrim2.utils;

import java.security.Permission;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.testutils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ShutdownHelperTest {
    @Before
    public void setUp() {
        Thread.interrupted(); // clear interrupted status
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(ShutdownHelper.class);
    }

    private static void doProtected(boolean expectShutdown, Runnable code) {
        ProtectFromExitSecurityManager securityManager = new ProtectFromExitSecurityManager();
        System.setSecurityManager(securityManager);
        try {
            code.run();

            if (expectShutdown) {
                securityManager.waitViolation();
            }
            assertEquals(expectShutdown, securityManager.getViolateCount() > 0);
        } finally {
            System.setSecurityManager(null);
        }
    }

    /**
     * Test of haltLater method, of class ShutdownHelper.
     */
    @Test(timeout = 5000)
    public void testHaltLater() {
        doProtected(true, () -> ShutdownHelper.haltLater(0, 10));
    }

    @Test(timeout = 5000, expected = TestSecurityException.class)
    public void testHaltLaterImmediately() {
        doProtected(true, () -> ShutdownHelper.haltLater(0, 0));
    }

    @Test(timeout = 5000)
    public void testExitLater_int_int() {
        doProtected(true, () -> ShutdownHelper.exitLater(0, 10));
    }

    @Test(timeout = 5000, expected = TestSecurityException.class)
    public void testExitLater_int_intImmediately() {
        doProtected(true, () -> ShutdownHelper.exitLater(0, 0));
    }

    /**
     * Test of exitLater method, of class ShutdownHelper.
     */
    @Test(timeout = 5000)
    public void testExitLater_3args() {
        final Runnable exitTask = mock(Runnable.class);

        doProtected(true, () -> ShutdownHelper.exitLater(exitTask, 0, 10));

        verify(exitTask).run();
        verifyNoMoreInteractions(exitTask);
    }

    @Test(timeout = 5000)
    public void testExitLater_3argsImmediately() {
        final Runnable exitTask = mock(Runnable.class);

        doProtected(true, () -> ShutdownHelper.exitLater(exitTask, 0, 0));

        verify(exitTask).run();
        verifyNoMoreInteractions(exitTask);
    }

    /**
     * Test of exit method, of class ShutdownHelper.
     */
    @Test(timeout = 5000)
    public void testExit() {
        final Runnable exitTask = mock(Runnable.class);

        doProtected(true, () -> ShutdownHelper.exit(exitTask, 0));

        verify(exitTask).run();
        verifyNoMoreInteractions(exitTask);
    }

    /**
     * Test of shutdownExecutors method, of class ShutdownHelper.
     */
    @Test(timeout = 5000)
    public void testShutdownExecutors() {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);
        ShutdownHelper.shutdownExecutors(executor1, executor2);

        verify(executor1).shutdown();
        verify(executor2).shutdown();
        verifyNoMoreInteractions(executor1, executor2);
    }

    @Test(timeout = 5000)
    public void testShutdownExecutorsForEmpty() {
        ShutdownHelper.shutdownExecutors();
    }

    private static Runnable dummyRunnable() {
        return () -> { };
    }

    /**
     * Test of shutdownNowExecutors method, of class ShutdownHelper.
     */
    @Test(timeout = 5000)
    public void testShutdownNowExecutors() {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        List<Runnable> tasks = Arrays.asList(
                dummyRunnable(),
                dummyRunnable(),
                dummyRunnable(),
                dummyRunnable(),
                dummyRunnable());

        stub(executor1.shutdownNow()).toReturn(tasks.subList(0, 2));
        stub(executor2.shutdownNow()).toReturn(tasks.subList(2, 5));

        assertEquals(tasks, ShutdownHelper.shutdownNowExecutors(executor1, executor2));

        verify(executor1).shutdownNow();
        verify(executor2).shutdownNow();
        verifyNoMoreInteractions(executor1, executor2);
    }

    @Test(timeout = 5000)
    public void testShutdownNowExecutorsEtmpy() {
        assertEquals(Collections.emptyList(), ShutdownHelper.shutdownNowExecutors());
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutorsSilently_ExecutorServiceArr() throws InterruptedException {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        stub(executor1.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(true);
        stub(executor2.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(true);

        ShutdownHelper.awaitTerminateExecutorsSilently(executor1, executor2);
        assertFalse(Thread.currentThread().isInterrupted());

        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(executor1, executor2);
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutorsSilently_ExecutorServiceArrInterrupt() throws InterruptedException {
        ExecutorService executor = mock(ExecutorService.class);

        stub(executor.awaitTermination(anyLong(), any(TimeUnit.class))).toThrow(new InterruptedException());

        ShutdownHelper.awaitTerminateExecutorsSilently(executor);
        assertTrue(Thread.currentThread().isInterrupted());

        verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(executor);
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutorsSilently_ExecutorServiceArrEmpty() throws InterruptedException {
        ShutdownHelper.awaitTerminateExecutorsSilently();
        assertFalse(Thread.currentThread().isInterrupted());
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutorsSilently_3args() throws InterruptedException {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        stub(executor1.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(true);
        stub(executor2.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(true);
        stub(executor1.isTerminated()).toReturn(true);
        stub(executor2.isTerminated()).toReturn(true);

        assertTrue(ShutdownHelper.awaitTerminateExecutorsSilently(1, TimeUnit.DAYS, executor1, executor2));
        assertFalse(Thread.currentThread().isInterrupted());

        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutorsSilently_3argsTimeout() throws InterruptedException {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        stub(executor1.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(true);
        stub(executor2.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(false);
        stub(executor1.isTerminated()).toReturn(true);
        stub(executor2.isTerminated()).toReturn(false);

        assertFalse(ShutdownHelper.awaitTerminateExecutorsSilently(1, TimeUnit.DAYS, executor1, executor2));
        assertFalse(Thread.currentThread().isInterrupted());

        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutorsSilently_3argsInterrupt() throws InterruptedException {
        ExecutorService executor = mock(ExecutorService.class);

        stub(executor.awaitTermination(anyLong(), any(TimeUnit.class))).toThrow(new InterruptedException());
        stub(executor.isTerminated()).toReturn(false);

        assertFalse(ShutdownHelper.awaitTerminateExecutorsSilently(1, TimeUnit.DAYS, executor));
        assertTrue(Thread.currentThread().isInterrupted());

        verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(executor);
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutorsSilently_3argsEmpty() throws InterruptedException {
        assertTrue(ShutdownHelper.awaitTerminateExecutorsSilently(Long.MAX_VALUE, TimeUnit.DAYS));
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutors_ExecutorServiceArr() throws InterruptedException {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        stub(executor1.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(true);
        stub(executor2.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(true);

        ShutdownHelper.awaitTerminateExecutors(executor1, executor2);

        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(executor1, executor2);
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutors_ExecutorServiceArrInterrupt() throws InterruptedException {
        ExecutorService executor = mock(ExecutorService.class);

        stub(executor.awaitTermination(anyLong(), any(TimeUnit.class))).toThrow(new InterruptedException());

        try {
            ShutdownHelper.awaitTerminateExecutors(executor);
            fail("Thread interrupt expected.");
        } catch (InterruptedException ex) {
        }

        verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(executor);
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutors_ExecutorServiceArrEmpty() throws InterruptedException {
        ShutdownHelper.awaitTerminateExecutors();
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutors_3args() throws InterruptedException {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        stub(executor1.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(true);
        stub(executor2.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(true);
        stub(executor1.isTerminated()).toReturn(true);
        stub(executor2.isTerminated()).toReturn(true);

        assertTrue(ShutdownHelper.awaitTerminateExecutors(1, TimeUnit.DAYS, executor1, executor2));

        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutors_3argsTimeout() throws InterruptedException {
        ExecutorService executor1 = mock(ExecutorService.class);
        ExecutorService executor2 = mock(ExecutorService.class);

        stub(executor1.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(true);
        stub(executor2.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(false);
        stub(executor1.isTerminated()).toReturn(true);
        stub(executor2.isTerminated()).toReturn(false);

        assertFalse(ShutdownHelper.awaitTerminateExecutors(1, TimeUnit.DAYS, executor1, executor2));

        verify(executor1).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executor2).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutors_3argsInterrupt() throws InterruptedException {
        ExecutorService executor = mock(ExecutorService.class);

        stub(executor.awaitTermination(anyLong(), any(TimeUnit.class))).toThrow(new InterruptedException());
        stub(executor.isTerminated()).toReturn(false);

        try {
            ShutdownHelper.awaitTerminateExecutors(1, TimeUnit.DAYS, executor);
            fail("Thread interrupt expected.");
        } catch (InterruptedException ex) {
        }

        verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(executor);
    }

    @Test(timeout = 5000)
    public void testAwaitTerminateExecutors_3argsEmpty() throws InterruptedException {
        assertTrue(ShutdownHelper.awaitTerminateExecutors(Long.MAX_VALUE, TimeUnit.DAYS));
    }

    private static class TestSecurityException extends SecurityException {
        private static final long serialVersionUID = 2453212575678751559L;

        public TestSecurityException(String s) {
            super(s);
        }
    }

    private static class ProtectFromExitSecurityManager extends SecurityManager {
        private static final Permission EXIT_VM_PERMISSON = new RuntimePermission("exitVM.*");

        private final AtomicLong violateCount = new AtomicLong(0);
        private final WaitableSignal violateSignal = new WaitableSignal();

        @Override
        public void checkPermission(Permission perm) {
            if (EXIT_VM_PERMISSON.implies(perm)) {
                StackTraceElement[] traces = new Exception().getStackTrace();
                for (int i = 1; i < traces.length; i++) {
                    StackTraceElement trace = traces[i];

                    String className = trace.getClassName();
                    if (!className.equals(SecurityManager.class.getName())) {
                        if (!className.startsWith("org.jtrim2.")) {
                            violateCount.incrementAndGet();
                            violateSignal.signal();
                            throw new TestSecurityException("Protected from shutdown");
                        }
                        break;
                    }
                }
            }
        }

        public long getViolateCount() {
            return violateCount.get();
        }

        public void waitViolation() {
            violateSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            checkPermission(perm);
        }
    }
}
