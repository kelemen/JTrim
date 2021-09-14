package org.jtrim2.stream;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.function.Supplier;
import org.jtrim2.executor.SingleThreadedExecutor;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.ThreadPoolTaskExecutor;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExecutorRefTest {

    @Test
    public void testExecutorRefExternal() {
        TaskExecutorService executor = new SyncTaskExecutor();
        Supplier<ExecutorRef> executorRefProvider
                = ExecutorRef.external(executor);

        ExecutorRef executorRef = executorRefProvider.get();
        assertSame(executor, executorRef.getExecutor());

        executorRef.finishUsage();
        assertFalse("executor.isShutdown", executor.isShutdown());
    }

    @Test
    public void testExecutorRefOwned() {
        TaskExecutorService executor1 = new SyncTaskExecutor();
        TaskExecutorService executor2 = new SyncTaskExecutor();

        Deque<TaskExecutorService> executors = new ArrayDeque<>(Arrays.asList(executor1, executor2));
        Supplier<ExecutorRef> executorRefProvider
                = ExecutorRef.owned(() -> executors.getFirst());

        ExecutorRef executor1Ref = executorRefProvider.get();
        ExecutorRef executor2Ref = executorRefProvider.get();

        assertSame(executor1, executor1Ref.getExecutor());
        assertSame(executor1, executor2Ref.getExecutor());

        executor1Ref.finishUsage();
        assertTrue("executor11.isShutdown", executor1.isShutdown());

        executor2Ref.finishUsage();
        assertFalse("executor2.isShutdown", executor2.isShutdown());
    }

    private TaskExecutorService verifyOwnedThreadPool(
            ExecutorRef executorRef,
            String expectedName,
            int expectedThreadCount) {

        TaskExecutorService executor = (TaskExecutorService) executorRef.getExecutor();
        try {
            if (!(executor instanceof ThreadPoolTaskExecutor)) {
                throw new AssertionError("Expected thread pool, but received: " + executor);
            }

            ThreadPoolTaskExecutor threadPool = (ThreadPoolTaskExecutor) executor;
            assertEquals(expectedName, threadPool.getPoolName());
            assertEquals(expectedThreadCount, threadPool.getMaxThreadCount());
        } finally {
            executorRef.finishUsage();
        }
        assertTrue("executor2.isShutdown", executor.isShutdown());
        return executor;
    }

    private TaskExecutorService verifyOwnedSingleThreadPool(
            ExecutorRef executorRef,
            String expectedName) {

        TaskExecutorService executor = (TaskExecutorService) executorRef.getExecutor();
        try {
            if (!(executor instanceof SingleThreadedExecutor)) {
                throw new AssertionError("Expected thread pool, but received: " + executor);
            }

            SingleThreadedExecutor threadPool = (SingleThreadedExecutor) executor;
            assertEquals(expectedName, threadPool.getPoolName());
        } finally {
            executorRef.finishUsage();
        }
        assertTrue("executor2.isShutdown", executor.isShutdown());
        return executor;
    }

    @Test
    public void testExecutorOwnedThreadPool() {
        Supplier<ExecutorRef> executorRefProvider
                = ExecutorRef.owned("MyTestExecutor", 11);

        TaskExecutorService executor1 = verifyOwnedThreadPool(executorRefProvider.get(), "MyTestExecutor", 11);
        TaskExecutorService executor2 = verifyOwnedThreadPool(executorRefProvider.get(), "MyTestExecutor", 11);
        assertNotSame(executor1, executor2);
    }

    @Test
    public void testExecutorOwnedSingleThreadPool() {
        Supplier<ExecutorRef> executorRefProvider
                = ExecutorRef.owned("MyTestExecutor", 1);

        TaskExecutorService executor1 = verifyOwnedSingleThreadPool(executorRefProvider.get(), "MyTestExecutor");
        TaskExecutorService executor2 = verifyOwnedSingleThreadPool(executorRefProvider.get(), "MyTestExecutor");
        assertNotSame(executor1, executor2);
    }
}
