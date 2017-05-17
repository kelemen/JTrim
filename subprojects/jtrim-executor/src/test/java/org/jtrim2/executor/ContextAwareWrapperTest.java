package org.jtrim2.executor;

import java.util.concurrent.atomic.AtomicBoolean;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.testutils.executor.MockCleanup;
import org.jtrim2.testutils.executor.MockFunction;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class ContextAwareWrapperTest {
    private static ContextAwareWrapper create() {
        return new ContextAwareWrapper(SyncTaskExecutor.getSimpleExecutor());
    }

    @Test
    public void testShotcutOfSameContextExecutor() {
        TaskExecutor wrapped = SyncTaskExecutor.getSimpleExecutor();
        ContextAwareWrapper executor1 = new ContextAwareWrapper(wrapped);
        ContextAwareWrapper executor2 = executor1.sameContextExecutor(wrapped);
        assertSame(executor1, executor2);
    }

    @Test
    public void testContextAwareness1() {
        ContextAwareWrapper executor = create();

        AtomicBoolean inContextTask = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            inContextTask.set(executor.isExecutingInThis());
        });

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask.get());
    }

    @Test
    public void testContextAwareness2() {
        ContextAwareWrapper executor = create();

        AtomicBoolean inContextTask = new AtomicBoolean(false);

        executor.execute(() -> {
            inContextTask.set(executor.isExecutingInThis());
        });

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask.get());
    }

    @Test
    public void testContextAwareness3() {
        ContextAwareWrapper executor = create();

        AtomicBoolean inContextTask = new AtomicBoolean(false);

        executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, (cancelToken) -> {
            inContextTask.set(executor.isExecutingInThis());
            return null;
        });

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask.get());
    }

    @Test
    public void testRecursiveContextAwarenessInTask() {
        final ContextAwareWrapper executor = create();
        final ContextAwareWrapper sibling = executor.sameContextExecutor(new SyncTaskExecutor());

        final AtomicBoolean inContextTask1 = new AtomicBoolean(false);
        final AtomicBoolean inContextTask2 = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken subTaskCancelToken) -> {
                inContextTask1.set(executor.isExecutingInThis());
                inContextTask2.set(sibling.isExecutingInThis());
            });
        });

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask1.get());

        assertFalse(sibling.isExecutingInThis());
        assertTrue(inContextTask2.get());
    }

    @Test
    public void testExecutorUsesRightExecutor() {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(true);
        ContextAwareWrapper executor = new ContextAwareWrapper(wrapped);

        AtomicBoolean inContextTask1 = new AtomicBoolean(false);

        Runnable executed = mock(Runnable.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            inContextTask1.set(executor.isExecutingInThis());
            executed.run();
        });

        verifyZeroInteractions(executed);
        wrapped.executeCurrentlySubmitted();
        verify(executed).run();

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask1.get());
    }

    @Test
    public void testSameContextExecutorUsesRightExecutor() {
        ContextAwareWrapper executor = create();

        ManualTaskExecutor wrapped = new ManualTaskExecutor(true);
        ContextAwareWrapper sibling = executor.sameContextExecutor(wrapped);

        AtomicBoolean inContextTask1 = new AtomicBoolean(false);
        AtomicBoolean inContextTask2 = new AtomicBoolean(false);

        Runnable executed = mock(Runnable.class);
        sibling.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            inContextTask1.set(executor.isExecutingInThis());
            inContextTask2.set(sibling.isExecutingInThis());
            executed.run();
        });

        verifyZeroInteractions(executed);
        wrapped.executeCurrentlySubmitted();
        verify(executed).run();

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask1.get());

        assertFalse(sibling.isExecutingInThis());
        assertTrue(inContextTask2.get());
    }

    @Test
    public void testCleanupAfterSuccess() throws Exception {
        ContextAwareWrapper executor = create();

        Object result = "Test-result-43243245";
        MockFunction<Object> function = MockFunction.mock(result);

        MockCleanup cleanup = mock(MockCleanup.class);
        executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, MockFunction.toFunction(function))
                .whenComplete(MockCleanup.toCleanupTask(cleanup));

        verify(function).execute(false);
        verify(cleanup).cleanup(same(result), isNull(Throwable.class));
    }

    @Test
    public void testCleanupAfterFailure() throws Exception {
        final ContextAwareWrapper executor = create();

        CancelableTask task = mock(CancelableTask.class);
        TestException testException = new TestException();
        doThrow(testException).when(task).execute(any(CancellationToken.class));

        MockCleanup cleanup = mock(MockCleanup.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                .whenComplete(MockCleanup.toCleanupTask(cleanup));

        verify(task).execute(any(CancellationToken.class));
        verify(cleanup).cleanup(isNull(), same(testException));
    }

    @Test
    public void testContextAwarenessAfterFailedCleanup() throws Exception {
        final ContextAwareWrapper executor = create();

        CancelableTask task = mock(CancelableTask.class);
        doThrow(new TestException()).when(task).execute(any(CancellationToken.class));

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
        assertFalse(executor.isExecutingInThis());

        AtomicBoolean inContextTask = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            inContextTask.set(executor.isExecutingInThis());
        });

        assertFalse(executor.isExecutingInThis());
        assertTrue(inContextTask.get());
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
