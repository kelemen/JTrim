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
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.logs.LogCollector;
import org.jtrim2.testutils.LogTests;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
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
            executor.execute(cancelToken, new AddToQueueTask(2, tasks), null);
            tasks.add(0);
            executor.execute(cancelToken, new AddToQueueTask(3, tasks), null);
        }, new AddToQueueCleanupTask(1, tasks));
        checkTaskList(tasks, 4);
    }

    @Test
    public void testSimpleCancellation() {
        InOrderTaskExecutor executor = createSyncExecutor();

        List<Integer> tasks = new LinkedList<>();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN,
                new AddToQueueTask(0, tasks), new AddToQueueCleanupTask(1, tasks));
        executor.execute(Cancellation.CANCELED_TOKEN,
                new AddToQueueTask(-1, tasks), new AddToQueueCleanupTask(2, tasks));
        executor.execute(Cancellation.UNCANCELABLE_TOKEN,
                new AddToQueueTask(3, tasks), new AddToQueueCleanupTask(4, tasks));
        executor.execute(Cancellation.CANCELED_TOKEN,
                new AddToQueueTask(-1, tasks), null);

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
        executor.execute(Cancellation.UNCANCELABLE_TOKEN,
                new AddToQueueTask(0, tasks), new AddToQueueCleanupTask(1, tasks));
        wrappedExecutor.shutdownAndCancel();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN,
                new AddToQueueTask(-1, tasks), new AddToQueueCleanupTask(2, tasks));
        executor.execute(Cancellation.UNCANCELABLE_TOKEN,
                new AddToQueueTask(-1, tasks), new AddToQueueCleanupTask(3, tasks));

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
            List<Map.Entry<CancelableTask, CleanupTask>> tasks
                    = Collections.synchronizedList(new LinkedList<Map.Entry<CancelableTask, CleanupTask>>());

            int taskIndex = 0;
            for (int i = 0; i < taskCount; i++) {
                CancelableTask task = new AddToQueueTask(taskIndex, executedTasks);
                taskIndex++;
                CleanupTask cleanupTask = new AddToQueueCleanupTask(taskIndex, executedTasks);
                taskIndex++;
                tasks.add(new AbstractMap.SimpleImmutableEntry<>(task, cleanupTask));
            }

            for (Map.Entry<CancelableTask, CleanupTask> task: tasks) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task.getKey(), task.getValue());
            }

            final WaitableSignal doneSignal = new WaitableSignal();
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                doneSignal.signal();
            }, null);

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
        }, null);

        assertTrue("ExecutingInThis", inContext.get());
    }

    @Test
    public void testContextAwarenessInCleanup() throws InterruptedException {
        TaskExecutorService wrappedExecutor = new SyncTaskExecutor();
        final InOrderTaskExecutor executor = new InOrderTaskExecutor(wrappedExecutor);

        final AtomicBoolean inContext = new AtomicBoolean();

        CancelableTask noop = CancelableTasks.noOpCancelableTask();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, noop, (boolean canceled, Throwable error) -> {
            inContext.set(executor.isExecutingInThis());
        });

        assertTrue("ExecutingInThis", inContext.get());
    }

    @Test
    public void testTaskThrowsException() throws Exception {
        TaskExecutorService wrappedExecutor = new SyncTaskExecutor();
        final InOrderTaskExecutor executor = new InOrderTaskExecutor(wrappedExecutor);

        CancelableTask task = mock(CancelableTask.class);

        doThrow(new TestException()).when(task).execute(any(CancellationToken.class));

        try (LogCollector logs = LogTests.startCollecting()) {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }

        verify(task).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(task);
    }

    @Test
    public void testTwoTasksThrowException() throws Exception {
        TaskExecutorService wrappedExecutor = new SyncTaskExecutor();
        final InOrderTaskExecutor executor = new InOrderTaskExecutor(wrappedExecutor);

        CancelableTask task = mock(CancelableTask.class);
        final CancelableTask subTask = mock(CancelableTask.class);

        doAnswer((InvocationOnMock invocation) -> {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, subTask, null);
            throw new TestException();
        }).when(task).execute(any(CancellationToken.class));

        doThrow(new TestException()).when(subTask).execute(any(CancellationToken.class));

        try (LogCollector logs = LogTests.startCollecting()) {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, null);
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 2, logs);
        }

        verify(task).execute(any(CancellationToken.class));
        verify(subTask).execute(any(CancellationToken.class));
        verifyNoMoreInteractions(task, subTask);
    }

    @Test
    public void testCleanupThrowsException() throws Exception {
        TaskExecutorService wrappedExecutor = new SyncTaskExecutor();
        final InOrderTaskExecutor executor = new InOrderTaskExecutor(wrappedExecutor);

        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);

        doThrow(new TestException()).when(cleanup).cleanup(anyBoolean(), any(Throwable.class));

        try (LogCollector logs = LogTests.startCollecting()) {
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1, cleanup);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2, null);
            LogTests.verifyLogCount(TestException.class, Level.SEVERE, 1, logs);
        }

        verify(task1).execute(any(CancellationToken.class));
        verify(task2).execute(any(CancellationToken.class));
        verify(cleanup).cleanup(anyBoolean(), any(Throwable.class));
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

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, mock(CancelableTask.class), null);

            numberOfExecutingTasks.add(executor.getNumberOfExecutingTasks());
            numberOfQueuedTasks.add(executor.getNumberOfQueuedTasks());
        }, null);

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

    private static class AddToQueueCleanupTask implements CleanupTask {
        private final int taskIndex;
        private final List<Integer> queue;

        public AddToQueueCleanupTask(int taskIndex, List<Integer> queue) {
            this.taskIndex = taskIndex;
            this.queue = queue;
        }

        @Override
        public void cleanup(boolean canceled, Throwable error) {
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
