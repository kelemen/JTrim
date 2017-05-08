package org.jtrim2.executor;

import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.testutils.TestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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

    @Test
    public void testUtilityClass() {
        TestUtils.testUtilityClass(TaskExecutors.class);
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

    /**
     * Test of inOrderExecutor method, of class TaskExecutors.
     */
    @Test
    public void testInOrderSimpleExecutor() {
        TaskExecutor subExecutor = mock(TaskExecutor.class);
        TaskExecutor executor = TaskExecutors.inOrderSimpleExecutor(subExecutor);
        assertTrue(executor instanceof InOrderTaskExecutor);

        // just test if it really delegates its calls to subExecutor
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(subExecutor).execute(any(CancellationToken.class), any(CancelableTask.class), any(CleanupTask.class));
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testInOrderExecutorAlreadyFifo1() {
        SingleThreadedExecutor executor = new SingleThreadedExecutor("TEST-POOL");
        executor.dontNeedShutdown();

        assertSame(executor, TaskExecutors.inOrderExecutor(executor));
        assertSame(executor, TaskExecutors.inOrderSimpleExecutor(executor));
    }

    @Test
    public void testInOrderExecutorAlreadyFifo2() {
        InOrderTaskExecutor executor = new InOrderTaskExecutor(SyncTaskExecutor.getSimpleExecutor());
        assertSame(executor, TaskExecutors.inOrderExecutor(executor));
        assertSame(executor, TaskExecutors.inOrderSimpleExecutor(executor));
    }

    @Test
    public void testInOrderSimpleExecutorAlreadyFifo1() {
        SingleThreadedExecutor deepExecutor = new SingleThreadedExecutor("TEST-POOL");
        deepExecutor.dontNeedShutdown();

        TaskExecutor executor = TaskExecutors.asUnstoppableExecutor(deepExecutor);

        assertSame(executor, TaskExecutors.inOrderSimpleExecutor(executor));
    }

    @Test
    public void testInOrderSimpleExecutorAlreadyFifo2() {
        SingleThreadedExecutor deepExecutor = new SingleThreadedExecutor("TEST-POOL");
        deepExecutor.dontNeedShutdown();

        TaskExecutor executor = new InOrderTaskExecutor(deepExecutor);

        assertSame(executor, TaskExecutors.inOrderSimpleExecutor(executor));
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

    private static void checkSubmitDelegates(TaskExecutorService executor, TaskExecutor wrappedMock) {
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(wrappedMock).execute(any(CancellationToken.class), any(CancelableTask.class), any(CleanupTask.class));
        verifyNoMoreInteractions(wrappedMock);
    }

    @Test
    public void testUpgradeToStoppable() {
        TaskExecutor subExecutor = mock(TaskExecutor.class);
        TaskExecutorService executor = TaskExecutors.upgradeToStoppable(subExecutor);
        assertTrue(executor instanceof UpgradedTaskExecutor);

        checkSubmitDelegates(executor, subExecutor);
    }

    @Test
    public void testUpgradeToStoppableForUnstoppable() {
        TaskExecutorService mockedExecutor = mock(TaskExecutorService.class);
        TaskExecutorService subExecutor = new UnstoppableTaskExecutor(mockedExecutor);
        TaskExecutorService executor = TaskExecutors.upgradeToStoppable(subExecutor);
        assertTrue(executor instanceof UpgradedTaskExecutor);

        checkSubmitDelegates(executor, mockedExecutor);
    }

    @Test
    public void testUpgradeToUnstoppable() {
        TaskExecutor subExecutor = mock(TaskExecutor.class);
        TaskExecutorService executor = TaskExecutors.upgradeToUnstoppable(subExecutor);
        assertTrue(executor instanceof UnstoppableTaskExecutor);

        checkSubmitDelegates(executor, subExecutor);
    }

    @Test
    public void testUpgradeToUnstoppableForUnstoppable() {
        // For UnstoppableTaskExecutor we can return the same executor.
        TaskExecutorService subExecutor = new UnstoppableTaskExecutor(mock(TaskExecutorService.class));
        TaskExecutorService executor = TaskExecutors.upgradeToUnstoppable(subExecutor);
        assertSame(subExecutor, executor);
    }

    @Test
    public void testContextAwareIfNecessary() {
        TaskExecutor wrapped = mock(TaskExecutor.class);
        ContextAwareTaskExecutor executor = TaskExecutors.contextAwareIfNecessary(wrapped);
        assertTrue(executor instanceof ContextAwareWrapper);
    }

    @Test
    public void testContextAwareIfNecessaryOfContextAware() {
        ContextAwareTaskExecutor wrapped = mock(ContextAwareTaskExecutor.class);
        ContextAwareTaskExecutor executor = TaskExecutors.contextAwareIfNecessary(wrapped);
        assertSame(wrapped, executor);
    }

    @Test
    public void testDebugExecutorService() {
        TaskExecutorService subExecutor = mock(TaskExecutorService.class);
        TaskExecutor executor = TaskExecutors.debugExecutorService(subExecutor);
        assertTrue(executor instanceof DebugTaskExecutorService);

        // just test if it really delegates its calls to subExecutor
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(subExecutor).execute(
                any(CancellationToken.class),
                any(CancelableTask.class),
                any(CleanupTask.class));
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testDebugExecutor() {
        TaskExecutor subExecutor = mock(TaskExecutor.class);
        TaskExecutor executor = TaskExecutors.debugExecutor(subExecutor);
        assertTrue(executor instanceof DebugTaskExecutor);

        // just test if it really delegates its calls to subExecutor
        CancelableTask task = mock(CancelableTask.class);
        CleanupTask cleanup = mock(CleanupTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task, cleanup);
        verify(subExecutor).execute(
                any(CancellationToken.class),
                any(CancelableTask.class),
                any(CleanupTask.class));
        verifyNoMoreInteractions(subExecutor);
    }
}
