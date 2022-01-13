package org.jtrim2.stream;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
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
                = ExecutorRef.owned(executors::getFirst);

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
            String expectedName) throws InterruptedException {

        TaskExecutorService executor = (TaskExecutorService) executorRef.getExecutor();
        try {
            int threadCount = 3;
            WaitableSignal startSignal = new WaitableSignal();
            CountDownLatch startedLatch = new CountDownLatch(threadCount);
            CountDownLatch finishedLatch = new CountDownLatch(threadCount);
            String[] threadNames = new String[threadCount];
            long[] threadIds = new long[threadCount];
            for (int i = 0; i < threadCount; i++) {
                int threadIndex = i;
                executor.execute(() -> {
                    try {
                        startedLatch.countDown();
                        startSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
                        threadNames[threadIndex] = Thread.currentThread().getName();
                        threadIds[threadIndex] = Thread.currentThread().getId();
                    } finally {
                        finishedLatch.countDown();
                    }
                });
            }

            // Ensure that all threads could start in parallel
            if (!startedLatch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Failed to start all threads in parallel.");
            }
            startSignal.signal();
            if (!finishedLatch.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Failed to finish all threads.");
            }

            Set<Long> ids = new HashSet<>();
            for (int i = 0; i < threadCount; i++) {
                String threadName = threadNames[i];
                assertNotNull("threadName", threadName);
                assertTrue(threadName, threadName.contains(expectedName));
                ids.add(threadIds[i]);
            }
            assertEquals("thread count", threadCount, ids.size());
        } finally {
            executorRef.finishUsage();
        }
        assertTrue("executor2.isShutdown", executor.isShutdown());
        return executor;
    }

    @Test(timeout = 10000)
    public void testExecutorOwnedThreadPool() throws InterruptedException {
        Supplier<ExecutorRef> executorRefProvider
                = ExecutorRef.owned("MyTestExecutor");

        TaskExecutorService executor1 = verifyOwnedThreadPool(executorRefProvider.get(), "MyTestExecutor");
        TaskExecutorService executor2 = verifyOwnedThreadPool(executorRefProvider.get(), "MyTestExecutor");
        assertNotSame(executor1, executor2);
    }
}
