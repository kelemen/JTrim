package org.jtrim2.executor;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matcher;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.testutils.executor.MockCleanup;
import org.jtrim2.testutils.executor.MockFunction;
import org.jtrim2.testutils.executor.MockTask;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class ExecutorServiceAsTaskExecutorTest {
    @Before
    public void setUp() {
        Thread.interrupted(); // clear interrupted status
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
        MockTask task = mock(MockTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutorService executor = new ExecutorServiceAsTaskExecutorService(wrapped, false);

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, MockTask.toTask(task))
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(false);
        inOrder.verify(cleanup).cleanup(null, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 5000)
    public void testSimpleExecuteFunction() throws Exception {
        Object result = "test-result-53443634";
        MockFunction<Object> function = MockFunction.mock(result);
        MockCleanup cleanup = mock(MockCleanup.class);

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutorService executor = new ExecutorServiceAsTaskExecutorService(wrapped, false);

            executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, MockFunction.toFunction(function))
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        InOrder inOrder = inOrder(function, cleanup);
        inOrder.verify(function).execute(false);
        inOrder.verify(cleanup).cleanup(same(result), isNull(Throwable.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 5000)
    public void testExecutePreCanceled() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutorService executor = new ExecutorServiceAsTaskExecutorService(wrapped, false);

            executor.execute(Cancellation.CANCELED_TOKEN, task)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        verifyZeroInteractions(task);
        verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
        verifyNoMoreInteractions(cleanup);
    }

    @Test(timeout = 5000)
    public void testExecuteInterruptWhileRunning() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        doAnswer((InvocationOnMock invocation) -> {
            cancelSource.getController().cancel();
            if (Thread.interrupted()) {
                throw new OperationCanceledException();
            }
            return null;
        }).when(task).execute(any(CancellationToken.class));

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutorService executor = new ExecutorServiceAsTaskExecutorService(wrapped, true);

            executor.execute(cancelSource.getToken(), task)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(any(CancellationToken.class));
        inOrder.verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 5000)
    public void testExecuteDontInterruptWhileRunning() throws Exception {
        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        final CancellationSource cancelSource = Cancellation.createCancellationSource();
        doAnswer((InvocationOnMock invocation) -> {
            cancelSource.getController().cancel();
            if (Thread.interrupted()) {
                throw new OperationCanceledException();
            }
            return null;
        }).when(task).execute(any(CancellationToken.class));

        ExecutorService wrapped = Executors.newSingleThreadExecutor();
        try {
            ExecutorServiceAsTaskExecutorService executor = new ExecutorServiceAsTaskExecutorService(wrapped, false);

            executor.execute(cancelSource.getToken(), task)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));
        } finally {
            wrapped.shutdown();
            wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        }

        InOrder inOrder = inOrder(task, cleanup);
        inOrder.verify(task).execute(any(CancellationToken.class));
        inOrder.verify(cleanup).cleanup(null, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 10000)
    public void testExecuteWithException() throws Exception {
        for (Class<? extends Throwable> exception: Arrays.asList(
                InterruptedException.class, Exception.class, Error.class)) {

            CancelableTask task = mock(CancelableTask.class);
            doThrow(exception).when(task).execute(any(CancellationToken.class));
            MockCleanup cleanup = mock(MockCleanup.class);

            ExecutorService wrapped = Executors.newSingleThreadExecutor();
            try {
                ExecutorServiceAsTaskExecutorService executor
                        = new ExecutorServiceAsTaskExecutorService(wrapped, false);

                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                        .whenComplete(MockCleanup.toCleanupTask(cleanup));
            } finally {
                wrapped.shutdown();
                wrapped.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            }

            InOrder inOrder = inOrder(task, cleanup);
            inOrder.verify(task).execute(any(CancellationToken.class));
            inOrder.verify(cleanup).cleanup(isNull(), isA(exception));
            inOrder.verifyNoMoreInteractions();
        }
    }
}
