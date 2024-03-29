package org.jtrim2.executor;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.ListenerRef;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DelegatedTaskExecutorServiceTest {
    private static DelegatedTaskExecutorService create(TaskExecutorService wrapped) {
        return new DelegatedTaskExecutorService(wrapped);
    }

    @Test(expected = NullPointerException.class)
    public void testIllegalCreate() {
        create(null);
    }

    @Test
    public void testExecuteRunnable() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        Runnable task = mock(Runnable.class);

        create(wrapped).execute(task);

        verify(wrapped).execute(task);
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of submit method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testExecuteTask() {
        CompletableFuture<Object> result = new CompletableFuture<>();
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        doReturn(result)
                .when(wrapped)
                .execute(any(CancellationToken.class), any(CancelableTask.class));

        CancellationToken cancelToken = mock(CancellationToken.class);
        CancelableTask task = mock(CancelableTask.class);

        assertSame(result, create(wrapped).execute(cancelToken, task));
        verify(wrapped).execute(cancelToken, task);
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of submit method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testExecuteFunction() {
        CompletableFuture<Object> result = new CompletableFuture<>();
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        doReturn(result)
                .when(wrapped)
                .executeFunction(any(CancellationToken.class), (CancelableFunction<?>) any(CancelableFunction.class));

        CancellationToken cancelToken = mock(CancellationToken.class);
        CancelableFunction<?> function = mock(CancelableFunction.class);


        assertSame(result, create(wrapped).executeFunction(cancelToken, function));
        verify(wrapped).executeFunction(cancelToken, function);
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of shutdown method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testShutdown() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);

        create(wrapped).shutdown();
        verify(wrapped).shutdown();
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of shutdownAndCancel method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testShutdownAndCancel() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);

        create(wrapped).shutdownAndCancel();
        verify(wrapped).shutdownAndCancel();
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of isShutdown method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testIsShutdown() {
        for (Boolean result: Arrays.asList(false, true)) {
            TaskExecutorService wrapped = mock(TaskExecutorService.class);
            DelegatedTaskExecutorService executor = create(wrapped);

            when(wrapped.isShutdown()).thenReturn(result);

            assertEquals(result, executor.isShutdown());
            verify(wrapped).isShutdown();
            verifyNoMoreInteractions(wrapped);
        }
    }

    /**
     * Test of isTerminated method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testIsTerminated() {
        for (Boolean result: Arrays.asList(false, true)) {
            TaskExecutorService wrapped = mock(TaskExecutorService.class);
            DelegatedTaskExecutorService executor = create(wrapped);

            when(wrapped.isTerminated()).thenReturn(result);

            assertEquals(result, executor.isTerminated());
            verify(wrapped).isTerminated();
            verifyNoMoreInteractions(wrapped);
        }
    }

    /**
     * Test of addTerminateListener method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testAddTerminateListener() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        Runnable listener = mock(Runnable.class);
        ListenerRef result = mock(ListenerRef.class);

        Mockito.<Object>when(wrapped.addTerminateListener(listener))
                .thenReturn(result);

        assertSame(result, create(wrapped).addTerminateListener(listener));
        verify(wrapped).addTerminateListener(listener);
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of awaitTermination method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testAwaitTermination() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        CancellationToken cancelToken = mock(CancellationToken.class);

        create(wrapped).awaitTermination(cancelToken);
        verify(wrapped).awaitTermination(cancelToken);
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of tryAwaitTermination method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testTryAwaitTermination() {
        for (TimeUnit unit: TimeUnit.values()) {
            for (Boolean result: Arrays.asList(false, true)) {
                TaskExecutorService wrapped = mock(TaskExecutorService.class);
                CancellationToken cancelToken = mock(CancellationToken.class);
                long timeout = 5489365467L;

                Mockito.<Object>when(wrapped.tryAwaitTermination(cancelToken, timeout, unit))
                        .thenReturn(result);

                assertEquals(result, create(wrapped).tryAwaitTermination(cancelToken, timeout, unit));
                verify(wrapped).tryAwaitTermination(cancelToken, timeout, unit);
                verifyNoMoreInteractions(wrapped);
            }
        }
    }

    /**
     * Test of toString method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testToString() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        String result = "TEST-STR" + DelegatedTaskExecutorServiceTest.class.getName();

        when(wrapped.toString()).thenReturn(result);
        assertSame(result, create(wrapped).toString());
    }
}
