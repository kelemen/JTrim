package org.jtrim2.testutils.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.testutils.UnsafeConsumer;
import org.jtrim2.testutils.UnsafeRunnable;
import org.jtrim2.testutils.cancel.TestCancellationSource;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public abstract class GenericExecutorTests<E extends TaskExecutor>
extends
        AbstractExecutorTests<E> {

    private final boolean shutdownWaitsForTasks;

    public GenericExecutorTests(Collection<? extends TestExecutorFactory<? extends E>> factories) {
        this(true, factories);
    }

    public GenericExecutorTests(
            boolean shutdownWaitsForTasks,
            Collection<? extends TestExecutorFactory<? extends E>> factories) {

        super(factories);

        this.shutdownWaitsForTasks = shutdownWaitsForTasks;
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
        } else {
            assertSame("If not canceled expect result", expectedResult, result);
        }
    }

    private void addWaitTasksIfNeeded(List<Runnable> waitTasks, CompletionStage<?>... stages) {
        if (shutdownWaitsForTasks) {
            return;
        }

        for (CompletionStage<?> stage : stages) {
            WaitableSignal doneSignal = new WaitableSignal();
            stage.whenComplete((result, ex) -> doneSignal.signal());

            waitTasks.add(() -> doneSignal.waitSignal(Cancellation.UNCANCELABLE_TOKEN));
        }
    }

    private AfterTerminate testConcurrentlyScheduled(TaskExecutor executor) throws Exception {
        int threadCount = getThreadCount();
        int taskPerThread = 10;

        List<UnsafeRunnable> verifications = new ArrayList<>();
        List<Runnable> waitTasks = new ArrayList<>();

        Runnable[] scheduleTasks = new Runnable[threadCount];
        for (int i = 0; i < scheduleTasks.length; i++) {
            @SuppressWarnings("unchecked")
            MockFunction<Object>[] normalTasks = (MockFunction<Object>[]) new MockFunction<?>[taskPerThread];
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
                    CompletionStage<?> complete1 = executor
                            .execute(Cancellation.UNCANCELABLE_TOKEN, errorTasks[j])
                            .whenComplete(toCleanupTask(threadErrorCleanups[j]));

                    CompletionStage<?> complete2 = executor
                            .executeFunction(Cancellation.UNCANCELABLE_TOKEN, toFunction(normalTasks[j]))
                            .whenComplete(toCleanupTask(threadNormalCleanups[j]));

                    addWaitTasksIfNeeded(waitTasks, complete1, complete2);
                }
            };
        }

        Tasks.runConcurrently(scheduleTasks);
        waitTasks.forEach(Runnable::run);

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

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
