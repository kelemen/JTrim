package org.jtrim.cancel;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.jtrim.utils.TestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class CancelableWaitsTest {

    public CancelableWaitsTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        // Clear the interrupted status of the current thread before executing
        // any of the test methods.
        Thread.interrupted();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(CancelableWaits.class);
    }

    /**
     * Test the lock method when is available.
     *
     * @throws InterruptedException this exception should never be thrown by
     *   this method
     */
    @Test
    public void testLockAvailable() throws InterruptedException {
        Lock lock = mock(Lock.class);

        CancelableWaits.lock(Cancellation.UNCANCELABLE_TOKEN, lock);

        verify(lock).lockInterruptibly();
        verifyNoMoreInteractions(lock);
    }

    /**
     * Test the lock method when the CancellationToken was canceled before the
     * call.
     */
    @Test(expected = OperationCanceledException.class)
    public void testLockPreCanceled() {
        Lock lock = mock(Lock.class);
        try {
            CancelableWaits.lock(Cancellation.CANCELED_TOKEN, lock);
        } finally {
            verifyZeroInteractions(lock);
        }
    }

    /**
     * Test the lock method when the CancellationToken gets canceled after the
     * call.
     *
     * @throws InterruptedException this exception should never be thrown by
     *   this method
     */
    @Test(expected = OperationCanceledException.class)
    public void testLockPostCanceled() throws InterruptedException {
        Lock lock = mock(Lock.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws InterruptedException {
                cancelSource.getController().cancel();
                Thread.sleep(5000);
                fail("Interrupt expected.");
                return null;
            }
        }).when(lock).lockInterruptibly();

        try {
            CancelableWaits.lock(cancelSource.getToken(), lock);
        } finally {
            verify(lock).lockInterruptibly();
            verifyNoMoreInteractions(lock);
        }
    }

    private static void checkTime(
            long tolerableMillis,
            long time,
            TimeUnit unit,
            long expected,
            TimeUnit expectedUnit) {

        long timeMillis = unit.toMillis(time);
        long expectedMillis = expectedUnit.toMillis(expected);

        assertTrue("The specified time values must be within " + tolerableMillis + " ms to each other",
                Math.abs(timeMillis - expectedMillis) < tolerableMillis);
    }

    private static Answer<Boolean> timeCheckAnswer(
            final long tolerableMillis,
            final long expected,
            final TimeUnit expectedUnit) {

        return new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) {
                Object[] arguments = invocation.getArguments();
                long timeout = (Long)arguments[0];
                TimeUnit unit = (TimeUnit)arguments[1];

                checkTime(tolerableMillis, timeout, unit, expected, expectedUnit);
                return true;
            }
        };
    }

    /**
     * Test the tryLock method when is available.
     *
     * @throws InterruptedException this exception should never be thrown by
     *   this method
     */
    @Test(timeout = 5000)
    public void testTryLockAvailable() throws InterruptedException {
        for (Boolean expectedResult: Arrays.asList(false, true)) {
            Lock lock = mock(Lock.class);
            stub(lock.tryLock(anyLong(), any(TimeUnit.class))).toReturn(expectedResult);

            boolean result = CancelableWaits.tryLock(
                    Cancellation.UNCANCELABLE_TOKEN,
                    Long.MAX_VALUE,
                    TimeUnit.DAYS,
                    lock);

            assertEquals(expectedResult.booleanValue(), result);
            verify(lock).tryLock(anyLong(), any(TimeUnit.class));
            verifyNoMoreInteractions(lock);
        }

        // Test if the passed timeout values passed are close enough to the
        // values added.
        final long expected = 1;
        final TimeUnit expectedUnit = TimeUnit.DAYS;
        // We pass 1 sec tolerance because 1 second is small compared to a day
        // and we don't want this test to be affected too much by thread
        // scheduling.
        Answer<Boolean> checkAnswer = timeCheckAnswer(1000, expected, expectedUnit);

        for (TimeUnit unit: TimeUnit.values()) {
            Lock lock = mock(Lock.class);
            stub(lock.tryLock(anyLong(), any(TimeUnit.class))).toAnswer(checkAnswer);

            long timeInUnit = unit.convert(expected, expectedUnit);
            CancelableWaits.tryLock(Cancellation.UNCANCELABLE_TOKEN, timeInUnit, unit, lock);
        }
    }

    /**
     * Test the tryLock method when the CancellationToken was canceled before
     * the call.
     */
    @Test(expected = OperationCanceledException.class, timeout = 5000)
    public void testTryLockPreCanceled() {
        Lock lock = mock(Lock.class);

        try {
            CancelableWaits.tryLock(Cancellation.CANCELED_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS, lock);
        } finally {
            verifyZeroInteractions(lock);
        }
    }

    /**
     * Test the tryLock method when the CancellationToken gets canceled after
     * the call.
     *
     * @throws InterruptedException this exception should never be thrown by
     *   this method
     */
    @Test(expected = OperationCanceledException.class)
    public void testTryLockPostCanceled() throws InterruptedException {
        Lock lock = mock(Lock.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        stub(lock.tryLock(anyLong(), any(TimeUnit.class))).toAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws InterruptedException {
                cancelSource.getController().cancel();
                Thread.sleep(5000);
                fail("Interrupt expected.");
                return false;
            }
        });

        try {
            CancelableWaits.tryLock(cancelSource.getToken(), Long.MAX_VALUE, TimeUnit.DAYS, lock);
        } finally {
            verify(lock).tryLock(anyLong(), any(TimeUnit.class));
            verifyNoMoreInteractions(lock);
        }
    }

    /**
     * Test of sleep method, of class CancelableWaits without cancellation.
     */
    @Test(timeout = 5000)
    public void testSleep() {
        CancelableWaits.sleep(Cancellation.UNCANCELABLE_TOKEN, 100, TimeUnit.NANOSECONDS);
    }

    /**
     * Test of sleep method, of class CancelableWaits when canceled before the
     * call.
     */
    @Test(expected = OperationCanceledException.class, timeout = 5000)
    public void testSleepPreCanceled() {
        CancelableWaits.sleep(Cancellation.CANCELED_TOKEN, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    /**
     * Test of sleep method, of class CancelableWaits when canceled after the
     * call.
     *
     * @throws InterruptedException this exception should never be thrown by
     *   this method
     */
    @Test(expected = OperationCanceledException.class, timeout = 5000)
    public void testSleepPostCanceled() throws InterruptedException {
        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        Thread cancelThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ex) {
                } finally {
                    cancelSource.getController().cancel();
                }
            }
        });
        cancelThread.start();
        try {
            CancelableWaits.sleep(cancelSource.getToken(), Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } finally {
            cancelThread.interrupt();
            cancelThread.join();
        }
    }

    /**
     * Test of awaitTerminate method, of class CancelableWaits without
     * cancellation.
     *
     * @throws InterruptedException this exception should never be thrown by
     *   this method
     */
    @Test(timeout = 5000)
    public void testAwaitTerminate() throws InterruptedException {
        for (Boolean expectedResult: Arrays.asList(false, true)) {
            ExecutorService executor = mock(ExecutorService.class);
            stub(executor.awaitTermination(anyLong(), any(TimeUnit.class))).toReturn(expectedResult);

            boolean result = CancelableWaits.awaitTerminate(
                    Cancellation.UNCANCELABLE_TOKEN,
                    Long.MAX_VALUE,
                    TimeUnit.DAYS,
                    executor);

            assertEquals(expectedResult.booleanValue(), result);
            verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
            verifyNoMoreInteractions(executor);
        }

        // Test if the passed timeout values passed are close enough to the
        // values added.
        final long expected = 1;
        final TimeUnit expectedUnit = TimeUnit.DAYS;
        // We pass 1 sec tolerance because 1 second is small compared to a day
        // and we don't want this test to be affected too much by thread
        // scheduling.
        Answer<Boolean> checkAnswer = timeCheckAnswer(1000, expected, expectedUnit);

        for (TimeUnit unit: TimeUnit.values()) {
            ExecutorService executor = mock(ExecutorService.class);
            stub(executor.awaitTermination(anyLong(), any(TimeUnit.class))).toAnswer(checkAnswer);

            long timeInUnit = unit.convert(expected, expectedUnit);
            CancelableWaits.awaitTerminate(Cancellation.UNCANCELABLE_TOKEN, timeInUnit, unit, executor);
        }
    }

    /**
     * Test of awaitTerminate method, of class CancelableWaits when canceled
     * before the call.
     */
    @Test(expected = OperationCanceledException.class, timeout = 5000)
    public void testAwaitTerminatePreCanceled() {
        ExecutorService executor = mock(ExecutorService.class);

        try {
            CancelableWaits.awaitTerminate(Cancellation.CANCELED_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS, executor);
        } finally {
            verifyZeroInteractions(executor);
        }
    }

    /**
     * Test of awaitTerminate method, of class CancelableWaits when canceled
     * after the call.
     *
     * @throws InterruptedException this exception should never be thrown by
     *   this method
     */
    @Test(expected = OperationCanceledException.class, timeout = 5000)
    public void testAwaitTerminatePostCanceled() throws InterruptedException {
        ExecutorService executor = mock(ExecutorService.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws InterruptedException {
                cancelSource.getController().cancel();
                Thread.sleep(5000);
                fail("Interrupt expected.");
                return null;
            }
        }).when(executor).awaitTermination(anyLong(), any(TimeUnit.class));

        try {
            CancelableWaits.awaitTerminate(cancelSource.getToken(), Long.MAX_VALUE, TimeUnit.MINUTES, executor);
        } finally {
            verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
            verifyNoMoreInteractions(executor);
        }
    }

    /**
     * Test of await method, of class CancelableWaits when waiting for a
     * condition.
     *
     * @throws InterruptedException this exception should never be thrown by
     *   this method
     */
    @Test
    public void testAwaitCondition() throws InterruptedException {
        Condition condition = mock(Condition.class);

        CancelableWaits.await(Cancellation.UNCANCELABLE_TOKEN, condition);

        verify(condition).await();
        verifyNoMoreInteractions(condition);
    }

    /**
     * Test of await method, of class CancelableWaits when waiting for a
     * condition.
     *
     * @throws InterruptedException this exception should never be thrown by
     *   this method
     */
    @Test
    public void testAwaitCondition2() throws InterruptedException {
        Condition condition = mock(Condition.class);

        Mockito.doThrow(InterruptedException.class).
                doNothing().
                when(condition).await();

        CancelableWaits.await(Cancellation.UNCANCELABLE_TOKEN, condition);

        verify(condition, times(2)).await();
        verifyNoMoreInteractions(condition);
    }

    @Test(expected = OperationCanceledException.class)
    public void testAwaitConditionPreCancel() throws InterruptedException {
        Condition condition = mock(Condition.class);

        try {
            CancelableWaits.await(Cancellation.CANCELED_TOKEN, condition);
        } finally {
            verifyNoMoreInteractions(condition);
        }
    }

    @Test(expected = OperationCanceledException.class)
    public void testAwaitConditionPostCancel() throws InterruptedException {
        Condition condition = mock(Condition.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws InterruptedException {
                cancelSource.getController().cancel();
                Thread.sleep(5000);
                fail("Interrupt expected.");
                return null;
            }
        }).when(condition).await();

        CancelableWaits.await(cancelSource.getToken(), condition);
    }

    @Test
    public void testAwaitConditionWithTimeout() throws InterruptedException {
        for (Boolean expectedResult: Arrays.asList(false, true)) {
            Condition condition = mock(Condition.class);

            Mockito.stub(condition.await(anyLong(), any(TimeUnit.class)))
                    .toReturn(expectedResult);

            boolean result = CancelableWaits.await(
                    Cancellation.UNCANCELABLE_TOKEN,
                    Long.MAX_VALUE,
                    TimeUnit.NANOSECONDS,
                    condition);
            assertEquals(expectedResult.booleanValue(), result);

            verify(condition).await(anyLong(), any(TimeUnit.class));
            verifyNoMoreInteractions(condition);
        }
    }

    @Test
    public void testAwaitConditionWithTimeout2() throws InterruptedException {
        Condition condition = mock(Condition.class);

        Mockito.stub(condition.await(anyLong(), any(TimeUnit.class)))
                .toThrow(new InterruptedException())
                .toReturn(true);

        CancelableWaits.await(Cancellation.UNCANCELABLE_TOKEN, Long.MAX_VALUE, TimeUnit.NANOSECONDS, condition);

        verify(condition, times(2)).await(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(condition);
    }

    @Test(expected = OperationCanceledException.class)
    public void testAwaitConditionWithTimeoutPreCancel() throws InterruptedException {
        Condition condition = mock(Condition.class);

        try {
            CancelableWaits.await(Cancellation.CANCELED_TOKEN, Long.MAX_VALUE, TimeUnit.NANOSECONDS, condition);
        } finally {
            verifyNoMoreInteractions(condition);
        }
    }

    @Test(expected = OperationCanceledException.class)
    public void testAwaitConditionWithTimeoutPostCancel() throws InterruptedException {
        Condition condition = mock(Condition.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws InterruptedException {
                cancelSource.getController().cancel();
                Thread.sleep(5000);
                fail("Interrupt expected.");
                return null;
            }
        }).when(condition).await(anyLong(), any(TimeUnit.class));

        CancelableWaits.await(cancelSource.getToken(), Long.MAX_VALUE, TimeUnit.NANOSECONDS, condition);
    }

    @Test
    public void testAwaitConditionWithTimeoutTimeouts() throws InterruptedException {
        Condition condition = mock(Condition.class);

        Mockito.stub(condition.await(anyLong(), any(TimeUnit.class)))
                .toAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws InterruptedException {
                Object[] arguments = invocation.getArguments();
                long timeout = (Long)arguments[0];
                TimeUnit unit = (TimeUnit)arguments[1];

                long millis = unit.toMillis(timeout) + 10;
                Thread.sleep(millis);
                return false;
            }
        });

        boolean result = CancelableWaits.await(Cancellation.UNCANCELABLE_TOKEN, 100, TimeUnit.NANOSECONDS, condition);
        assertFalse(result);

        verify(condition).await(anyLong(), any(TimeUnit.class));
        verifyNoMoreInteractions(condition);
    }

    @Test
    public void testAwaitGenericWait() throws InterruptedException {
        InterruptibleWait wait = mock(InterruptibleWait.class);

        CancelableWaits.await(Cancellation.UNCANCELABLE_TOKEN, wait);

        verify(wait).await();
        verifyNoMoreInteractions(wait);
    }

    @Test
    public void testAwaitGenericWait2() throws InterruptedException {
        InterruptibleWait wait = mock(InterruptibleWait.class);

        Mockito.doNothing().
                doThrow(InterruptedException.class).
                when(wait).await();

        CancelableWaits.await(Cancellation.UNCANCELABLE_TOKEN, wait);

        verify(wait).await();
        verifyNoMoreInteractions(wait);
    }

    @Test(expected = OperationCanceledException.class)
    public void testAwaitGenericWaitPreCanceled() throws InterruptedException {
        InterruptibleWait wait = mock(InterruptibleWait.class);

        try {
            CancelableWaits.await(Cancellation.CANCELED_TOKEN, wait);
        } finally {
            verifyZeroInteractions(wait);
        }
    }

    @Test(expected = OperationCanceledException.class)
    public void testAwaitGenericWaitPostCanceled() throws InterruptedException {
        InterruptibleWait wait = mock(InterruptibleWait.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws InterruptedException {
                cancelSource.getController().cancel();
                Thread.sleep(5000);
                fail("Interrupt expected.");
                return null;
            }
        }).when(wait).await();

        try {
            CancelableWaits.await(cancelSource.getToken(), wait);
        } finally {
            verify(wait).await();
            verifyNoMoreInteractions(wait);
        }
    }

    @Test
    public void testAwaitGenericTimeoutWait() throws InterruptedException {
        for (Boolean expectedResult: Arrays.asList(false, true)) {
            InterruptibleLimitedWait wait = mock(InterruptibleLimitedWait.class);

            stub(wait.await(anyLong())).toReturn(expectedResult);

            boolean result = CancelableWaits.await(
                    Cancellation.UNCANCELABLE_TOKEN,
                    Long.MAX_VALUE,
                    TimeUnit.DAYS,
                    wait);
            assertEquals(expectedResult.booleanValue(), result);

            verify(wait).await(anyLong());
            verifyNoMoreInteractions(wait);
        }
    }

    @Test
    public void testAwaitGenericTimeoutWait2() throws InterruptedException {
        for (Boolean expectedResult: Arrays.asList(false, true)) {
            InterruptibleLimitedWait wait = mock(InterruptibleLimitedWait.class);

            stub(wait.await(anyLong()))
                    .toThrow(new InterruptedException())
                    .toReturn(expectedResult);

            boolean result = CancelableWaits.await(
                    Cancellation.UNCANCELABLE_TOKEN,
                    Long.MAX_VALUE,
                    TimeUnit.DAYS,
                    wait);
            assertEquals(expectedResult.booleanValue(), result);

            verify(wait, times(2)).await(anyLong());
            verifyNoMoreInteractions(wait);
        }
    }

    @Test
    public void testAwaitGenericTimeoutWait3() throws InterruptedException {
        // Test if the passed timeout values passed are close enough to the
        // values added.
        final long expected = 1;
        final TimeUnit expectedUnit = TimeUnit.DAYS;

        // We pass 1 sec tolerance because 1 second is small compared to a day
        // and we don't want this test to be affected too much by thread
        // scheduling.
        Answer<Boolean> checkAnswer = new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) {
                Object[] arguments = invocation.getArguments();
                long timeout = (Long)arguments[0];
                TimeUnit unit = TimeUnit.NANOSECONDS;

                checkTime(1000, timeout, unit, expected, expectedUnit);
                return true;
            }
        };

        for (TimeUnit unit: TimeUnit.values()) {
            InterruptibleLimitedWait wait = mock(InterruptibleLimitedWait.class);
            stub(wait.await(anyLong())).toAnswer(checkAnswer);

            long timeInUnit = unit.convert(expected, expectedUnit);
            CancelableWaits.await(Cancellation.UNCANCELABLE_TOKEN, timeInUnit, unit, wait);
            verify(wait).await(anyLong());
            verifyNoMoreInteractions(wait);
        }
    }

    @Test(expected = OperationCanceledException.class)
    public void testAwaitGenericTimeoutWaitPreCanceled() throws InterruptedException {
        InterruptibleLimitedWait wait = mock(InterruptibleLimitedWait.class);

        try {
            CancelableWaits.await(Cancellation.CANCELED_TOKEN, Long.MAX_VALUE, TimeUnit.DAYS, wait);
        } finally {
            verifyZeroInteractions(wait);
        }
    }

    @Test(expected = OperationCanceledException.class)
    public void testAwaitGenericTimeoutWaitPostCanceled() throws InterruptedException {
        InterruptibleLimitedWait wait = mock(InterruptibleLimitedWait.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        stub(wait.await(anyLong())).toAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws InterruptedException {
                cancelSource.getController().cancel();
                Thread.sleep(5000);
                fail("Interrupt expected.");
                return null;
            }
        });

        try {
            CancelableWaits.await(cancelSource.getToken(), Long.MAX_VALUE, TimeUnit.DAYS, wait);
        } finally {
            verify(wait).await(anyLong());
            verifyNoMoreInteractions(wait);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalLock1() {
        CancelableWaits.lock(null, mock(Lock.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalLock2() {
        CancelableWaits.lock(mock(CancellationToken.class), null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalTryLock1() {
        CancelableWaits.tryLock(null, 0, TimeUnit.DAYS, mock(Lock.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalTryLock2() {
        CancelableWaits.tryLock(mock(CancellationToken.class), -1, TimeUnit.DAYS, mock(Lock.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalTryLock3() {
        CancelableWaits.tryLock(mock(CancellationToken.class), 0, null, mock(Lock.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalTryLock4() {
        CancelableWaits.tryLock(mock(CancellationToken.class), 0, TimeUnit.DAYS, null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalSleep1() {
        CancelableWaits.sleep(null, 0, TimeUnit.DAYS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalSleep2() {
        CancelableWaits.sleep(mock(CancellationToken.class), -1, TimeUnit.DAYS);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalSleep3() {
        CancelableWaits.sleep(mock(CancellationToken.class), 0, null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitTerminate1() {
        CancelableWaits.awaitTerminate(null, 0, TimeUnit.DAYS, mock(ExecutorService.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalAwaitTerminate2() {
        CancelableWaits.awaitTerminate(
                mock(CancellationToken.class),
                -1,
                TimeUnit.DAYS,
                mock(ExecutorService.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitTerminate3() {
        CancelableWaits.awaitTerminate(mock(CancellationToken.class), 0, null, mock(ExecutorService.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitTerminate4() {
        CancelableWaits.awaitTerminate(mock(CancellationToken.class), 0, TimeUnit.DAYS, null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitConditionWithTimeout1() {
        CancelableWaits.await(null, 0, TimeUnit.DAYS, mock(Condition.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalAwaitConditionWithTimeout2() {
        CancelableWaits.await(mock(CancellationToken.class), -1, TimeUnit.DAYS, mock(Condition.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitConditionWithTimeout3() {
        CancelableWaits.await(mock(CancellationToken.class), 0, null, mock(Condition.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitConditionWithTimeout4() {
        CancelableWaits.await(mock(CancellationToken.class), 0, TimeUnit.DAYS, (Condition)null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitCondition1() {
        CancelableWaits.await(null, mock(Condition.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitCondition2() {
        CancelableWaits.await(mock(CancellationToken.class), (Condition)null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitGenericWithTimeout1() {
        CancelableWaits.await(null, 0, TimeUnit.DAYS, mock(InterruptibleLimitedWait.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalAwaitGenericWithTimeout2() {
        CancelableWaits.await(
                mock(CancellationToken.class),
                -1,
                TimeUnit.DAYS,
                mock(InterruptibleLimitedWait.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitGenericWithTimeout3() {
        CancelableWaits.await(mock(CancellationToken.class), 0, null, mock(InterruptibleLimitedWait.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitGenericWithTimeout4() {
        CancelableWaits.await(mock(CancellationToken.class), 0, TimeUnit.DAYS, (InterruptibleLimitedWait)null);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitGeneric1() {
        CancelableWaits.await(null, mock(InterruptibleWait.class));
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalAwaitGeneric2() {
        CancelableWaits.await(mock(CancellationToken.class), (InterruptibleWait)null);
    }
}
