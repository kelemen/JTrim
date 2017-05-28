package org.jtrim2.testutils.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.CancelableTasks;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Test;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public abstract class GenericExecutorServiceTests extends GenericExecutorTests<TaskExecutorService> {
    public GenericExecutorServiceTests(
            Collection<? extends TestExecutorFactory<? extends TaskExecutorService>> factories) {
        super(factories);
    }

    public static <E extends TaskExecutorService> Collection<TestExecutorFactory<E>> executorServices(
            Collection<? extends Supplier<? extends E>> factories) {

        return factories.stream()
                .map(GenericExecutorServiceTests::executorServiceFactory)
                .collect(Collectors.toList());
    }

    private static <E extends TaskExecutorService> TestExecutorFactory<E> executorServiceFactory(
            Supplier<? extends E> executorFactory) {
        return new TestExecutorFactory<>(executorFactory, GenericExecutorServiceTests::shutdownTestExecutor);
    }

    public static void shutdownTestExecutor(TaskExecutorService executor) throws InterruptedException {
        executor.shutdown();
        waitTerminateAndTest(executor);
    }

    // Waits until the specified executor terminates and tests
    // if the terminate listener has been called.
    public static void waitTerminateAndTest(TaskExecutorService executor) throws InterruptedException {
        final CountDownLatch listener1Latch = new CountDownLatch(1);
        executor.addTerminateListener(listener1Latch::countDown);
        executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue(executor.isTerminated());
        listener1Latch.await();

        final AtomicReference<Thread> callingThread = new AtomicReference<>(null);
        executor.addTerminateListener(() -> {
            callingThread.set(Thread.currentThread());
        });
        assertSame(Thread.currentThread(), callingThread.get());
    }

    private static void ensureBackgroundThreadStarted(TaskExecutorService executor) {
        WaitableSignal taskSignal = new WaitableSignal();
        executor.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask())
                .whenComplete((result, error) -> taskSignal.signal());
        if (!taskSignal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, 5, TimeUnit.SECONDS)) {
            throw new AssertionError("timeout");
        }
    }

    private void testShutdownAllowsPreviouslySubmittedTasks(
            boolean preStartThread,
            boolean testCleanup) throws Exception {
        testAllCreated((executor) -> {
            AtomicReference<Throwable> error = new AtomicReference<>();

            if (preStartThread) {
                ensureBackgroundThreadStarted(executor);
            }

            Object result = "Test-Result-twt3t53543";
            MockFunction<Object> function = mockFunction(result);
            MockCleanup cleanup = null;

            CompletionStage<Object> future
                    = executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, toFunction(function));

            if (testCleanup) {
                cleanup = mock(MockCleanup.class);
                future.whenComplete(toCleanupTask(cleanup));
            }
            executor.shutdown();

            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);

            if (cleanup == null) {
                verify(function).execute(false);
            }
            else {
                InOrder inOrder = inOrder(function, cleanup);
                inOrder.verify(function).execute(false);
                inOrder.verify(cleanup).cleanup(result, null);
                inOrder.verifyNoMoreInteractions();
            }

            return () -> ExceptionHelper.rethrowIfNotNull(error.get());
        });
    }

    @Test(timeout = 10000)
    public final void testShutdownAllowsPreviouslySubmittedTasks1() throws Exception {
        testShutdownAllowsPreviouslySubmittedTasks(false, false);
    }

    @Test(timeout = 10000)
    public final void testShutdownAllowsPreviouslySubmittedTasks2() throws Exception {
        testShutdownAllowsPreviouslySubmittedTasks(true, false);
    }

    @Test(timeout = 10000)
    public final void testShutdownAllowsPreviouslySubmittedCleanupTasks1() throws Exception {
        testShutdownAllowsPreviouslySubmittedTasks(false, true);
    }

    @Test(timeout = 10000)
    public final void testShutdownAllowsPreviouslySubmittedCleanupTasks2() throws Exception {
        testShutdownAllowsPreviouslySubmittedTasks(true, true);
    }

    @Test(timeout = 10000)
    public final void testDoesntTerminateBeforeTaskCompletes1() throws Exception {
        testAllCreated(this::testDoesntTerminateBeforeTaskCompletes1);
    }

    private AfterTerminate testDoesntTerminateBeforeTaskCompletes1(TaskExecutorService executor) throws Exception {
        final WaitableSignal mayWaitSignal = new WaitableSignal();

        MockTask task = mock(MockTask.class);
        MockTaskResult taskResult = MockTask.stubNonFailing(task, (canceled) -> {
            executor.shutdown();
            mayWaitSignal.signal();
            assertTrue("Should be shut down.", executor.isShutdown());
            Thread.sleep(50);
            assertFalse("Should not be terminated", executor.isTerminated());
        });

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, toTask(task));

        if (!mayWaitSignal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, 10, TimeUnit.SECONDS)) {
            throw new AssertionError("timeout");
        }
        executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        taskResult.verifySuccess();
        return null;
    }

    private void testCleanupsWithShutdown(boolean cancel) throws Exception {
        testCleanups(cancel, (executor) -> {
            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        });
    }

    @Test(timeout = 10000)
    public final void testShutdownWithCleanups() throws Exception {
        testCleanupsWithShutdown(false);
    }

    @Test(timeout = 10000)
    public final void testCanceledShutdownWithCleanups() throws Exception {
        testCleanupsWithShutdown(true);
    }

    @Test(timeout = 10000)
    public final void testShutdownAndCancel() throws Exception {
        testAllCreated((executor) -> {
            CancelableTask task1 = mock(CancelableTask.class);
            WaitableSignal shutdownSignal = new WaitableSignal();
            MockTaskResult task1Verification = MockTask.stubCancelableNonFailing(task1, (cancelToken) -> {
                assertFalse("pre-shutdown-cancel", cancelToken.isCanceled());
                executor.shutdownAndCancel();
                shutdownSignal.signal();
                assertTrue("post-shutdown-cancel", cancelToken.isCanceled());
            });

            CancelableTask task2 = mock(CancelableTask.class);
            MockCleanup cleanup1 = mock(MockCleanup.class);
            MockCleanup cleanup2 = mock(MockCleanup.class);

            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1)
                    .whenComplete(toCleanupTask(cleanup1));
            shutdownSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
            executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2)
                    .whenComplete(toCleanupTask(cleanup2));

            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);

            task1Verification.verifySuccess();
            verifyZeroInteractions(task2);

            verify(cleanup1).cleanup(isNull(), isNull(Throwable.class));
            verify(cleanup2).cleanup(isNull(), isA(OperationCanceledException.class));

            return null;
        });
    }

    @Test(timeout = 10000)
    public final void testSubmitTasksAfterShutdown() throws Exception {
        testAllCreated((executor) -> {
            int taskCount = 100;

            CancelableTask task1 = mock(CancelableTask.class);
            CancelableTask task2 = mock(CancelableTask.class);

            MockCleanup cleanup1 = mock(MockCleanup.class);
            MockCleanup cleanup2 = mock(MockCleanup.class);

            for (int i = 0; i < taskCount; i++) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task1)
                        .whenComplete(toCleanupTask(cleanup1));
            }

            executor.shutdown();

            for (int i = 0; i < taskCount; i++) {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, task2)
                        .whenComplete(toCleanupTask(cleanup2));
            }

            return () -> {
                verify(task1, times(taskCount)).execute(any(CancellationToken.class));
                verifyZeroInteractions(task2);

                verify(cleanup1, times(taskCount)).cleanup(isNull(), isNull(Throwable.class));
                verify(cleanup2, times(taskCount)).cleanup(isNull(), isA(OperationCanceledException.class));
            };
        });
    }

    @Test(timeout = 10000)
    public final void testAwaitTerminationTimeout() throws Exception {
        testAllCreated((executor) -> {
            assertFalse(executor.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, 100, TimeUnit.NANOSECONDS));
            return null;
        });
    }

    @Test(timeout = 10000)
    public final void testTerminatedAfterAwaitTermination() throws Exception {
        for (int i = 0; i < 100; i++) {
            testAllCreated((executor) -> {
                executor.execute(Cancellation.UNCANCELABLE_TOKEN, CancelableTasks.noOpCancelableTask());
                executor.shutdown();
                executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
                assertTrue("Must be terminated after awaitTermination.", executor.isTerminated());
                return null;
            });
        }
    }

    @Test(timeout = 30000)
    public final void testConcurrentShutdown() throws Exception {
        for (int i = 0; i < 100; i++) {
            testAllCreated(this::testConcurrentShutdown);
        }
    }

    private AfterTerminate testConcurrentShutdown(TaskExecutorService executor) throws Exception {
        int taskCount = getThreadCount();

        Runnable[] concurrentTasks = new Runnable[taskCount + 1];
        concurrentTasks[0] = executor::shutdown;

        List<MockCleanup> cleanups = new ArrayList<>(taskCount);
        List<Object> results = new ArrayList<>(taskCount);

        for (int i = 0; i < taskCount; i++) {
            Object result = "test-result-43289594-" + i;
            results.add(result);

            CancelableFunction<Object> cancelableFunction = (cancelToken) -> result;
            MockCleanup cleanup = mock(MockCleanup.class);
            cleanups.add(cleanup);

            concurrentTasks[i + 1] = () -> {
                executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, cancelableFunction)
                        .whenComplete(MockCleanup.toCleanupTask(cleanup));
            };
        }

        Tasks.runConcurrently(concurrentTasks);

        return () -> {
            for (int i = 0; i < taskCount; i++) {
                verifyResultOrCanceled(cleanups.get(i), results.get(i));
            }
        };
    }

    protected interface TestCreatedMethod {
        public AfterTerminate doTest(TaskExecutorService executor) throws Exception;
    }
}
