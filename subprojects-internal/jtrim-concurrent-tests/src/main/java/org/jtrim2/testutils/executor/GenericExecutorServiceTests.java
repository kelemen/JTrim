package org.jtrim2.testutils.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.CancelableTasks;
import org.jtrim2.executor.MonitorableTaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.testutils.JTrimTests;
import org.jtrim2.testutils.UnsafeRunnable;
import org.jtrim2.testutils.cancel.TestCancellationSource;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public abstract class GenericExecutorServiceTests extends JTrimTests<Supplier<TaskExecutorService>> {
    public GenericExecutorServiceTests(Collection<Supplier<TaskExecutorService>> factories) {
        super(factories);
    }

    protected final void testAllCreated(TestCreatedMethod testMethod) throws Exception {
        testAll((factory) -> {
            AfterTerminate afterTerminateVerification;

            TaskExecutorService executor = factory.get();
            try {
                afterTerminateVerification = testMethod.doTest(executor);
            } finally {
                executor.shutdown();
                waitTerminateAndTest(executor);
            }

            if (afterTerminateVerification != null) {
                afterTerminateVerification.verifyAfterTerminate();
            }
        });
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

    public static <V> MockFunction<V> mockFunction(V result) {
        return MockFunction.mock(result);
    }

    public static CancelableTask toTask(MockTask mockTask) {
        return MockTask.toTask(mockTask);
    }

    public static <V> CancelableFunction<V> toFunction(MockFunction<V> mockFunction) {
        return MockFunction.toFunction(mockFunction);
    }

    public static <V> BiConsumer<V, Throwable> toCleanupTask(MockCleanup mockCleanup) {
        return MockCleanup.toCleanupTask(mockCleanup);
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
    public void testShutdownAllowsPreviouslySubmittedTasks1() throws Exception {
        testShutdownAllowsPreviouslySubmittedTasks(false, false);
    }

    @Test(timeout = 10000)
    public void testShutdownAllowsPreviouslySubmittedTasks2() throws Exception {
        testShutdownAllowsPreviouslySubmittedTasks(true, false);
    }

    @Test(timeout = 10000)
    public void testShutdownAllowsPreviouslySubmittedCleanupTasks1() throws Exception {
        testShutdownAllowsPreviouslySubmittedTasks(false, true);
    }

    @Test(timeout = 10000)
    public void testShutdownAllowsPreviouslySubmittedCleanupTasks2() throws Exception {
        testShutdownAllowsPreviouslySubmittedTasks(true, true);
    }

    @Test(timeout = 10000)
    public void testDoesntTerminateBeforeTaskCompletes1() throws Exception {
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

    private static <V> V waitResult(CompletionStage<V> future) {
        return CompletionStages.get(future.toCompletableFuture(), 10, TimeUnit.SECONDS);
    }

    private static <V> V waitResultWithCallback(CompletionStage<V> future) {
        CompletableFuture<V> waitableFuture = CompletionStages.toSafeWaitable(future);
        return CompletionStages.get(waitableFuture, 10, TimeUnit.SECONDS);
    }

    @Test(timeout = 10000)
    public void testSubmitTaskNoCleanup() throws Exception {
        testAllCreated(this::testSubmitTaskNoCleanup);
    }

    private AfterTerminate testSubmitTaskNoCleanup(TaskExecutorService executor) throws Exception {
        Object taskResult = "TASK-RESULT";
        MockFunction<Object> function = MockFunction.mock(taskResult);

        CompletionStage<Object> future
                = executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, toFunction(function));
        Object result = waitResult(future);

        assertSame(taskResult, result);
        return null;
    }

    @Test(timeout = 10000)
    public void testSubmitTaskWithCleanup() throws Exception {
        testAllCreated(this::testSubmitTaskWithCleanup);
    }

    private AfterTerminate testSubmitTaskWithCleanup(TaskExecutorService executor) throws Exception {
        Object taskResult = "TASK-RESULT";
        MockFunction<Object> function = MockFunction.mock(taskResult);

        CompletionStage<Object> future
                = executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, toFunction(function));
        Object result = waitResultWithCallback(future);

        assertSame(taskResult, result);
        return null;
    }

    private void testCleanups(boolean shutdown, boolean cancel) throws Exception {
        testAllCreated((executor) -> {
            return testCleanups(executor, shutdown, cancel);
        });
    }

    private AfterTerminate testCleanups(
            TaskExecutorService executor,
            boolean shutdown,
            boolean cancel) throws Exception {

        int taskCount = 100;

        TestCancellationSource cancelSource = newCancellationSource();
        MockCleanup[] mockCleanups = new MockCleanup[taskCount];
        List<CancelableFunction<Integer>> functions = new ArrayList<>(taskCount);
        List<BiConsumer<Integer, Throwable>> cleanups = new ArrayList<>(taskCount);

        CountDownLatch waitSignal = new CountDownLatch(taskCount);
        for (int i = 0; i < taskCount; i++) {
            mockCleanups[i] = mock(MockCleanup.class);
            doAnswer((invocation) -> {
                waitSignal.countDown();
                return null;
            }).when(mockCleanups[i]).cleanup(any(), any(Throwable.class));

            Integer taskResult = i;
            functions.add((cancelToken) -> taskResult);
            cleanups.add(toCleanupTask(mockCleanups[i]));
        }

        for (int i = 0; i < taskCount; i++) {
            CancelableFunction<Integer> function = functions.get(i);
            BiConsumer<Integer, Throwable> cleanup = cleanups.get(i);

            executor.executeFunction(cancelSource.getToken(), function)
                    .whenComplete(cleanup);
        }

        if (cancel) {
            cancelSource.getController().cancel();
        }

        if (shutdown) {
            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }

        if (!waitSignal.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("timeout");
        }

        for (int i = 0; i < taskCount; i++) {
            MockCleanup cleanup = mockCleanups[i];
            verifyResultOrCanceled(cleanup, i);
        }

        return () -> cancelSource.checkNoRegistration();
    }

    @Test(timeout = 10000)
    public void testShutdownWithCleanups() throws Exception {
        testCleanups(true, false);
    }

    @Test(timeout = 10000)
    public void testCanceledShutdownWithCleanups() throws Exception {
        testCleanups(true, true);
    }

    @Test(timeout = 10000)
    public void testCancellationWithCleanups() throws Exception {
        testCleanups(false, true);
    }

    @Test(timeout = 10000)
    public void testContextAwarenessInTask() throws Exception {
        testAllCreated(this::testContextAwarenessInTask);
    }

    private AfterTerminate testContextAwarenessInTask(TaskExecutorService executor) throws Exception {
        // TODOX: This should be moved from here to not run for executors not implementing MonitorableTaskExecutor.
        if (!(executor instanceof MonitorableTaskExecutor)) {
            return () -> { };
        }

        assertFalse("ExecutingInThis", ((MonitorableTaskExecutor)executor).isExecutingInThis());

        final WaitableSignal taskSignal = new WaitableSignal();
        final AtomicBoolean inContext = new AtomicBoolean(false);

        executor.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            inContext.set(((MonitorableTaskExecutor)executor).isExecutingInThis());
            taskSignal.signal();
        });

        taskSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue("ExecutingInThis", inContext.get());
        return null;
    }

    @Test(timeout = 10000)
    public void testToString() throws Exception {
        testAllCreated((executor) -> {
            assertNotNull(executor.toString());
            return null;
        });
    }

    @Test(timeout = 10000)
    public void testShutdownAndCancel() throws Exception {
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
    public void testSubmitTasksAfterShutdown() throws Exception {
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
    public void testAwaitTerminationTimeout() throws Exception {
        testAllCreated((executor) -> {
            assertFalse(executor.tryAwaitTermination(Cancellation.UNCANCELABLE_TOKEN, 100, TimeUnit.NANOSECONDS));
            return null;
        });
    }

    private AfterTerminate testConcurrentlyScheduled(TaskExecutorService executor) throws Exception {
        int threadCount = getThreadCount();
        int taskPerThread = 10;

        List<UnsafeRunnable> verifications = new ArrayList<>();

        Runnable[] scheduleTasks = new Runnable[threadCount];
        for (int i = 0; i < scheduleTasks.length; i++) {
            @SuppressWarnings("unchecked")
            MockFunction<Object>[] normalTasks = (MockFunction<Object>[])new MockFunction<?>[taskPerThread];
            MockCleanup[] threadNormalCleanups = new MockCleanup[taskPerThread];

            CancelableTask[] errorTasks = new CancelableTask[taskPerThread];
            MockCleanup[] threadErrorCleanups = new MockCleanup[taskPerThread];
            for (int j = 0; j < errorTasks.length; j++) {
                Object normalResult = "Test-Result-" + i + "-" + j;
                MockFunction<Object> normalTask = MockFunction.mock(normalResult);
                normalTasks[j] = normalTask;
                verifications.add(() -> {
                    verify(normalTask).execute(false);
                });

                CancelableTask errorTask = mock(CancelableTask.class);
                errorTasks[j] = errorTask;
                TestException taskError = new TestException();
                doThrow(taskError)
                        .when(errorTask)
                        .execute(any(CancellationToken.class));
                verifications.add(() -> {
                    verify(errorTask).execute(any(CancellationToken.class));
                });

                MockCleanup normalCleanup = mock(MockCleanup.class);
                threadNormalCleanups[j] = normalCleanup;
                verifications.add(() -> {
                    verify(normalCleanup).cleanup(same(normalResult), isNull(Throwable.class));
                });

                MockCleanup errorCleanup = mock(MockCleanup.class);
                threadErrorCleanups[j] = errorCleanup;
                verifications.add(() -> {
                    verify(errorCleanup).cleanup(isNull(), same(taskError));
                });
            }

            scheduleTasks[i] = () -> {
                for (int j = 0; j < taskPerThread; j++) {
                    executor.execute(Cancellation.UNCANCELABLE_TOKEN, errorTasks[j])
                            .whenComplete(toCleanupTask(threadErrorCleanups[j]));
                    executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, toFunction(normalTasks[j]))
                            .whenComplete(toCleanupTask(threadNormalCleanups[j]));
                }
            };
        }

        Tasks.runConcurrently(scheduleTasks);

        return () -> {
            for (UnsafeRunnable verification: verifications) {
                verification.run();
            }
        };
    }

    @Test(timeout = 30000)
    public void testConcurrentlyScheduled() throws Exception {
        testAllCreated(this::testConcurrentlyScheduled);
    }

    @Test(timeout = 10000)
    public void testTerminatedAfterAwaitTermination() throws Exception {
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
    public void testConcurrentShutdown() throws Exception {
        for (int i = 0; i < 100; i++) {
            testAllCreated(this::testConcurrentShutdown);
        }
    }

    public void verifyResultOrCanceled(MockCleanup cleanup, Object expectedResult) {
        ArgumentCaptor<Object> cleanupResult = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Throwable> cleanupError = ArgumentCaptor.forClass(Throwable.class);

        verify(cleanup).cleanup(cleanupResult.capture(), cleanupError.capture());

        Object result = cleanupResult.getValue();
        Throwable error = cleanupError.getValue();

        if (result == null) {
            assertTrue("If there is no result, must be canceled",
                    error instanceof OperationCanceledException);
        }
        else {
            assertSame("If not canceled expect result", expectedResult, result);
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

    private static TestCancellationSource newCancellationSource() {
        return new TestCancellationSource();
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    protected interface TestCreatedMethod {
        public AfterTerminate doTest(TaskExecutorService executor) throws Exception;
    }
}
