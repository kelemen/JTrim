package org.jtrim2.executor;

import java.util.Arrays;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.testutils.executor.MockCleanup;
import org.jtrim2.testutils.executor.MockFunction;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class ManualTaskExecutorTest {
    private static ManualTaskExecutor lazyExecutor() {
        return new ManualTaskExecutor(false);
    }

    private static ManualTaskExecutor eagerExecutor() {
        return new ManualTaskExecutor(true);
    }

    @Test
    public void testTryExecuteOneEmpty() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            assertFalse(executor.tryExecuteOne());

            CancelableTask task = mock(CancelableTask.class);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
            assertTrue(executor.tryExecuteOne());
            verify(task).execute(any(CancellationToken.class));

            assertFalse(executor.tryExecuteOne());
            verifyNoMoreInteractions(task);
        }
    }

    @Test
    public void testTryExecuteOneFunction() throws Exception {
        int resultIndex = 0;
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            assertFalse(executor.tryExecuteOne());

            Object result = "test-result-45476475-" + resultIndex;
            resultIndex++;

            MockFunction<Object> function = MockFunction.mock(result);
            MockCleanup cleanup = mock(MockCleanup.class);

            executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, MockFunction.toFunction(function))
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));

            assertTrue(executor.tryExecuteOne());

            InOrder inOrder = inOrder(function, cleanup);
            inOrder.verify(function).execute(false);
            inOrder.verify(cleanup).cleanup(same(result), isNull(Throwable.class));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void testTryExecuteOne() throws Exception {
        for (int numberOfTasks = 1; numberOfTasks < 5; numberOfTasks++) {
            for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
                CancellationToken[] tokens = new CancellationToken[numberOfTasks];
                CancelableTask[] tasks = new CancelableTask[numberOfTasks];
                MockCleanup[] cleanups = new MockCleanup[numberOfTasks];

                for (int i = 0; i < numberOfTasks; i++) {
                    tokens[i] = Cancellation.createCancellationSource().getToken();
                    tasks[i] = mock(CancelableTask.class);
                    cleanups[i] = mock(MockCleanup.class);
                }

                for (int i = 0; i < numberOfTasks; i++) {
                    executor.execute(tokens[i], tasks[i])
                            .whenComplete(MockCleanup.toCleanupTask(cleanups[i]));
                }

                Object[] tasksAndCleanups = new Object[2 * numberOfTasks];
                for (int i = 0; i < numberOfTasks; i++) {
                    tasksAndCleanups[2 * i] = tasks[i];
                    tasksAndCleanups[2 * i + 1] = cleanups[i];
                }

                verifyZeroInteractions(tasksAndCleanups);

                for (int i = 0; i < numberOfTasks; i++) {
                    assertTrue(executor.tryExecuteOne());
                }

                InOrder inOrder = inOrder(tasksAndCleanups);
                for (int i = 0; i < numberOfTasks; i++) {
                    inOrder.verify(tasks[i]).execute(same(tokens[i]));
                    inOrder.verify(cleanups[i]).cleanup(null, null);
                }
                inOrder.verifyNoMoreInteractions();
            }
        }
    }

    @Test
    public void testTryExecuteOneNoCleanup() throws Exception {
        for (int numberOfTasks = 1; numberOfTasks < 5; numberOfTasks++) {
            for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
                CancellationToken[] tokens = new CancellationToken[numberOfTasks];
                CancelableTask[] tasks = new CancelableTask[numberOfTasks];

                for (int i = 0; i < numberOfTasks; i++) {
                    tokens[i] = Cancellation.createCancellationSource().getToken();
                    tasks[i] = mock(CancelableTask.class);
                }

                for (int i = 0; i < numberOfTasks; i++) {
                    executor.execute(tokens[i], tasks[i]);
                }

                verifyZeroInteractions((Object[])tasks);

                for (int i = 0; i < numberOfTasks; i++) {
                    assertTrue(executor.tryExecuteOne());
                }

                InOrder inOrder = inOrder((Object[])tasks);
                for (int i = 0; i < numberOfTasks; i++) {
                    inOrder.verify(tasks[i]).execute(same(tokens[i]));
                }
                inOrder.verifyNoMoreInteractions();
            }
        }
    }

    @Test
    public void testExecuteCurrentlySubmittedEmpty() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            assertEquals(0, executor.executeCurrentlySubmitted());

            CancelableTask task = mock(CancelableTask.class);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
            assertEquals(1, executor.executeCurrentlySubmitted());
            verify(task).execute(any(CancellationToken.class));

            assertEquals(0, executor.executeCurrentlySubmitted());
            verifyNoMoreInteractions(task);
        }
    }

    @Test
    public void testExecuteCurrentlySubmitted() throws Exception {
        for (int numberOfTasks = 1; numberOfTasks < 5; numberOfTasks++) {
            for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
                CancellationToken[] tokens = new CancellationToken[numberOfTasks];
                CancelableTask[] tasks = new CancelableTask[numberOfTasks];
                MockCleanup[] cleanups = new MockCleanup[numberOfTasks];

                for (int i = 0; i < numberOfTasks; i++) {
                    tokens[i] = Cancellation.createCancellationSource().getToken();
                    tasks[i] = mock(CancelableTask.class);
                    cleanups[i] = mock(MockCleanup.class);
                }

                for (int i = 0; i < numberOfTasks; i++) {
                    executor.execute(tokens[i], tasks[i])
                            .whenComplete(MockCleanup.toCleanupTask(cleanups[i]));
                }

                Object[] tasksAndCleanups = new Object[2 * numberOfTasks];
                for (int i = 0; i < numberOfTasks; i++) {
                    tasksAndCleanups[2 * i] = tasks[i];
                    tasksAndCleanups[2 * i + 1] = cleanups[i];
                }

                verifyZeroInteractions(tasksAndCleanups);

                assertEquals(numberOfTasks, executor.executeCurrentlySubmitted());

                InOrder inOrder = inOrder(tasksAndCleanups);
                for (int i = 0; i < numberOfTasks; i++) {
                    inOrder.verify(tasks[i]).execute(same(tokens[i]));
                    inOrder.verify(cleanups[i]).cleanup(null, null);
                }
                inOrder.verifyNoMoreInteractions();
            }
        }
    }

    @Test
    public void testExecuteCurrentlySubmittedNoCleanup() throws Exception {
        for (int numberOfTasks = 1; numberOfTasks < 5; numberOfTasks++) {
            for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
                CancellationToken[] tokens = new CancellationToken[numberOfTasks];
                CancelableTask[] tasks = new CancelableTask[numberOfTasks];

                for (int i = 0; i < numberOfTasks; i++) {
                    tokens[i] = Cancellation.createCancellationSource().getToken();
                    tasks[i] = mock(CancelableTask.class);
                }

                for (int i = 0; i < numberOfTasks; i++) {
                    executor.execute(tokens[i], tasks[i]);
                }

                verifyZeroInteractions((Object[])tasks);

                assertEquals(numberOfTasks, executor.executeCurrentlySubmitted());

                InOrder inOrder = inOrder((Object[])tasks);
                for (int i = 0; i < numberOfTasks; i++) {
                    inOrder.verify(tasks[i]).execute(same(tokens[i]));
                }
                inOrder.verifyNoMoreInteractions();
            }
        }
    }

    @Test
    public void testExecuteCurrentlySubmittedWithInternalTask() throws Exception {
        for (final ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask outerTask = mock(CancelableTask.class);
            final CancelableTask innerTask = mock(CancelableTask.class);

            doAnswer((InvocationOnMock invocation) -> {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, innerTask);
                return null;
            }).when(outerTask).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, outerTask);
            assertEquals(1, executor.executeCurrentlySubmitted());

            verify(outerTask).execute(any(CancellationToken.class));
            verifyZeroInteractions(innerTask);
        }
    }

    @Test
    public void testEagerExecuteCurrentlySubmitted() throws Exception {
        ManualTaskExecutor executor = eagerExecutor();

        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);
        CancellationSource cancelSource = Cancellation.createCancellationSource();

        executor.execute(cancelSource.getToken(), task)
                .whenComplete(MockCleanup.toCleanupTask(cleanup));
        cancelSource.getController().cancel();
        assertEquals(1, executor.executeCurrentlySubmitted());

        verifyZeroInteractions(task);
        verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
    }

    @Test
    public void testEagerExecuteCurrentlySubmittedNoCleanup() throws Exception {
        ManualTaskExecutor executor = eagerExecutor();

        CancelableTask task = mock(CancelableTask.class);
        CancellationSource cancelSource = Cancellation.createCancellationSource();

        executor.execute(cancelSource.getToken(), task);
        cancelSource.getController().cancel();
        assertEquals(1, executor.executeCurrentlySubmitted());

        verifyZeroInteractions(task);
    }

    @Test
    public void testEagerTryExecuteOne() throws Exception {
        ManualTaskExecutor executor = eagerExecutor();

        CancelableTask task = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);
        CancellationSource cancelSource = Cancellation.createCancellationSource();

        executor.execute(cancelSource.getToken(), task)
                .whenComplete(MockCleanup.toCleanupTask(cleanup));
        cancelSource.getController().cancel();
        assertTrue(executor.tryExecuteOne());

        verifyZeroInteractions(task);
        verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
    }

    @Test
    public void testEagerTryExecuteOneNoCleanup() throws Exception {
        ManualTaskExecutor executor = eagerExecutor();

        CancelableTask task = mock(CancelableTask.class);
        CancellationSource cancelSource = Cancellation.createCancellationSource();

        executor.execute(cancelSource.getToken(), task);
        cancelSource.getController().cancel();
        assertTrue(executor.tryExecuteOne());

        verifyZeroInteractions(task);
    }

    @Test
    public void testTryExecuteOneWithException() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);
            MockCleanup cleanup = mock(MockCleanup.class);

            Exception taskException = new RuntimeException();
            doThrow(taskException).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));
            assertTrue(executor.tryExecuteOne());

            verify(task).execute(any(CancellationToken.class));
            verify(cleanup).cleanup(isNull(), same(taskException));
        }
    }

    @Test
    public void testTryExecuteOneWithExceptionNoCleanup() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);

            Exception taskException = new RuntimeException();
            doThrow(taskException).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
            assertTrue(executor.tryExecuteOne());

            verify(task).execute(any(CancellationToken.class));
        }
    }

    @Test
    public void testExecuteCurrentlySubmittedWithException() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);
            MockCleanup cleanup = mock(MockCleanup.class);

            Exception taskException = new RuntimeException();
            doThrow(taskException).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));
            assertEquals(1, executor.executeCurrentlySubmitted());

            verify(task).execute(any(CancellationToken.class));
            verify(cleanup).cleanup(isNull(), same(taskException));
        }
    }

    @Test
    public void testExecuteCurrentlySubmittedWithExceptionNoCleanup() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);

            Exception taskException = new RuntimeException();
            doThrow(taskException).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
            assertEquals(1, executor.executeCurrentlySubmitted());

            verify(task).execute(any(CancellationToken.class));
        }
    }

    @Test
    public void testTryExecuteOneThrowsCancellation() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);
            MockCleanup cleanup = mock(MockCleanup.class);

            doThrow(OperationCanceledException.class).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));
            assertTrue(executor.tryExecuteOne());

            verify(task).execute(any(CancellationToken.class));
            verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
        }
    }

    @Test
    public void testExecuteCurrentlySubmittedThrowsCancellation() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);
            MockCleanup cleanup = mock(MockCleanup.class);

            doThrow(OperationCanceledException.class).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));
            assertEquals(1, executor.executeCurrentlySubmitted());

            verify(task).execute(any(CancellationToken.class));
            verify(cleanup).cleanup(isNull(), isA(OperationCanceledException.class));
        }
    }

@Test
    public void testLazyTryExecuteOne() throws Exception {
        ManualTaskExecutor executor = lazyExecutor();

        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
        assertTrue(executor.tryExecuteOne());

        verify(task).execute(any(CancellationToken.class));
    }

    @Test
    public void testLazyExecuteCurrentlySubmitted() throws Exception {
        ManualTaskExecutor executor = lazyExecutor();

        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
        assertEquals(1, executor.executeCurrentlySubmitted());

        verify(task).execute(any(CancellationToken.class));
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -805102970036494035L;
    }
}
