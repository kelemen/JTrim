package org.jtrim2.executor;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.jtrim2.cancel.CancellationToken;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FallbackExecutorTest {
    @SuppressWarnings("unchecked")
    private static <V> CancelableFunction<V> mockFunction() {
        return (CancelableFunction<V>) (CancelableFunction<?>) mock(CancelableFunction.class);
    }

    @SuppressWarnings("unchecked")
    private static <V> CompletionStage<V> mockStage() {
        return (CompletionStage<V>) (CompletionStage<?>) mock(CompletionStage.class);
    }

    @SuppressWarnings("unchecked")
    private static <V> CancelableFunction<V> anyFunction() {
        return (CancelableFunction<V>) (CancelableFunction<?>) any(CancelableFunction.class);
    }

    private static CancellationToken anyToken() {
        return any(CancellationToken.class);
    }

    @Test
    public void testFallbackExecuteFunction() {
        TestSetup test = new TestSetup();

        CancellationToken cancelToken = mock(CancellationToken.class);
        CancelableFunction<Object> task = mockFunction();
        CompletionStage<Object> fallbackResult = mockStage();

        when(test.main.executeFunction(anyToken(), anyFunction()))
                .thenThrow(test.fallbackException());

        when(test.fallback.executeFunction(anyToken(), anyFunction()))
                .thenReturn(fallbackResult);

        assertSame(fallbackResult, test.executor.executeFunction(cancelToken, task));

        verifyZeroInteractions(fallbackResult);
        test.verifyBoth((inOrder, executor) -> {
            inOrder.verify(executor).executeFunction(same(cancelToken), same(task));
        });
    }

    @Test
    public void testFallbackExecuteCancelable() {
        TestSetup test = new TestSetup();

        CancellationToken cancelToken = mock(CancellationToken.class);
        CancelableTask task = mock(CancelableTask.class);
        CompletionStage<Void> fallbackResult = mockStage();

        when(test.main.execute(anyToken(), any(CancelableTask.class)))
                .thenThrow(test.fallbackException());

        when(test.fallback.execute(anyToken(), any(CancelableTask.class)))
                .thenReturn(fallbackResult);

        assertSame(fallbackResult, test.executor.execute(cancelToken, task));

        verifyZeroInteractions(fallbackResult);
        test.verifyBoth((inOrder, executor) -> {
            inOrder.verify(executor).execute(same(cancelToken), same(task));
        });
    }

    @Test
    public void testFallbackExecuteStaged() {
        TestSetup test = new TestSetup();

        Runnable task = mock(Runnable.class);
        CompletionStage<Void> fallbackResult = mockStage();

        when(test.main.executeStaged(any(Runnable.class)))
                .thenThrow(test.fallbackException());

        when(test.fallback.executeStaged(any(Runnable.class)))
                .thenReturn(fallbackResult);

        assertSame(fallbackResult, test.executor.executeStaged(task));

        verifyZeroInteractions(fallbackResult);
        test.verifyBoth((inOrder, executor) -> {
            inOrder.verify(executor).executeStaged(same(task));
        });
    }

    @Test
    public void testFallbackExecuteBasic() {
        TestSetup test = new TestSetup();

        Runnable task = mock(Runnable.class);

        doThrow(test.fallbackException())
                .when(test.main)
                .execute(any(Runnable.class));

        test.executor.execute(task);

        test.verifyBoth((inOrder, executor) -> {
            inOrder.verify(executor).execute(same(task));
        });
    }

    @Test
    public void testWrongFallbackExecuteFunction() {
        TestSetup test = new TestSetup();

        CancellationToken cancelToken = mock(CancellationToken.class);
        CancelableFunction<Object> task = mockFunction();
        CompletionStage<Object> fallbackResult = mockStage();

        FallbackExecutor.FallbackException expectedException = test.wrongFallbackException();
        when(test.main.executeFunction(anyToken(), anyFunction()))
                .thenThrow(expectedException);

        when(test.fallback.executeFunction(anyToken(), anyFunction()))
                .thenReturn(fallbackResult);

        try {
            test.executor.executeFunction(cancelToken, task);
            fail("Expected exception");
        } catch (FallbackExecutor.FallbackException ex) {
            assertSame(expectedException, ex);
        }

        verifyZeroInteractions(fallbackResult);
        test.verifyMain(executor -> {
            executor.executeFunction(same(cancelToken), same(task));
        });
    }

    @Test
    public void testWrongFallbackExecuteCancelable() {
        TestSetup test = new TestSetup();

        CancellationToken cancelToken = mock(CancellationToken.class);
        CancelableTask task = mock(CancelableTask.class);
        CompletionStage<Void> fallbackResult = mockStage();

        FallbackExecutor.FallbackException expectedException = test.wrongFallbackException();
        when(test.main.execute(anyToken(), any(CancelableTask.class)))
                .thenThrow(expectedException);

        when(test.fallback.execute(anyToken(), any(CancelableTask.class)))
                .thenReturn(fallbackResult);

        try {
            test.executor.execute(cancelToken, task);
            fail("Expected exception");
        } catch (FallbackExecutor.FallbackException ex) {
            assertSame(expectedException, ex);
        }

        verifyZeroInteractions(fallbackResult);
        test.verifyMain(executor -> {
            executor.execute(same(cancelToken), same(task));
        });
    }

    @Test
    public void testWrongFallbackExecuteStaged() {
        TestSetup test = new TestSetup();

        Runnable task = mock(Runnable.class);
        CompletionStage<Void> fallbackResult = mockStage();

        FallbackExecutor.FallbackException expectedException = test.wrongFallbackException();
        when(test.main.executeStaged(any(Runnable.class)))
                .thenThrow(expectedException);

        when(test.fallback.executeStaged(any(Runnable.class)))
                .thenReturn(fallbackResult);

        try {
            test.executor.executeStaged(task);
            fail("Expected exception");
        } catch (FallbackExecutor.FallbackException ex) {
            assertSame(expectedException, ex);
        }

        verifyZeroInteractions(fallbackResult);
        test.verifyMain(executor -> {
            executor.executeStaged(same(task));
        });
    }

    @Test
    public void testWrongFallbackExecuteBasic() {
        TestSetup test = new TestSetup();

        Runnable task = mock(Runnable.class);

        FallbackExecutor.FallbackException expectedException = test.wrongFallbackException();
        doThrow(expectedException)
                .when(test.main)
                .execute(any(Runnable.class));

        try {
            test.executor.execute(task);
            fail("Expected exception");
        } catch (FallbackExecutor.FallbackException ex) {
            assertSame(expectedException, ex);
        }

        test.verifyMain(executor -> {
            executor.execute(same(task));
        });
    }

    @Test
    public void testNoFallbackExecuteFunction() {
        TestSetup test = new TestSetup();

        CancellationToken cancelToken = mock(CancellationToken.class);
        CancelableFunction<Object> task = mockFunction();
        CompletionStage<Object> fallbackResult = mockStage();

        when(test.main.executeFunction(anyToken(), anyFunction()))
                .thenReturn(fallbackResult);

        assertSame(fallbackResult, test.executor.executeFunction(cancelToken, task));

        verifyZeroInteractions(fallbackResult);
        test.verifyMain(executor -> {
            executor.executeFunction(same(cancelToken), same(task));
        });
    }

    @Test
    public void testNoFallbackExecuteCancelable() {
        TestSetup test = new TestSetup();

        CancellationToken cancelToken = mock(CancellationToken.class);
        CancelableTask task = mock(CancelableTask.class);
        CompletionStage<Void> fallbackResult = mockStage();

        when(test.main.execute(anyToken(), any(CancelableTask.class)))
                .thenReturn(fallbackResult);

        assertSame(fallbackResult, test.executor.execute(cancelToken, task));

        verifyZeroInteractions(fallbackResult);
        test.verifyMain(executor -> {
            executor.execute(same(cancelToken), same(task));
        });
    }

    @Test
    public void testNoFallbackExecuteStaged() {
        TestSetup test = new TestSetup();

        Runnable task = mock(Runnable.class);
        CompletionStage<Void> fallbackResult = mockStage();

        when(test.main.executeStaged(any(Runnable.class)))
                .thenReturn(fallbackResult);

        assertSame(fallbackResult, test.executor.executeStaged(task));

        verifyZeroInteractions(fallbackResult);
        test.verifyMain(executor -> {
            executor.executeStaged(same(task));
        });
    }

    @Test
    public void testNoFallbackExecuteBasic() {
        TestSetup test = new TestSetup();

        Runnable task = mock(Runnable.class);

        test.executor.execute(task);

        test.verifyMain(executor -> {
            executor.execute(same(task));
        });
    }

    private void testDelegateIsExecutingInThis(boolean result) {
        TestSetup test = new TestSetup();

        when(test.main.isExecutingInThis()).thenReturn(result);

        assertEquals(result, test.executor.isExecutingInThis());
        test.verifyMain(MonitorableTaskExecutorService::isExecutingInThis);
    }

    @Test
    public void testDelegateIsExecutingInThis() {
        testDelegateIsExecutingInThis(false);
        testDelegateIsExecutingInThis(true);
    }

    private void testDelegateGetNumberOfQueuedTasks(long result) {
        TestSetup test = new TestSetup();

        when(test.main.getNumberOfQueuedTasks()).thenReturn(result);

        assertEquals(result, test.executor.getNumberOfQueuedTasks());
        test.verifyMain(MonitorableTaskExecutorService::getNumberOfQueuedTasks);
    }

    @Test
    public void testDelegateGetNumberOfQueuedTasks() {
        testDelegateGetNumberOfQueuedTasks(1);
        testDelegateGetNumberOfQueuedTasks(9);
    }

    private void testDelegateGetNumberOfExecutingTasks(long result) {
        TestSetup test = new TestSetup();

        when(test.main.getNumberOfExecutingTasks()).thenReturn(result);

        assertEquals(result, test.executor.getNumberOfExecutingTasks());
        test.verifyMain(MonitorableTaskExecutorService::getNumberOfExecutingTasks);
    }

    @Test
    public void testDelegateGetNumberOfExecutingTasks() {
        testDelegateGetNumberOfExecutingTasks(2);
        testDelegateGetNumberOfExecutingTasks(7);
    }

    private static final class TestSetup {
        private final MonitorableTaskExecutorService main;
        private final TaskExecutor fallback;
        private final FallbackExecutor executor;

        private TestSetup() {
            this.main = mock(MonitorableTaskExecutorService.class);
            this.fallback = mock(TaskExecutor.class);
            this.executor = new FallbackExecutor(main);
        }

        public FallbackExecutor.FallbackException fallbackException() {
            return new FallbackExecutor.FallbackException(main, fallback);
        }

        public FallbackExecutor.FallbackException wrongFallbackException() {
            return new FallbackExecutor.FallbackException(fallback, fallback);
        }

        public void verifyBoth(BiConsumer<? super InOrder, ? super TaskExecutor> verification) {
            InOrder inOrder = inOrder(main, fallback);
            verification.accept(inOrder, main);
            verification.accept(inOrder, fallback);
            inOrder.verifyNoMoreInteractions();
        }

        public void verifyMain(Consumer<? super MonitorableTaskExecutorService> verification) {
            verification.accept(verify(main));
            verifyZeroInteractions(fallback);
        }
    }
}
