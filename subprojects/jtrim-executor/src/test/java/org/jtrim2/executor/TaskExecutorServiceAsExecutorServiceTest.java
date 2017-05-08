package org.jtrim2.executor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.Matcher;
import org.jtrim2.cancel.CancellationToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TaskExecutorServiceAsExecutorServiceTest {
    @Before
    public void setUp() {
        Thread.interrupted(); // clear interrupted status
    }

    /**
     * Test of shutdown method, of class TaskExecutorServiceAsExecutorService.
     */
    @Test
    public void testShutdown() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(wrapped, false);

        executor.shutdown();

        verify(wrapped).shutdown();
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of shutdownNow method, of class TaskExecutorServiceAsExecutorService.
     */
    @Test
    public void testShutdownNow() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(wrapped, false);

        List<Runnable> result = executor.shutdownNow();
        assertTrue(result.isEmpty());

        verify(wrapped).shutdownAndCancel();
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of isShutdown method, of class TaskExecutorServiceAsExecutorService.
     */
    @Test
    public void testIsShutdown() {
        for (boolean result: Arrays.asList(false, true)) {
            TaskExecutorService wrapped = mock(TaskExecutorService.class);
            TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(wrapped, false);

            stub(wrapped.isShutdown()).toReturn(result);

            assertEquals(result, executor.isShutdown());

            verify(wrapped).isShutdown();
            verifyNoMoreInteractions(wrapped);
        }
    }

    /**
     * Test of isTerminated method, of class TaskExecutorServiceAsExecutorService.
     */
    @Test
    public void testIsTerminated() {
        for (boolean result: Arrays.asList(false, true)) {
            TaskExecutorService wrapped = mock(TaskExecutorService.class);
            TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(wrapped, false);

            stub(wrapped.isTerminated()).toReturn(result);

            assertEquals(result, executor.isTerminated());

            verify(wrapped).isTerminated();
            verifyNoMoreInteractions(wrapped);
        }
    }

    @Test
    public void testAwaitTermination() throws Exception {
        for (boolean result: Arrays.asList(false, true)) {
            TaskExecutorService wrapped = mock(TaskExecutorService.class);
            TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(wrapped, false);

            long timeout = 543638468943L;
            TimeUnit unit = TimeUnit.MINUTES;

            stub(wrapped.tryAwaitTermination(any(CancellationToken.class), anyLong(), any(TimeUnit.class)))
                    .toReturn(result);

            assertEquals(result, executor.awaitTermination(timeout, unit));

            Matcher<CancellationToken> tokenMatcher = new ArgumentMatcher<CancellationToken>() {
                @Override
                public boolean matches(Object argument) {
                    return !((CancellationToken)argument).isCanceled();
                }
            };

            verify(wrapped).tryAwaitTermination(argThat(tokenMatcher), eq(timeout), eq(unit));
            verifyNoMoreInteractions(wrapped);
        }
    }

    /**
     * Test of execute method, of class TaskExecutorServiceAsExecutorService.
     */
    @Test
    public void testSimpleExecute() {
        TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(
                new SyncTaskExecutor(), false);

        for (int i = 0; i < 5; i++) {
            Runnable task = mock(Runnable.class);
            executor.execute(task);

            verify(task).run();
            verifyNoMoreInteractions(task);
        }
    }

    @Test
    public void testSubmitRunnable() throws Exception {
        ManualTaskExecutor manual = new ManualTaskExecutor(false);
        TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(
                new UpgradedTaskExecutor(manual), false);

        for (int i = 0; i < 5; i++) {
            Runnable task = mock(Runnable.class);
            Future<?> future = executor.submit(task);

            verifyZeroInteractions(task);

            assertFalse(future.isCancelled());
            assertFalse(future.isDone());

            manual.executeCurrentlySubmitted();

            assertFalse(future.isCancelled());
            assertTrue(future.isDone());

            assertNull(future.get());
            assertNull(future.get(0, TimeUnit.NANOSECONDS));
            assertNull(future.get(Long.MAX_VALUE, TimeUnit.DAYS));

            verify(task).run();
            verifyNoMoreInteractions(task);
        }
    }

    @Test
    public void testSubmitRunnableWithResult() throws Exception {
        ManualTaskExecutor manual = new ManualTaskExecutor(false);
        TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(
                new UpgradedTaskExecutor(manual), false);

        for (int i = 0; i < 5; i++) {
            Runnable task = mock(Runnable.class);
            Object result = new Object();

            Future<?> future = executor.submit(task, result);

            verifyZeroInteractions(task);

            assertFalse(future.isCancelled());
            assertFalse(future.isDone());

            manual.executeCurrentlySubmitted();

            assertFalse(future.isCancelled());
            assertTrue(future.isDone());

            assertSame(result, future.get());
            assertSame(result, future.get(0, TimeUnit.NANOSECONDS));
            assertSame(result, future.get(Long.MAX_VALUE, TimeUnit.DAYS));

            verify(task).run();
            verifyNoMoreInteractions(task);
        }
    }

    @Test
    public void testSubmitCallable() throws Exception {
        ManualTaskExecutor manual = new ManualTaskExecutor(false);
        TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(
                new UpgradedTaskExecutor(manual), false);

        for (int i = 0; i < 5; i++) {
            Callable<?> task = mock(Callable.class);
            Object result = new Object();

            stub(task.call()).toReturn(result);

            Future<?> future = executor.submit(task);

            verifyZeroInteractions(task);

            assertFalse(future.isCancelled());
            assertFalse(future.isDone());

            manual.executeCurrentlySubmitted();

            assertFalse(future.isCancelled());
            assertTrue(future.isDone());

            assertSame(result, future.get());
            assertSame(result, future.get(0, TimeUnit.NANOSECONDS));
            assertSame(result, future.get(Long.MAX_VALUE, TimeUnit.DAYS));

            verify(task).call();
            verifyNoMoreInteractions(task);
        }
    }

    @Test
    public void testSubmitRunnableAndCancel() throws Exception {
        ManualTaskExecutor manual = new ManualTaskExecutor(false);
        TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(
                new UpgradedTaskExecutor(manual), false);

        for (int i = 0; i < 5; i++) {
            Runnable task = mock(Runnable.class);
            Future<?> future = executor.submit(task);

            verifyZeroInteractions(task);

            assertTrue(future.cancel(true));

            assertTrue(future.isCancelled());
            assertTrue(future.isDone());

            manual.executeCurrentlySubmitted();

            assertTrue(future.isCancelled());
            assertTrue(future.isDone());

            verifyZeroInteractions(task);
        }
    }

    @Test
    public void testSubmitRunnableAndCancelWhileRunningNoInterrupt() throws Exception {
        ManualTaskExecutor manual = new ManualTaskExecutor(false);
        TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(
                new UpgradedTaskExecutor(manual), false);

        for (int i = 0; i < 5; i++) {
            Runnable task = mock(Runnable.class);

            final AtomicBoolean interrupted = new AtomicBoolean(true);
            final AtomicReference<Future<?>> futureRef = new AtomicReference<>(null);
            doAnswer((InvocationOnMock invocation) -> {
                futureRef.get().cancel(true);
                interrupted.set(Thread.currentThread().isInterrupted());
                return null;
            }).when(task).run();

            Future<?> future = executor.submit(task);
            futureRef.set(future);

            verifyZeroInteractions(task);

            assertFalse(future.isCancelled());
            assertFalse(future.isDone());

            manual.executeCurrentlySubmitted();

            assertFalse(interrupted.get());
            assertTrue(future.isCancelled());
            assertTrue(future.isDone());

            verify(task).run();
            verifyNoMoreInteractions(task);
        }
    }

    @Test
    public void testSubmitRunnableAndCancelWhileRunningInterrupt() throws Exception {
        ManualTaskExecutor manual = new ManualTaskExecutor(false);
        TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(
                new UpgradedTaskExecutor(manual), true);

        for (int i = 0; i < 5; i++) {
            Thread.interrupted(); // clear interrupted status

            Runnable task = mock(Runnable.class);

            final AtomicBoolean interrupted = new AtomicBoolean(false);
            final AtomicReference<Future<?>> futureRef = new AtomicReference<>(null);
            doAnswer((InvocationOnMock invocation) -> {
                futureRef.get().cancel(true);
                interrupted.set(Thread.currentThread().isInterrupted());
                return null;
            }).when(task).run();

            Future<?> future = executor.submit(task);
            futureRef.set(future);

            verifyZeroInteractions(task);

            assertFalse(future.isCancelled());
            assertFalse(future.isDone());

            manual.executeCurrentlySubmitted();

            assertTrue(interrupted.get());
            assertTrue(future.isCancelled());
            assertTrue(future.isDone());

            verify(task).run();
            verifyNoMoreInteractions(task);
        }
    }

    @Test
    public void testSubmitCallableAndCancelWhileRunningInterrupt() throws Exception {
        ManualTaskExecutor manual = new ManualTaskExecutor(false);
        TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(
                new UpgradedTaskExecutor(manual), true);

        for (int i = 0; i < 5; i++) {
            Thread.interrupted(); // clear interrupted status

            Callable<?> task = mock(Callable.class);
            final Object result = new Object();

            final AtomicBoolean interrupted = new AtomicBoolean(false);
            final AtomicReference<Future<?>> futureRef = new AtomicReference<>(null);
            stub(task.call()).toAnswer((InvocationOnMock invocation) -> {
                futureRef.get().cancel(true);
                interrupted.set(Thread.currentThread().isInterrupted());
                return result;
            });

            Future<?> future = executor.submit(task);
            futureRef.set(future);

            verifyZeroInteractions(task);

            assertFalse(future.isCancelled());
            assertFalse(future.isDone());

            manual.executeCurrentlySubmitted();

            assertTrue(interrupted.get());
            assertTrue(future.isCancelled());
            assertTrue(future.isDone());

            verify(task).call();
            verifyNoMoreInteractions(task);
        }
    }

     @Test
     public void testPostShutdownExecute1() throws Exception {
         TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(
                new SyncTaskExecutor(), false);
         executor.shutdown();

         Runnable task = mock(Runnable.class);
         executor.execute(task);
         verifyZeroInteractions(task);
     }

     @Test
     public void testPostShutdownExecute2() throws Exception {
         TaskExecutorServiceAsExecutorService executor = new TaskExecutorServiceAsExecutorService(
                new SyncTaskExecutor(), true);
         executor.shutdown();

         Runnable task = mock(Runnable.class);
         executor.execute(task);
         verifyZeroInteractions(task);
     }
}
