package org.jtrim.concurrent;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class InOrderTaskExecutorTest {

    public InOrderTaskExecutorTest() {
    }

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

    private static void checkTaskList(List<Integer> list) {
        Integer prev = null;
        for (Integer task: list) {
            if (prev != null && task <= prev) {
                fail("Invalid task order: " + list);
            }
        }
    }

    private static <E> void checkForAll(Collection<E> elements, ParameterizedTask<E> checkTask) {
        for (E element: elements) {
            checkTask.execute(element);
        }
    }

    private static TaskExecutor createSyncExecutor() {
        return TaskExecutors.inOrderExecutor(SyncTaskExecutor.getSimpleExecutor());
    }

    @Test
    public void testRecursiveExecute() {
        final TaskExecutor executor = createSyncExecutor();

        final List<Integer> tasks = new LinkedList<>();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                executor.execute(cancelToken, new AddToQueueTask(2, tasks), null);
                tasks.add(0);
                executor.execute(cancelToken, new AddToQueueTask(3, tasks), null);
            }
        }, new AddToQueueCleanupTask(1, tasks));
        checkTaskList(tasks);
        assertEquals("All tasks must be executed", 4, tasks.size());
    }

    @Test
    public void testSimpleCancellation() {
        TaskExecutor executor = createSyncExecutor();

        List<Integer> tasks = new LinkedList<>();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new AddToQueueTask(0, tasks), new AddToQueueCleanupTask(1, tasks));
        executor.execute(Cancellation.CANCELED_TOKEN, new AddToQueueTask(-1, tasks), new AddToQueueCleanupTask(2, tasks));
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new AddToQueueTask(3, tasks), new AddToQueueCleanupTask(4, tasks));

        checkForAll(tasks, new ParameterizedTask<Integer>() {
            @Override
            public void execute(Integer arg) {
                assertTrue("Task should have been canceled.", arg >= 0);
            }
        });

        checkTaskList(tasks);
        assertEquals("Wrong number of tasks have been executed.", 5, tasks.size());
    }

    @Test
    public void testSimpleShutdown() {
        TaskExecutorService wrappedExecutor = new SyncTaskExecutor();
        TaskExecutor executor = TaskExecutors.inOrderExecutor(wrappedExecutor);

        List<Integer> tasks = new LinkedList<>();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new AddToQueueTask(0, tasks), new AddToQueueCleanupTask(1, tasks));
        wrappedExecutor.shutdownAndCancel();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new AddToQueueTask(-1, tasks), new AddToQueueCleanupTask(2, tasks));
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, new AddToQueueTask(-1, tasks), new AddToQueueCleanupTask(3, tasks));

        checkForAll(tasks, new ParameterizedTask<Integer>() {
            @Override
            public void execute(Integer arg) {
                assertTrue("Task should have been canceled.", arg >= 0);
            }
        });

        checkTaskList(tasks);
        assertEquals("Wrong number of tasks have been executed.", 4, tasks.size());
    }

    @Test
    public void testConcurrentTasks() {
        final int concurrencyLevel = 4;
        final int taskCount = 100;

        TaskExecutorService wrappedExecutor = new ThreadPoolTaskExecutor(
                "InOrderTaskExecutorTest executor", concurrencyLevel);
        try {
            TaskExecutor executor = TaskExecutors.inOrderExecutor(wrappedExecutor);

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
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    doneSignal.signal();
                }
            }, null);

            doneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN, 10000, TimeUnit.MILLISECONDS);

            checkTaskList(executedTasks);
            assertEquals("All tasks must be executed", taskIndex, executedTasks.size());
        } finally {
            wrappedExecutor.shutdown();
        }
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
}
