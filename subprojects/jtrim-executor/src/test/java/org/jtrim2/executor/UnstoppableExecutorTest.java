package org.jtrim2.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UnstoppableExecutorTest {
    private static UnstoppableExecutor create(ExecutorService executor) {
        return new UnstoppableExecutor(executor);
    }

    /**
     * Test of execute method, of class UnstoppableExecutor.
     */
    @Test
    public void testExecute() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        Runnable task = mock(Runnable.class);

        create(subExecutor).execute(task);
        verify(subExecutor).execute(task);

        verifyNoInteractions(task);
        verifyNoMoreInteractions(subExecutor);
    }

    /**
     * Test of submit method, of class UnstoppableExecutor.
     */
    @Test
    public void testSubmit_Runnable() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        Runnable task = mock(Runnable.class);
        Future<?> future = mock(Future.class);

        Mockito.<Object>when(subExecutor.submit(any(Runnable.class))).thenReturn(future);

        assertSame(future, create(subExecutor).submit(task));
        verify(subExecutor).submit(task);

        verifyNoInteractions(task);
        verifyNoMoreInteractions(subExecutor);
    }

    /**
     * Test of submit method, of class UnstoppableExecutor.
     */
    @Test
    public void testSubmit_Runnable_GenericType() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        Runnable task = mock(Runnable.class);
        Future<?> future = mock(Future.class);
        Object result = new Object();

        Mockito.<Object>when(subExecutor.submit(any(Runnable.class), any())).thenReturn(future);

        assertSame(future, create(subExecutor).submit(task, result));

        verify(subExecutor).submit(task, result);

        verifyNoInteractions(task);
        verifyNoMoreInteractions(subExecutor);
    }

    /**
     * Test of submit method, of class UnstoppableExecutor.
     */
    @Test
    public void testSubmit_Callable() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        Callable<?> task = mock(Callable.class);
        Future<?> future = mock(Future.class);

        Mockito.<Object>when(subExecutor.submit((Callable<?>) any(Callable.class)))
                .thenReturn(future);

        assertSame(future, create(subExecutor).submit(task));
        verify(subExecutor).submit(task);

        verifyNoInteractions(task);
        verifyNoMoreInteractions(subExecutor);
    }

    /**
     * Test of shutdownNow method, of class UnstoppableExecutor.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testShutdownNow() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        try {
            create(subExecutor).shutdownNow();
        } finally {
            verifyNoInteractions(subExecutor);
        }
    }

    /**
     * Test of shutdown method, of class UnstoppableExecutor.
     */
    @Test(expected = UnsupportedOperationException.class)
    public void testShutdown() {
        ExecutorService subExecutor = mock(ExecutorService.class);
        try {
            create(subExecutor).shutdown();
        } finally {
            verifyNoInteractions(subExecutor);
        }
    }

    /**
     * Test of isTerminated method, of class UnstoppableExecutor.
     */
    @Test
    public void testIsTerminated() {
        for (Boolean result: Arrays.asList(false, true)) {
            ExecutorService subExecutor = mock(ExecutorService.class);

            when(subExecutor.isTerminated()).thenReturn(result);

            assertEquals(result, create(subExecutor).isTerminated());
            verify(subExecutor).isTerminated();
            verifyNoMoreInteractions(subExecutor);
        }
    }

    /**
     * Test of isShutdown method, of class UnstoppableExecutor.
     */
    @Test
    public void testIsShutdown() {
        for (Boolean result: Arrays.asList(false, true)) {
            ExecutorService subExecutor = mock(ExecutorService.class);

            when(subExecutor.isShutdown()).thenReturn(result);

            assertEquals(result, create(subExecutor).isShutdown());
            verify(subExecutor).isShutdown();
            verifyNoMoreInteractions(subExecutor);
        }
    }

    @Test
    public void testInvokeAny_3args() throws Exception {
        ExecutorService subExecutor = mock(ExecutorService.class);

        @SuppressWarnings("unchecked")
        Collection<Callable<Object>> tasks = mock(Collection.class);
        long timeout = 54645375432L;
        TimeUnit unit = TimeUnit.MINUTES;
        Object result = new Object();

        Mockito.<Object>when(subExecutor.invokeAny(same(tasks), anyLong(), any(TimeUnit.class)))
                .thenReturn(result);

        assertSame(result, create(subExecutor).invokeAny(tasks, timeout, unit));

        verify(subExecutor).invokeAny(tasks, timeout, unit);
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testInvokeAny_Collection() throws Exception {
        ExecutorService subExecutor = mock(ExecutorService.class);

        @SuppressWarnings("unchecked")
        Collection<Callable<Object>> tasks = mock(Collection.class);
        Object result = new Object();

        Mockito.<Object>when(subExecutor.invokeAny(tasks))
                .thenReturn(result);

        assertSame(result, create(subExecutor).invokeAny(tasks));

        verify(subExecutor).invokeAny(tasks);
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testInvokeAll_3args() throws Exception {
        ExecutorService subExecutor = mock(ExecutorService.class);

        @SuppressWarnings("unchecked")
        Collection<Callable<Object>> tasks = mock(Collection.class);
        long timeout = 54645375432L;
        TimeUnit unit = TimeUnit.MINUTES;

        List<Future<Object>> result = new ArrayList<>();

        Mockito.<Object>when(subExecutor.invokeAll(same(tasks), anyLong(), any(TimeUnit.class)))
                .thenReturn(result);

        assertSame(result, create(subExecutor).invokeAll(tasks, timeout, unit));

        verify(subExecutor).invokeAll(tasks, timeout, unit);
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testInvokeAll_Collection() throws Exception {
        ExecutorService subExecutor = mock(ExecutorService.class);

        @SuppressWarnings("unchecked")
        Collection<Callable<Object>> tasks = mock(Collection.class);

        List<Future<Object>> result = new ArrayList<>();

        Mockito.<Object>when(subExecutor.invokeAll(tasks))
                .thenReturn(result);

        assertSame(result, create(subExecutor).invokeAll(tasks));

        verify(subExecutor).invokeAll(tasks);
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testAwaitTermination() throws Exception {
        for (Boolean result: Arrays.asList(false, true)) {
            ExecutorService subExecutor = mock(ExecutorService.class);

            long timeout = 54645375432L;
            TimeUnit unit = TimeUnit.MINUTES;

            when(subExecutor.awaitTermination(anyLong(), any(TimeUnit.class)))
                    .thenReturn(result);

            assertEquals(result, create(subExecutor).awaitTermination(timeout, unit));

            verify(subExecutor).awaitTermination(timeout, unit);
            verifyNoMoreInteractions(subExecutor);
        }
    }

    /**
     * Test of toString method, of class UnstoppableExecutor.
     */
    @Test
    public void testToString() {
        ExecutorService subExecutor = mock(ExecutorService.class);

        when(subExecutor.toString()).thenReturn("STR-VALUE");

        assertNotNull(create(subExecutor).toString());
    }
}
