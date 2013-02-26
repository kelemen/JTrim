package org.jtrim.concurrent;

import java.util.Arrays;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.cancel.OperationCanceledException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class ManualTaskExecutorTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

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
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
            assertTrue(executor.tryExecuteOne());
            verify(task).execute(any(CancellationToken.class));

            assertFalse(executor.tryExecuteOne());
            verifyNoMoreInteractions(task);
        }
    }

    @Test
    public void testTryExecuteOne() throws Exception {
        for (int numberOfTasks = 1; numberOfTasks < 5; numberOfTasks++) {
            for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
                CancellationToken[] tokens = new CancellationToken[numberOfTasks];
                CancelableTask[] tasks = new CancelableTask[numberOfTasks];
                CleanupTask[] cleanups = new CleanupTask[numberOfTasks];

                for (int i = 0; i < numberOfTasks; i++) {
                    tokens[i] = Cancellation.createCancellationSource().getToken();
                    tasks[i] = mock(CancelableTask.class);
                    cleanups[i] = mock(CleanupTask.class);
                }

                for (int i = 0; i < numberOfTasks; i++) {
                    executor.execute(tokens[i], tasks[i], cleanups[i]);
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
                    inOrder.verify(cleanups[i]).cleanup(false, null);
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
                    executor.execute(tokens[i], tasks[i], null);
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
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
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
                CleanupTask[] cleanups = new CleanupTask[numberOfTasks];

                for (int i = 0; i < numberOfTasks; i++) {
                    tokens[i] = Cancellation.createCancellationSource().getToken();
                    tasks[i] = mock(CancelableTask.class);
                    cleanups[i] = mock(CleanupTask.class);
                }

                for (int i = 0; i < numberOfTasks; i++) {
                    executor.execute(tokens[i], tasks[i], cleanups[i]);
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
                    inOrder.verify(cleanups[i]).cleanup(false, null);
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
                    executor.execute(tokens[i], tasks[i], null);
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

            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    executor.execute(Cancellation.UNCANCELABLE_TOKEN, innerTask, null);
                    return null;
                }
            }).when(outerTask).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, outerTask, null);
            assertEquals(1, executor.executeCurrentlySubmitted());

            verify(outerTask).execute(any(CancellationToken.class));
            verifyZeroInteractions(innerTask);
        }
    }

    @Test
    public void testEagerExecuteCurrentlySubmitted() throws Exception {
        ManualTaskExecutor executor = eagerExecutor();

        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        CancellationSource cancelSource = Cancellation.createCancellationSource();

        executor.execute(cancelSource.getToken(), task, cleanup);
        cancelSource.getController().cancel();
        assertEquals(1, executor.executeCurrentlySubmitted());

        verifyZeroInteractions(task);
        verify(cleanup).cleanup(true, null);
    }

    @Test
    public void testEagerExecuteCurrentlySubmittedNoCleanup() throws Exception {
        ManualTaskExecutor executor = eagerExecutor();

        CancelableTask task = mock(CancelableTask.class);
        CancellationSource cancelSource = Cancellation.createCancellationSource();

        executor.execute(cancelSource.getToken(), task, null);
        cancelSource.getController().cancel();
        assertEquals(1, executor.executeCurrentlySubmitted());

        verifyZeroInteractions(task);
    }

    @Test
    public void testEagerTryExecuteOne() throws Exception {
        ManualTaskExecutor executor = eagerExecutor();

        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        CancellationSource cancelSource = Cancellation.createCancellationSource();

        executor.execute(cancelSource.getToken(), task, cleanup);
        cancelSource.getController().cancel();
        assertTrue(executor.tryExecuteOne());

        verifyZeroInteractions(task);
        verify(cleanup).cleanup(true, null);
    }

    @Test
    public void testEagerTryExecuteOneNoCleanup() throws Exception {
        ManualTaskExecutor executor = eagerExecutor();

        CancelableTask task = mock(CancelableTask.class);
        CancellationSource cancelSource = Cancellation.createCancellationSource();

        executor.execute(cancelSource.getToken(), task, null);
        cancelSource.getController().cancel();
        assertTrue(executor.tryExecuteOne());

        verifyZeroInteractions(task);
    }

    @Test
    public void testTryExecuteOneWithException() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);

            Exception taskException = new RuntimeException();
            doThrow(taskException).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            assertTrue(executor.tryExecuteOne());

            verify(task).execute(any(CancellationToken.class));
            verify(cleanup).cleanup(eq(false), same(taskException));
        }
    }

    @Test
    public void testTryExecuteOneWithExceptionNoCleanup() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);

            Exception taskException = new RuntimeException();
            doThrow(taskException).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
            assertTrue(executor.tryExecuteOne());

            verify(task).execute(any(CancellationToken.class));
        }
    }

    @Test
    public void testTryExecuteOneWithExceptionInCleanup() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CleanupTask cleanup = mock(CleanupTask.class);

            TestException taskException = new TestException();
            doThrow(taskException).when(cleanup).cleanup(anyBoolean(), any(Throwable.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), cleanup);

            try {
                assertTrue(executor.tryExecuteOne());
                fail("Expected exception.");
            } catch (TaskExecutionException ex) {
                assertSame(taskException, ex.getCause());
            }

            verify(cleanup).cleanup(false, null);
        }
    }

    @Test
    public void testExecuteCurrentlySubmittedWithException() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);

            Exception taskException = new RuntimeException();
            doThrow(taskException).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            assertEquals(1, executor.executeCurrentlySubmitted());

            verify(task).execute(any(CancellationToken.class));
            verify(cleanup).cleanup(eq(false), same(taskException));
        }
    }

    @Test
    public void testExecuteCurrentlySubmittedWithExceptionNoCleanup() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);

            Exception taskException = new RuntimeException();
            doThrow(taskException).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
            assertEquals(1, executor.executeCurrentlySubmitted());

            verify(task).execute(any(CancellationToken.class));
        }
    }

    @Test
    public void testExecuteCurrentlySubmittedWithExceptionInCleanup() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CleanupTask cleanup = mock(CleanupTask.class);

            TestException taskException = new TestException();
            doThrow(taskException).when(cleanup).cleanup(anyBoolean(), any(Throwable.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), cleanup);

            try {
                assertEquals(1, executor.executeCurrentlySubmitted());
                fail("Expected exception.");
            } catch (TaskExecutionException ex) {
                assertSame(taskException, ex.getCause());
            }

            verify(cleanup).cleanup(false, null);
        }
    }

    @Test
    public void testExecuteCurrentlySubmittedWithMultipleExceptionInCleanup() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CleanupTask cleanup1 = mock(CleanupTask.class);
            CleanupTask cleanup2 = mock(CleanupTask.class);

            TestException task1Exception = new TestException();
            TestException task2Exception = new TestException();

            doThrow(task1Exception).when(cleanup1).cleanup(anyBoolean(), any(Throwable.class));
            doThrow(task2Exception).when(cleanup2).cleanup(anyBoolean(), any(Throwable.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), cleanup1);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, Tasks.noOpCancelableTask(), cleanup2);

            try {
                assertEquals(2, executor.executeCurrentlySubmitted());
                fail("Expected exception.");
            } catch (TaskExecutionException ex) {
                assertSame(task1Exception, ex.getCause());
                Throwable[] suppressed = ex.getSuppressed();
                assertEquals(1, suppressed.length);
                assertSame(task2Exception, suppressed[0]);
            }

            verify(cleanup1).cleanup(false, null);
            verify(cleanup2).cleanup(false, null);
        }
    }

    @Test
    public void testTryExecuteOneThrowsCancellation() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);

            doThrow(OperationCanceledException.class).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            assertTrue(executor.tryExecuteOne());

            verify(task).execute(any(CancellationToken.class));
            verify(cleanup).cleanup(eq(true), isA(OperationCanceledException.class));
        }
    }

    @Test
    public void testExecuteCurrentlySubmittedThrowsCancellation() throws Exception {
        for (ManualTaskExecutor executor: Arrays.asList(lazyExecutor(), eagerExecutor())) {
            CancelableTask task = mock(CancelableTask.class);
            CleanupTask cleanup = mock(CleanupTask.class);

            doThrow(OperationCanceledException.class).when(task).execute(any(CancellationToken.class));

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
            assertEquals(1, executor.executeCurrentlySubmitted());

            verify(task).execute(any(CancellationToken.class));
            verify(cleanup).cleanup(eq(true), isA(OperationCanceledException.class));
        }
    }

@Test
    public void testLazyTryExecuteOne() throws Exception {
        ManualTaskExecutor executor = lazyExecutor();

        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
        assertTrue(executor.tryExecuteOne());

        verify(task).execute(any(CancellationToken.class));
    }

    @Test
    public void testLazyExecuteCurrentlySubmitted() throws Exception {
        ManualTaskExecutor executor = lazyExecutor();

        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
        assertEquals(1, executor.executeCurrentlySubmitted());

        verify(task).execute(any(CancellationToken.class));
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = -805102970036494035L;
    }
}
