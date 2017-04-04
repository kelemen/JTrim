package org.jtrim.concurrent;

import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class TaskSchedulerTest {

    public TaskSchedulerTest() {
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

    @Test
    public void testSingleSchedule1() {
        Runnable task = mock(Runnable.class);
        TaskScheduler scheduler = TaskScheduler.newSyncScheduler();

        scheduler.scheduleTask(task);
        verifyZeroInteractions(task);

        scheduler.dispatchTasks();
        verify(task).run();
        verifyNoMoreInteractions(task);
    }

    @Test
    public void testSingleSchedule2() {
        Runnable task = mock(Runnable.class);
        TaskScheduler scheduler = new TaskScheduler(SyncTaskExecutor.getSimpleExecutor());

        scheduler.scheduleTask(task);
        verifyZeroInteractions(task);

        scheduler.dispatchTasks();
        verify(task).run();
        verifyNoMoreInteractions(task);
    }

    @Test
    public void testComplexSchedule() {
        Runnable task1 = mock(Runnable.class);
        Runnable task2 = mock(Runnable.class);
        Runnable task3 = mock(Runnable.class);
        Runnable task4 = mock(Runnable.class);
        final Runnable task5 = mock(Runnable.class);
        final Runnable task6 = mock(Runnable.class);

        final TaskScheduler scheduler = TaskScheduler.newSyncScheduler();

        doAnswer((InvocationOnMock invocation) -> {
            scheduler.scheduleTask(task6);
            scheduler.dispatchTasks();
            task5.run();
            return null;
        }).when(task4).run();

        scheduler.scheduleTasks(Arrays.asList(task1, task2, task3));
        scheduler.scheduleTask(task4);

        verifyZeroInteractions(task1, task2, task3, task4, task5, task6);

        scheduler.dispatchTasks();

        verify(task1).run();
        verify(task2).run();
        verify(task3).run();
        verify(task4).run();
        verify(task5).run();
        verify(task6).run();
        verifyNoMoreInteractions(task1, task2, task3, task4, task5, task6);
    }

    @Test
    public void testConcurrentSchedule() {
        final TaskScheduler scheduler = TaskScheduler.newSyncScheduler();

        Runnable task1 = mock(Runnable.class);
        Runnable task2 = mock(Runnable.class);
        final Runnable task3 = mock(Runnable.class);

        doAnswer((InvocationOnMock invocation) -> {
            Thread concurrent = new Thread(() -> {
                scheduler.scheduleTask(task3);
                scheduler.dispatchTasks();
            });
            concurrent.start();
            concurrent.join();
            return null;
        }).when(task1).run();

        scheduler.scheduleTask(task1);
        scheduler.scheduleTask(task2);
        scheduler.dispatchTasks();

        verify(task1).run();
        verify(task2).run();
        verify(task3).run();
        verifyNoMoreInteractions(task1, task2, task3);
    }

    @Test
    public void testExceptions() {
        Runnable task1 = mock(Runnable.class);
        Runnable task2 = mock(Runnable.class);
        Runnable task3 = mock(Runnable.class);

        RuntimeException exception1 = new RuntimeException("FIRST_EXCEPTION");
        RuntimeException exception2 = new RuntimeException("SUPPRESSED-1");
        RuntimeException exception3 = new RuntimeException("SUPPRESSED-2");

        doThrow(exception1).when(task1).run();
        doThrow(exception2).when(task2).run();
        doThrow(exception3).when(task3).run();

        TaskScheduler scheduler = TaskScheduler.newSyncScheduler();
        scheduler.scheduleTasks(Arrays.asList(task1, task2, task3));

        try {
            scheduler.dispatchTasks();
            fail("Expected an exception.");
        } catch (RuntimeException ex) {
            assertSame(exception1, ex);

            Throwable[] suppressed = ex.getSuppressed();
            assertEquals(2, suppressed.length);

            assertSame(exception2, suppressed[0]);
            assertSame(exception3, suppressed[1]);
        }

        verify(task1).run();
        verify(task2).run();
        verify(task3).run();
        verifyNoMoreInteractions(task1, task2, task3);
    }

    /**
     * Test of toString method, of class TaskScheduler.
     */
    @Test
    public void testToString() {
        assertNotNull(TaskScheduler.newSyncScheduler().toString());
    }
}
