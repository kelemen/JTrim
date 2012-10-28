package org.jtrim.concurrent;

import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author Kelemen Attila
 */
public class TaskExecutorsTest {

    public TaskExecutorsTest() {
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

    /**
     * Test of asUnstoppableExecutor method, of class TaskExecutors.
     */
    @Test
    public void testAsUnstoppableExecutor() {
        TaskExecutorService subExecutor = mock(TaskExecutorService.class);
        TaskExecutorService executor = TaskExecutors.asUnstoppableExecutor(subExecutor);
        assertTrue(executor instanceof UnstoppableTaskExecutor);

        // just test if it really delegates its calls to subExecutor
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(subExecutor).execute(any(CancellationToken.class), any(CancelableTask.class), any(CleanupTask.class));
        verifyNoMoreInteractions(subExecutor);
    }

    /**
     * Test of asUnstoppableExecutor method, of class TaskExecutors.
     */
    @Test
    public void testAsUnstoppableExecutorForUnstoppable() {
        // For UnstoppableTaskExecutor we can return the same executor.
        TaskExecutorService subExecutor = new UnstoppableTaskExecutor(mock(TaskExecutorService.class));
        TaskExecutorService executor = TaskExecutors.asUnstoppableExecutor(subExecutor);
        assertSame(subExecutor, executor);
    }

    /**
     * Test of inOrderExecutor method, of class TaskExecutors.
     */
    @Test
    public void testInOrderExecutor() {
        TaskExecutor subExecutor = mock(TaskExecutor.class);
        TaskExecutor executor = TaskExecutors.inOrderExecutor(subExecutor);
        assertTrue(executor instanceof InOrderTaskExecutor);

        // just test if it really delegates its calls to subExecutor
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(subExecutor).execute(any(CancellationToken.class), any(CancelableTask.class), any(CleanupTask.class));
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testInOrderSyncExecutor() throws Exception {
        TaskExecutor executor = TaskExecutors.inOrderSyncExecutor();
        assertTrue(executor instanceof InOrderTaskExecutor);

        // just test if it really delegates its calls to a sync executor
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);

        verify(task).execute(any(CancellationToken.class));
        verify(cleanup).cleanup(false, null);
        verifyNoMoreInteractions(task, cleanup);
    }

    /**
     * Test of upgradeExecutor method, of class TaskExecutors.
     */
    @Test
    public void testUpgradeExecutor() {
        TaskExecutor subExecutor = mock(TaskExecutor.class);
        TaskExecutorService executor = TaskExecutors.upgradeExecutor(subExecutor);
        assertTrue(executor instanceof UpgradedTaskExecutor);

        // just test if it really delegates its calls to subExecutor
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(subExecutor).execute(any(CancellationToken.class), any(CancelableTask.class), any(CleanupTask.class));
        verifyNoMoreInteractions(subExecutor);
    }

    /**
     * Test of upgradeExecutor method, of class TaskExecutors.
     */
    @Test
    public void testUpgradeExecutorForUnstoppable() {
        // For UnstoppableTaskExecutor we can return the same executor.
        TaskExecutorService subExecutor = new UnstoppableTaskExecutor(mock(TaskExecutorService.class));
        TaskExecutorService executor = TaskExecutors.upgradeExecutor(subExecutor);
        assertSame(subExecutor, executor);
    }
}