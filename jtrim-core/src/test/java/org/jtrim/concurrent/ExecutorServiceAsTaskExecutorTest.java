package org.jtrim.concurrent;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matcher;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class ExecutorServiceAsTaskExecutorTest {

    public ExecutorServiceAsTaskExecutorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        Thread.interrupted(); // clear interrupted status
    }

    @After
    public void tearDown() {
    }

    private static Matcher<CancellationToken> checkCanceled(final boolean canceled) {
        return new ArgumentMatcher<CancellationToken>() {
            @Override
            public boolean matches(Object argument) {
                return ((CancellationToken)argument).isCanceled() == canceled;
            }
        };
    }

    @Test(timeout = 5000)
    public void testSimpleExecute() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutor executor = new ExecutorServiceAsTaskExecutor(wrapped, false);

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(argThat(checkCanceled(false)));
        inOrder.verify(cleanup).cleanup(false, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 5000)
    public void testExecutePreCanceled() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutor executor = new ExecutorServiceAsTaskExecutor(wrapped, false);

            executor.execute(Cancellation.CANCELED_TOKEN, task, cleanup);
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        verifyZeroInteractions(task);
        verify(cleanup).cleanup(true, null);
        verifyNoMoreInteractions(cleanup);
    }

    @Test(timeout = 5000)
    public void testExecuteInterruptWhileRunning() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                cancelSource.getController().cancel();
                if (Thread.interrupted()) {
                    throw new OperationCanceledException();
                }
                return null;
            }
        }).when(task).execute(any(CancellationToken.class));

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutor executor = new ExecutorServiceAsTaskExecutor(wrapped, true);

            executor.execute(cancelSource.getToken(), task, cleanup);
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(any(CancellationToken.class));
        inOrder.verify(cleanup).cleanup(eq(true), isA(OperationCanceledException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 5000)
    public void testExecuteDontInterruptWhileRunning() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                cancelSource.getController().cancel();
                if (Thread.interrupted()) {
                    throw new OperationCanceledException();
                }
                return null;
            }
        }).when(task).execute(any(CancellationToken.class));

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutor executor = new ExecutorServiceAsTaskExecutor(wrapped, false);

            executor.execute(cancelSource.getToken(), task, cleanup);
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(any(CancellationToken.class));
        inOrder.verify(cleanup).cleanup(false, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 5000)
    public void testSimpleExecuteWithoutCleanup() throws Exception {
        CancelableTask task = mock(CancelableTask.class);

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutor executor = new ExecutorServiceAsTaskExecutor(wrapped, false);

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        verify(task).execute(argThat(checkCanceled(false)));
        verifyNoMoreInteractions(task);
    }

    @Test(timeout = 5000)
    public void testExecutePreCanceledWithoutCleanup() throws Exception {
        CancelableTask task = mock(CancelableTask.class);

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutor executor = new ExecutorServiceAsTaskExecutor(wrapped, false);

            executor.execute(Cancellation.CANCELED_TOKEN, task, null);
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        verifyZeroInteractions(task);
    }

    @Test(timeout = 5000)
    public void testExecuteInterruptWhileRunningWithoutCleanup() throws Exception {
        CancelableTask task = mock(CancelableTask.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                cancelSource.getController().cancel();
                if (Thread.interrupted()) {
                    throw new OperationCanceledException();
                }
                return null;
            }
        }).when(task).execute(any(CancellationToken.class));

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutor executor = new ExecutorServiceAsTaskExecutor(wrapped, true);

            executor.execute(cancelSource.getToken(), task, null);
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        verify(task).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(task);
    }

    @Test(timeout = 5000)
    public void testExecuteDontInterruptWhileRunningWithoutCleanup() throws Exception {
        CancelableTask task = mock(CancelableTask.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                cancelSource.getController().cancel();
                if (Thread.interrupted()) {
                    throw new OperationCanceledException();
                }
                return null;
            }
        }).when(task).execute(any(CancellationToken.class));

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutor executor = new ExecutorServiceAsTaskExecutor(wrapped, false);

            executor.execute(cancelSource.getToken(), task, null);
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        verify(task).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(task);
    }

    @Test(timeout = 10000)
    public void testExecuteWithExceptionWithoutCleanup() throws Exception {
        for (Class<? extends Throwable> exception: Arrays.asList(
                InterruptedException.class, Exception.class, Error.class)) {

            CancelableTask task = mock(CancelableTask.class);
            doThrow(exception).when(task).execute(any(CancellationToken.class));

            ExecutorService wrapped = Executors.newSingleThreadExecutor();
            try {
                ExecutorServiceAsTaskExecutor executor = new ExecutorServiceAsTaskExecutor(wrapped, false);

                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
            } finally {
                wrapped.shutdown();
                wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            }

            verify(task).execute(any(CancellationToken.class));
            verifyNoMoreInteractions(task);
        }
    }
}
