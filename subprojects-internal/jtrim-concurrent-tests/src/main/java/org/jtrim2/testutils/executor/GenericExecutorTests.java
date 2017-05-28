package org.jtrim2.testutils.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.testutils.JTrimTests;
import org.jtrim2.testutils.UnsafeConsumer;
import org.jtrim2.testutils.UnsafeFunction;
import org.jtrim2.testutils.UnsafeRunnable;
import org.jtrim2.testutils.cancel.TestCancellationSource;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;


public abstract class GenericExecutorTests<E extends TaskExecutor> extends JTrimTests<TestExecutorFactory<E>> {
    public GenericExecutorTests(Collection<? extends TestExecutorFactory<E>> factories) {
        super(factories);
    }

    public static <E extends TaskExecutor, W extends TaskExecutorService> TestExecutorFactory<E> wrappedExecutor(
            Supplier<? extends W> wrappedFactory,
            Function<? super W, ? extends E> wrapperFactory) {
        Objects.requireNonNull(wrappedFactory, "wrapped");
        Objects.requireNonNull(wrapperFactory, "wrapperFactory");

        return new TestExecutorFactory<>(() -> {
            W wrappedExecutor = wrappedFactory.get();
            E executor = wrapperFactory.apply(wrappedExecutor);
            return new TestExecutorFactory.ExecutorRef<>(executor, () -> {
                GenericExecutorServiceTests.shutdownTestExecutor(wrappedExecutor);
            });
        });
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

    public static <V> V waitResult(CompletionStage<V> future) {
        return CompletionStages.get(future.toCompletableFuture(), 10, TimeUnit.SECONDS);
    }

    public static <V> V waitResultWithCallback(CompletionStage<V> future) {
        CompletableFuture<V> waitableFuture = CompletionStages.toSafeWaitable(future);
        return CompletionStages.get(waitableFuture, 10, TimeUnit.SECONDS);
    }

    public static void verifyResultOrCanceled(MockCleanup cleanup, Object expectedResult) {
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

    private AfterTerminate testConcurrentlyScheduled(TaskExecutor executor) throws Exception {
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
    public final void testConcurrentlyScheduled() throws Exception {
        testAllCreated(this::testConcurrentlyScheduled);
    }

    @Test(timeout = 10000)
    public final void testSubmitTaskNoCleanup() throws Exception {
        testAllCreated(this::testSubmitTaskNoCleanup);
    }

    private AfterTerminate testSubmitTaskNoCleanup(TaskExecutor executor) throws Exception {
        Object taskResult = "TASK-RESULT";
        MockFunction<Object> function = MockFunction.mock(taskResult);

        CompletionStage<Object> future
                = executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, toFunction(function));
        Object result = waitResult(future);

        assertSame(taskResult, result);
        return null;
    }

    @Test(timeout = 10000)
    public final void testContextAwarenessInTask() throws Exception {
        testAllCreated(this::testContextAwarenessInTask);
    }

    private AfterTerminate testContextAwarenessInTask(TaskExecutor executor) throws Exception {
        // TODOX: This should be moved from here to not run for executors not implementing MonitorableTaskExecutor.
        if (!(executor instanceof ContextAwareTaskExecutor)) {
            return () -> { };
        }

        ContextAwareTaskExecutor contextAware = (ContextAwareTaskExecutor)executor;

        assertFalse("ExecutingInThis", contextAware.isExecutingInThis());

        final WaitableSignal taskSignal = new WaitableSignal();
        final AtomicBoolean inContext = new AtomicBoolean(false);

        contextAware.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
            inContext.set(contextAware.isExecutingInThis());
            taskSignal.signal();
        });

        taskSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN);
        assertTrue("ExecutingInThis", inContext.get());
        return null;
    }

    @Test(timeout = 10000)
    public final void testSubmitTaskWithCleanup() throws Exception {
        testAllCreated(this::testSubmitTaskWithCleanup);
    }

    private AfterTerminate testSubmitTaskWithCleanup(TaskExecutor executor) throws Exception {
        Object taskResult = "TASK-RESULT";
        MockFunction<Object> function = MockFunction.mock(taskResult);

        CompletionStage<Object> future
                = executor.executeFunction(Cancellation.UNCANCELABLE_TOKEN, toFunction(function));
        Object result = waitResultWithCallback(future);

        assertSame(taskResult, result);
        return null;
    }

    @Test(timeout = 10000)
    public final void testToString() throws Exception {
        testAllCreated((executor) -> {
            assertNotNull(executor.toString());
            return null;
        });
    }

    private static TestCancellationSource newCancellationSource() {
        return new TestCancellationSource();
    }

    protected final void testCleanups(
            boolean cancel,
            UnsafeConsumer<? super E> beforeVerify) throws Exception {

        testAllCreated((executor) -> {
            return testCleanups(executor, cancel, beforeVerify);
        });
    }

    private AfterTerminate testCleanups(
            E executor,
            boolean cancel,
            UnsafeConsumer<? super E> beforeVerify) throws Exception {

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

        beforeVerify.accept(executor);

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
    public final void testCancellationWithCleanups() throws Exception {
        testCleanups(false, (executor) -> { });
    }

    protected final void testAllCreated(
            UnsafeFunction<? super E, ? extends AfterTerminate> testMethod) throws Exception {

        testAll((factory) -> {
            AfterTerminate afterTerminateVerification = factory.runTest(testMethod);
            if (afterTerminateVerification != null) {
                afterTerminateVerification.verifyAfterTerminate();
            }
        });
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
