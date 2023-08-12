package org.jtrim2.concurrent;

import java.util.Arrays;
import org.jtrim2.testutils.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TasksTest {
    @Before
    public void setUp() {
        // clear interrupted status
        Thread.interrupted();
    }

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(Tasks.class);
    }

    /**
     * Test of noOpTask method, of class Tasks.
     */
    @Test
    public void testNoOpTask() {
        Runnable task = Tasks.noOpTask();
        assertNotNull(task.toString());
        task.run();
    }

    @Test
    public void testRunOnceTask() {
        Runnable subTask = mock(Runnable.class);
        when(subTask.toString()).thenReturn("TEST");

        Runnable task = Tasks.runOnceTask(subTask);
        assertNotNull(task.toString());

        task.run();
        verify(subTask).run();
        assertNotNull(task.toString());

        task.run();
        verify(subTask).run();
    }

    @Test
    public void testRunOnceTaskOptimized() {
        Runnable subTask = mock(Runnable.class);

        Runnable task1 = Tasks.runOnceTask(subTask);
        Runnable task2 = Tasks.runOnceTask(task1);
        assertSame(task1, task2);
    }

    @Test
    public void testRunOnceTaskOptimizedStrict() {
        Runnable subTask = mock(Runnable.class);

        Runnable task1 = Tasks.runOnceTaskStrict(subTask);
        Runnable task2 = Tasks.runOnceTaskStrict(task1);
        assertSame(task1, task2);
    }

    @Test
    public void testRunOnceTaskNotOptimizedForMixed1() {
        Runnable subTask = mock(Runnable.class);

        Runnable task1 = Tasks.runOnceTaskStrict(subTask);
        Runnable task2 = Tasks.runOnceTask(task1);
        assertNotSame(task1, task2);
    }

    @Test
    public void testRunOnceTaskNotOptimizedForMixed2() {
        Runnable subTask = mock(Runnable.class);

        Runnable task1 = Tasks.runOnceTask(subTask);
        Runnable task2 = Tasks.runOnceTaskStrict(task1);
        assertNotSame(task1, task2);
    }

    @Test
    public void testRunOnceTaskStrict() {
        Runnable subTask = mock(Runnable.class);
        Runnable task = Tasks.runOnceTaskStrict(subTask);

        task.run();
        try {
            task.run();
        } catch (IllegalStateException ex) {
            verify(subTask).run();
            return;
        }

        throw new AssertionError("Expected IllegalStateException on second try.");
    }

    public static class RunConcurrentlyTestsArray extends AbstractRunConcurrentlyTests {
        public RunConcurrentlyTestsArray() {
            super(Tasks::runConcurrently);
        }
    }

    public static class RunConcurrentlyTestsCollection extends AbstractRunConcurrentlyTests {
        public RunConcurrentlyTestsCollection() {
            super(tasks -> Tasks.runConcurrently(Arrays.asList(tasks)));
        }
    }

    private static Thread onInterruptThread(Runnable interruptTask) {
        Thread result = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                interruptTask.run();
            }
        });
        result.start();
        return result;
    }

    @Test
    public void testInterruptAll0() {
        Tasks.interruptAllNonNulls();
    }

    @Test
    public void testInterruptAll3() throws InterruptedException {
        Runnable interrupted1 = mock(Runnable.class);
        Runnable interrupted2 = mock(Runnable.class);

        Thread thread1 = onInterruptThread(interrupted1);
        Thread thread2 = onInterruptThread(interrupted2);

        Tasks.interruptAllNonNulls(thread1, null, thread2);

        thread1.join();
        thread2.join();

        verify(interrupted1).run();
        verify(interrupted2).run();
    }

    public abstract static class AbstractRunConcurrentlyTests {
        private final ConcurrentRunner runner;

        private AbstractRunConcurrentlyTests(ConcurrentRunner runner) {
            this.runner = runner;
        }

        @Test(timeout = 30000)
        public void testRunConcurrently() {
            for (int testIndex = 0; testIndex < 100; testIndex++) {
                for (int taskCount = 0; taskCount < 5; taskCount++) {
                    Runnable[] tasks = new Runnable[taskCount];
                    for (int i = 0; i < tasks.length; i++) {
                        tasks[i] = mock(Runnable.class);
                    }

                    runner.run(tasks);

                    for (int i = 0; i < tasks.length; i++) {
                        verify(tasks[i]).run();
                    }
                }
            }
        }

        @Test(timeout = 30000)
        public void testRunConcurrentlyWithInterrupt() {
            Runnable task1 = mock(Runnable.class);
            Runnable task2 = mock(Runnable.class);

            Thread.currentThread().interrupt();
            runner.run(task1, task2);
            assertTrue(Thread.currentThread().isInterrupted());

            verify(task1).run();
            verify(task2).run();
        }

        @Test(timeout = 30000)
        public void testRunConcurrentlyWithException() {
            Runnable task1 = mock(Runnable.class);
            Runnable task2 = mock(Runnable.class);
            Runnable task3 = mock(Runnable.class);
            Runnable task4 = mock(Runnable.class);

            RuntimeException ex2 = new RuntimeException();
            RuntimeException ex3 = new RuntimeException();

            doThrow(ex2).when(task2).run();
            doThrow(ex3).when(task3).run();

            try {
                runner.run(task1, task2, task3, task4);
                fail("Expected TaskExecutionException.");
            } catch (TaskExecutionException ex) {
                assertSame(ex2, ex.getCause());

                Throwable[] suppressed = ex.getSuppressed();
                assertEquals(1, suppressed.length);
                assertSame(ex3, suppressed[0]);
            }

            verify(task1).run();
            verify(task2).run();
            verify(task3).run();
            verify(task4).run();
        }
    }

    private interface ConcurrentRunner {
        public void run(Runnable... tasks);
    }
}
