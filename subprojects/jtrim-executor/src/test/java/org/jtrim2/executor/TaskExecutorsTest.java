package org.jtrim2.executor;

import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.testutils.TestUtils;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TaskExecutorsTest {
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
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
        verify(subExecutor).execute(any(CancellationToken.class), any(CancelableTask.class));
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

    @Test
    public void testInOrderExecutor() throws Exception {
        ManualTaskExecutor subExecutor = new ManualTaskExecutor(true);
        TaskExecutor executor = TaskExecutors.inOrderExecutor(subExecutor);
        assertTrue(executor instanceof InOrderTaskExecutor);

        // just test if it really delegates its calls to subExecutor
        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);

        verifyNoInteractions(task);
        subExecutor.executeCurrentlySubmitted();
        verify(task).execute(any(CancellationToken.class));
    }

    @Test
    public void testInOrderSimpleExecutor() throws Exception {
        ManualTaskExecutor subExecutor = new ManualTaskExecutor(true);
        TaskExecutor executor = TaskExecutors.inOrderSimpleExecutor(subExecutor);
        assertTrue(executor instanceof InOrderTaskExecutor);

        // just test if it really delegates its calls to subExecutor
        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);

        verifyNoInteractions(task);
        subExecutor.executeCurrentlySubmitted();
        verify(task).execute(any(CancellationToken.class));
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
    public void testInOrderExecutorAlreadyFifo3() {
        SimpleThreadPoolTaskExecutor executor = new SimpleThreadPoolTaskExecutor("TEST-POOL", 1, 1, Thread::new);
        executor.dontNeedShutdown();

        assertSame(executor, TaskExecutors.inOrderExecutor(executor));
        assertSame(executor, TaskExecutors.inOrderSimpleExecutor(executor));
    }

    @Test
    public void testSimpleThreadPoolTaskExecutorNotFifoWithMoreThreads() {
        SimpleThreadPoolTaskExecutor executor = new SimpleThreadPoolTaskExecutor("TEST-POOL", 2, 1, Thread::new);
        executor.dontNeedShutdown();

        assertFalse("fifo", FifoExecutor.isFifoExecutor(executor));
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
    public void testInOrderSimpleExecutorAlreadyFifo3() {
        SimpleThreadPoolTaskExecutor deepExecutor = new SimpleThreadPoolTaskExecutor(
                "TEST-POOL",
                1,
                1,
                Thread::new
        );
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
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);

        verify(task).execute(any(CancellationToken.class));
    }

    private static void checkSubmitDelegates(
            TaskExecutorService executor,
            ManualTaskExecutor wrapped) throws Exception {

        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);

        verifyNoInteractions(task);
        wrapped.executeCurrentlySubmitted();
        verify(task).execute(any(CancellationToken.class));
    }

    @Test
    public void testUpgradeToStoppable() throws Exception {
        ManualTaskExecutor subExecutor = new ManualTaskExecutor(true);
        TaskExecutorService executor = TaskExecutors.upgradeToStoppable(subExecutor);
        assertTrue(executor instanceof UpgradedTaskExecutor);

        checkSubmitDelegates(executor, subExecutor);
    }

    @Test
    public void testUpgradeToUnstoppable() throws Exception {
        ManualTaskExecutor subExecutor = new ManualTaskExecutor(true);
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
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
        verify(subExecutor).execute(
                any(CancellationToken.class),
                any(CancelableTask.class));
        verifyNoMoreInteractions(subExecutor);
    }

    @Test
    public void testDebugExecutor() {
        TaskExecutor subExecutor = mock(TaskExecutor.class);
        TaskExecutor executor = TaskExecutors.debugExecutor(subExecutor);
        assertTrue(executor instanceof DebugTaskExecutor);

        // just test if it really delegates its calls to subExecutor
        CancelableTask task = mock(CancelableTask.class);
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, task);
        verify(subExecutor).execute(
                any(CancellationToken.class),
                any(CancelableTask.class));
        verifyNoMoreInteractions(subExecutor);
    }
}
