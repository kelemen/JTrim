package org.jtrim2.executor;

import java.util.Arrays;
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

    /**
     * Test of submit method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testSubmitTask() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        CancellationToken cancelToken = mock(CancellationToken.class);
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        TaskFuture<?> result = mock(TaskFuture.class);

        Mockito.<Object>stub(wrapped.submit(cancelToken, task, cleanup))
                .toReturn(result);

        assertSame(result, create(wrapped).submit(cancelToken, task, cleanup));
        verify(wrapped).submit(cancelToken, task, cleanup);
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of submit method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testSubmitFunction() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        CancellationToken cancelToken = mock(CancellationToken.class);
        CancelableFunction<?> function = mock(CancelableFunction.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        TaskFuture<?> result = mock(TaskFuture.class);

        Mockito.<Object>stub(wrapped.submit(cancelToken, function, cleanup))
                .toReturn(result);

        assertSame(result, create(wrapped).submit(cancelToken, function, cleanup));
        verify(wrapped).submit(cancelToken, function, cleanup);
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

            stub(wrapped.isShutdown()).toReturn(result);

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

            stub(wrapped.isTerminated()).toReturn(result);

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

        Mockito.<Object>stub(wrapped.addTerminateListener(listener))
                .toReturn(result);

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

                Mockito.<Object>stub(wrapped.tryAwaitTermination(cancelToken, timeout, unit))
                        .toReturn(result);

                assertEquals(result, create(wrapped).tryAwaitTermination(cancelToken, timeout, unit));
                verify(wrapped).tryAwaitTermination(cancelToken, timeout, unit);
                verifyNoMoreInteractions(wrapped);
            }
        }
    }

    /**
     * Test of execute method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testExecute() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        CancellationToken cancelToken = mock(CancellationToken.class);
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        create(wrapped).execute(cancelToken, task, cleanup);
        verify(wrapped).execute(cancelToken, task, cleanup);
        verifyNoMoreInteractions(wrapped);
    }

    /**
     * Test of toString method, of class DelegatedTaskExecutorService.
     */
    @Test
    public void testToString() {
        TaskExecutorService wrapped = mock(TaskExecutorService.class);
        String result = "TEST-STR" + DelegatedTaskExecutorServiceTest.class.getName();

        stub(wrapped.toString()).toReturn(result);
        assertSame(result, create(wrapped).toString());
    }
}
