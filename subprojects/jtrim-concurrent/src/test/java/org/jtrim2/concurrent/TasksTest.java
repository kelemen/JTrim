package org.jtrim2.concurrent;

import org.jtrim2.testutils.TestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TasksTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        // clear interrupted status
        Thread.interrupted();
    }

    @After
    public void tearDown() {
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

    /**
     * Test of runOnceTask method, of class Tasks.
     */
    @Test
    public void testRunOnceTask() {
        Runnable subTask = mock(Runnable.class);
        stub(subTask.toString()).toReturn("TEST");

        Runnable task = Tasks.runOnceTask(subTask, false);
        assertNotNull(task.toString());

        task.run();
        verify(subTask).run();
        assertNotNull(task.toString());

        task.run();
        verify(subTask).run();
    }

    @Test(expected = IllegalStateException.class)
    public void testRunOnceTaskFailOnReRun() {
        Runnable subTask = mock(Runnable.class);
        Runnable task = Tasks.runOnceTask(subTask, true);

        try {
            try {
                task.run();
            } catch (IllegalStateException ex) {
                throw new RuntimeException(ex);
            }
            task.run();
        } finally {
            verify(subTask).run();
        }
    }

    @Test(timeout = 30000)
    public void testRunConcurrently() {
        for (int testIndex = 0; testIndex < 100; testIndex++) {
            for (int taskCount = 0; taskCount < 5; taskCount++) {
                Runnable[] tasks = new Runnable[taskCount];
                for (int i = 0; i < tasks.length; i++) {
                    tasks[i] = mock(Runnable.class);
                }

                Tasks.runConcurrently(tasks);

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
        Tasks.runConcurrently(task1, task2);
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
            Tasks.runConcurrently(task1, task2, task3, task4);
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
