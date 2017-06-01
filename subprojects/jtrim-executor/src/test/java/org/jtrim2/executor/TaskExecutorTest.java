package org.jtrim2.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class TaskExecutorTest {
    @Test
    public void testExecuteCancelableNullTask() throws Exception {
        TestTaskExecutor executor = new TestTaskExecutor();

        try {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, null);
        } catch (NullPointerException ex) {
            return;
        }

        fail("Expected NullPointerException");
    }

    @Test
    public void testExecuteStagedNullTask() throws Exception {
        TestTaskExecutor executor = new TestTaskExecutor();

        try {
            executor.executeStaged(null);
        } catch (NullPointerException ex) {
            return;
        }

        fail("Expected NullPointerException");
    }

    @Test
    public void testExecuteNullTask() throws Exception {
        TestTaskExecutor executor = new TestTaskExecutor();

        try {
            executor.execute(null);
        } catch (NullPointerException ex) {
            return;
        }

        fail("Expected NullPointerException");
    }

    @Test
    public void testExecuteCancelable() throws Exception {
        TestTaskExecutor executor = new TestTaskExecutor();

        CancellationToken cancelToken = Cancellation.createCancellationSource().getToken();
        CancelableTask task = mock(CancelableTask.class);

        CompletionStage<Void> future = executor.execute(cancelToken, task);

        TestSubmittedTask<?> submitted = executor.expectSingleTask();
        assertSame(submitted.future, future);
        assertSame(cancelToken, submitted.cancelToken);
        verifyZeroInteractions(task);

        Object result = submitted.function.execute(cancelToken);
        assertNull("result", result);

        verify(task).execute(same(cancelToken));
    }

    @Test
    public void testExecuteStaged() throws Exception {
        TestTaskExecutor executor = new TestTaskExecutor();

        Runnable task = mock(Runnable.class);

        CompletionStage<Void> future = executor.executeStaged(task);

        TestSubmittedTask<?> submitted = executor.expectSingleTask();
        assertSame(submitted.future, future);
        assertSame(Cancellation.UNCANCELABLE_TOKEN, submitted.cancelToken);
        verifyZeroInteractions(task);

        Object result = submitted.function.execute(Cancellation.CANCELED_TOKEN);
        assertNull("result", result);

        verify(task).run();
    }

    @Test
    public void testExecute() throws Exception {
        TestTaskExecutor executor = new TestTaskExecutor();

        Runnable task = mock(Runnable.class);

        executor.execute(task);

        TestSubmittedTask<?> submitted = executor.expectSingleTask();
        assertSame(Cancellation.UNCANCELABLE_TOKEN, submitted.cancelToken);
        verifyZeroInteractions(task);

        Object result = submitted.function.execute(Cancellation.CANCELED_TOKEN);
        assertNull("result", result);

        verify(task).run();
    }

    private static class TestTaskExecutor implements TaskExecutor {
        private final List<TestSubmittedTask<?>> submittedTasks;

        public TestTaskExecutor() {
            this.submittedTasks = new ArrayList<>();
        }

        public <V> CompletionStage<V> executeFunction(
                CancellationToken cancelToken,
                CancelableFunction<? extends V> function) {
            TestSubmittedTask<V> submittedTask = new TestSubmittedTask<>(cancelToken, function);
            submittedTasks.add(submittedTask);
            return submittedTask.future;
        }

        public List<TestSubmittedTask<?>> getSubmittedTasks() {
            return new ArrayList<>(submittedTasks);
        }

        public TestSubmittedTask<?> expectSingleTask() {
            assertEquals("submittedTasks.size()", 1, submittedTasks.size());
            return submittedTasks.get(0);
        }
    }

    private static final class TestSubmittedTask<V> {
        private final CancellationToken cancelToken;
        private final CancelableFunction<? extends V> function;
        private final CompletableFuture<V> future;

        public TestSubmittedTask(CancellationToken cancelToken, CancelableFunction<? extends V> function) {
            this.cancelToken = cancelToken;
            this.function = function;
            this.future = new CompletableFuture<>();
        }
    }
}
