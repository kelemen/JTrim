package org.jtrim2.executor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.testutils.executor.MockCleanup;
import org.jtrim2.testutils.executor.MockFunction;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class InOrderTaskExecutorTest {
    private static void checkTaskList(List<Integer> list, int expectedSize) {
        assertEquals("Unexpected executed tasks count.", expectedSize, list.size());

        Integer prev = null;
        for (Integer task: list) {
            if (prev != null && task <= prev) {
                fail("Invalid task order: " + list);
            }
            prev = task;
        }
    }

    private static <E> void checkForAll(Collection<E> elements, ParameterizedTask<E> checkTask) {
        for (E element: elements) {
            checkTask.execute(element);
        }
    }

    private static InOrderTaskExecutor createSyncExecutor() {
        return new InOrderTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
    }

    @Test
    public void testRecursiveExecute() {
        final InOrderTaskExecutor executor = createSyncExecutor();

        final List<Integer> tasks = new LinkedList<>();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            executor.execute(cancelToken, new AddToQueueTask(1, tasks));
            tasks.add(0);
            executor.execute(cancelToken, new AddToQueueTask(2, tasks));
        }).whenComplete(new AddToQueueCleanupTask<>(3, tasks));
        checkTaskList(tasks, 4);
    }

    @Test
    public void testSimpleCancellation() {
        InOrderTaskExecutor executor = createSyncExecutor();

        List<Integer> tasks = new LinkedList<>();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new AddToQueueTask(0, tasks))
                .whenComplete(new AddToQueueCleanupTask<>(1, tasks));
        executor.execute(Cancellation.CANCELED_TOKEN, new AddToQueueTask(-1, tasks))
                .whenComplete(new AddToQueueCleanupTask<>(2, tasks));
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new AddToQueueTask(3, tasks))
                .whenComplete(new AddToQueueCleanupTask<>(4, tasks));
        executor.execute(Cancellation.CANCELED_TOKEN, new AddToQueueTask(-1, tasks));

        checkForAll(tasks, (Integer arg) -> {
            assertTrue("Task should have been canceled.", arg >= 0);
        });

        checkTaskList(tasks, 5);
    }

    @Test
    public void testSimpleShutdown() {
        TaskExecutorService wrappedExecutor = new SyncTaskExecutor();
        InOrderTaskExecutor executor = new InOrderTaskExecutor(wrappedExecutor);

        List<Integer> tasks = new LinkedList<>();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new AddToQueueTask(0, tasks))
                .whenComplete(new AddToQueueCleanupTask<>(1, tasks));
        wrappedExecutor.shutdownAndCancel();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new AddToQueueTask(-1, tasks))
                .whenComplete(new AddToQueueCleanupTask<>(2, tasks));
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new AddToQueueTask(-1, tasks))
                .whenComplete(new AddToQueueCleanupTask<>(3, tasks));

        checkForAll(tasks, (Integer arg) -> {
            assertTrue("Task should have been canceled.", arg >= 0);
        });

        checkTaskList(tasks, 4);
    }

    @Test
    public void testConcurrentTasks() {
        final int concurrencyLevel = 4;
        final int taskCount = 100;

        TaskExecutorService wrappedExecutor = new ThreadPoolTaskExecutor(
                "InOrderTaskExecutorTest executor", concurrencyLevel);
        try {
            InOrderTaskExecutor executor = new InOrderTaskExecutor(wrappedExecutor);

            List<Integer> executedTasks = new LinkedList<>();
            List<Map.Entry<CancelableTask, AddToQueueCleanupTask<Void>>> tasks
                    = Collections.synchronizedList(new ArrayList<>());

            int taskIndex = 0;
            for (int i = 0; i < taskCount; i++) {
                CancelableTask task = new AddToQueueTask(taskIndex, executedTasks);
                taskIndex++;
                AddToQueueCleanupTask<Void> cleanupTask = new AddToQueueCleanupTask<>(taskIndex, executedTasks);
                taskIndex++;
                tasks.add(new AbstractMap.SimpleImmutableEntry<>(task, cleanupTask));
            }

            for (Map.Entry<CancelableTask, AddToQueueCleanupTask<Void>> task: tasks) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task.getKey())
                        .whenComplete(task.getValue());
            }

            final WaitableSignal doneSignal = new WaitableSignal();
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                doneSignal.signal();
            });

            assertTrue(doneSignal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, 10000, TimeUnit.MILLISECONDS));

            checkTaskList(executedTasks, taskIndex);
        } finally {
            wrappedExecutor.shutdown();
        }
    }

    @Test
    public void testContextAwarenessInTask() throws InterruptedException {
        TaskExecutorService wrappedExecutor = new SyncTaskExecutor();
        final InOrderTaskExecutor executor = new InOrderTaskExecutor(wrappedExecutor);
        assertFalse("ExecutingInThis", executor.isExecutingInThis());

        final AtomicBoolean inContext = new AtomicBoolean();

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            inContext.set(executor.isExecutingInThis());
        });

        assertTrue("ExecutingInThis", inContext.get());
    }

    @Test
    public void testTaskThrowsException() throws Exception {
        TaskExecutorService wrappedExecutor = new SyncTaskExecutor();
        final InOrderTaskExecutor executor = new InOrderTaskExecutor(wrappedExecutor);

        Object result = "test-result-543643654";
        MockFunction<Object> function = MockFunction.mock(result);
        TestException error = new TestException();
        doThrow(error).when(function).execute(anyBoolean());

        MockCleanup cleanup = mock(MockCleanup.class);

        executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, MockFunction.toFunction(function))
                .whenComplete(MockCleanup.toCleanupTask(cleanup));

        InOrder inOrder = inOrder(function, cleanup);
        inOrder.verify(function).execute(false);
        inOrder.verify(cleanup).cleanup(isNull(), same(error));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTwoTasksThrowException() throws Exception {
        TaskExecutorService wrappedExecutor = new SyncTaskExecutor();
        InOrderTaskExecutor executor = new InOrderTaskExecutor(wrappedExecutor);

        TestException error = new TestException();
        CancelableFunction<Object> subFunction = (cancelToken) -> {
            throw error;
        };

        MockCleanup cleanup = mock(MockCleanup.class);

        Runnable parentTaskRun = mock(Runnable.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (cancelToken) -> {
            executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, subFunction)
                    .whenComplete(MockCleanup.toCleanupTask(cleanup));
            parentTaskRun.run();
        });

        InOrder inOrder = inOrder(parentTaskRun, cleanup);
        inOrder.verify(parentTaskRun).run();
        inOrder.verify(cleanup).cleanup(isNull(), same(error));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testCleanupThrowsException() throws Exception {
        TaskExecutorService wrappedExecutor = new SyncTaskExecutor();
        final InOrderTaskExecutor executor = new InOrderTaskExecutor(wrappedExecutor);

        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);
        MockCleanup cleanup = mock(MockCleanup.class);

        doThrow(new TestException()).when(cleanup).cleanup(anyBoolean(), any(Throwable.class));

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1)
                .whenComplete(MockCleanup.toCleanupTask(cleanup));
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2);

        verify(task1).execute(any(CancellationToken.class));
        verify(task2).execute(any(CancellationToken.class));
        verify(cleanup).cleanup(isNull(), isNull(Throwable.class));
        verifyNoMoreInteractions(task1, task2, cleanup);
    }

    @Test
    public void testMonitoredValues() throws Exception {
        TaskExecutorService wrappedExecutor = new SyncTaskExecutor();
        final InOrderTaskExecutor executor = new InOrderTaskExecutor(wrappedExecutor);

        assertEquals(0L, executor.getNumberOfExecutingTasks());
        assertEquals(0L, executor.getNumberOfQueuedTasks());

        final List<Long> numberOfExecutingTasks = new ArrayList<>(2);
        final List<Long> numberOfQueuedTasks = new ArrayList<>(2);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            numberOfExecutingTasks.add(executor.getNumberOfExecutingTasks());
            numberOfQueuedTasks.add(executor.getNumberOfQueuedTasks());

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, mock(CancelableTask.class));

            numberOfExecutingTasks.add(executor.getNumberOfExecutingTasks());
            numberOfQueuedTasks.add(executor.getNumberOfQueuedTasks());
        });

        assertEquals(Arrays.asList(1L, 1L), numberOfExecutingTasks);
        assertEquals(Arrays.asList(0L, 1L), numberOfQueuedTasks);

        assertEquals(0L, executor.getNumberOfExecutingTasks());
        assertEquals(0L, executor.getNumberOfQueuedTasks());
    }

    private static class AddToQueueTask implements CancelableTask {
        private final int taskIndex;
        private final List<Integer> queue;

        public AddToQueueTask(int taskIndex, List<Integer> queue) {
            this.taskIndex = taskIndex;
            this.queue = queue;
        }

        @Override
        public void execute(CancellationToken cancelToken) {
            queue.add(taskIndex);
        }
    }

    private static class AddToQueueCleanupTask<V> implements BiConsumer<V, Throwable> {
        private final int taskIndex;
        private final List<Integer> queue;

        public AddToQueueCleanupTask(int taskIndex, List<Integer> queue) {
            this.taskIndex = taskIndex;
            this.queue = queue;
        }

        @Override
        public void accept(V result, Throwable error) {
            queue.add(taskIndex);
        }
    }

    private static interface ParameterizedTask<ArgType> {
        public void execute(ArgType arg);
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 6038646201346761782L;
    }
}
