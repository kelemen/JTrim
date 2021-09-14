package org.jtrim2.jobprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.executor.CancelableFunction;
import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.jtrim2.executor.ExecutorConverter;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.executor.ThreadPoolTaskExecutor;
import org.jtrim2.logs.LogCollector;
import org.junit.Test;

import static org.jtrim2.jobprocessing.ProducerConsumerTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BackgroundWorkerManagerTest {

    @Test(timeout = 20000)
    public void testWaitForWorkersReturns() throws Exception {
        TestSetup test = new TestSetup(true);
        test.startWorkers(Cancellation.UNCANCELABLE_TOKEN, 1);
        test.runWorker(0);
        test.verifySuccessfulFinishedAll();
        test.waitForWorkers();
    }

    @Test
    public void testNormalFlow() throws Exception {
        TestSetup test = new TestSetup(true);

        test.startWorkers(Cancellation.UNCANCELABLE_TOKEN, 3);
        test.verifyUnfinished();
        test.assertFinishedWorker(0);

        test.runWorker(1);
        test.verifyUnfinished();
        test.assertFinishedWorker(1);

        test.runWorker(2);
        test.verifyUnfinished();
        test.assertFinishedWorker(2);

        test.runWorker(0);
        test.assertFinishedWorker(3);
        test.verifySuccessfulFinishedAll();
    }

    @Test
    public void testOneWorkerFails() throws Exception {
        TestSetup test = new TestSetup(true);
        test.setWorkerTask((cancelToken, index) -> {
            if (index == 1) {
                throw new TestException(Integer.toString(index));
            }
        });

        test.startWorkers(Cancellation.UNCANCELABLE_TOKEN, 3);
        test.verifyUnfinished();
        test.assertFinishedWorker(0);

        test.runAllWorkers();

        test.assertFinishedWorker(3);
        test.verifyFinishedAll(failures -> {
            if (failures.size() != 1) {
                AssertionError testError = new AssertionError("Expected single failure, but received: "
                        + failures.size());
                failures.forEach(failure -> testError.addSuppressed(failure));
                throw testError;
            }
            Throwable failure = failures.get(0);
            assertEquals(TestException.class, failure.getClass());
            assertEquals("exception-message", "1", failure.getMessage());
        });
    }

    @Test
    public void testFailureHandlerFails() throws Exception {
        try (LogCollector logs = LogCollector.startCollecting(BackgroundWorkerManager.class.getName())) {
            TestSetup test = new TestSetup(true);
            test.setWorkerTask((cancelToken, index) -> {
                throw new TestException(Integer.toString(index));
            });
            test.setFailureHandler(failure -> {
                throw new TestException("failure-handler", failure);
            });

            test.startWorkers(Cancellation.UNCANCELABLE_TOKEN, 3);
            test.verifyUnfinished();
            test.assertFinishedWorker(0);

            test.runAllWorkers();

            test.assertFinishedWorker(3);

            Set<String> expectedFailureMessages = new TreeSet<>(Arrays.asList("0", "1", "2"));

            test.verifyFinishedAll(failures -> {
                if (failures.size() != 3) {
                    AssertionError testError = new AssertionError("Expected 3 failures, but received: "
                            + failures.size());
                    failures.forEach(failure -> testError.addSuppressed(failure));
                    throw testError;
                }

                Set<String> receivedIds = new TreeSet<>();
                failures.forEach(failure -> {
                    receivedIds.add(failure.getMessage());
                });
                assertEquals(expectedFailureMessages, receivedIds);
            });

            Throwable[] loggedExceptions = logs.getExceptions(Level.SEVERE);
            assertEquals(3, loggedExceptions.length);
            Set<String> receivedIds = new TreeSet<>();
            for (Throwable failure: loggedExceptions) {
                assertEquals("failure-handler", failure.getMessage());
                Throwable cause = failure.getCause();
                assertNotNull("cause", cause);
                receivedIds.add(cause.getMessage());
            }
            assertEquals(new TreeSet<>(Arrays.asList("0", "1", "2")), receivedIds);
        }
    }

    @Test
    public void testCancellationDetection() throws Exception {
        CancellationSource cancel = Cancellation.createCancellationSource();
        TestSetup test = new TestSetup(true);

        AtomicReference<RuntimeException> detectionFailure = new AtomicReference<>();
        test.setWorkerTask((cancelToken, index) -> {
            cancel.getController().cancel();
            if (!cancelToken.isCanceled()) {
                detectionFailure.set(new RuntimeException("Expected cancellation detection."));
            }
        });
        test.startWorkers(cancel.getToken(), 3);

        test.runAllWorkers();
        test.assertFinishedWorker(1);
        test.verifyFinishedAll(failures -> {
            failures.forEach(failure -> {
                if (!(failure instanceof OperationCanceledException)) {
                    throw new AssertionError("expected cancellation, but received cause.", failure);
                }
            });
            assertEquals("failure-coutn", 2, failures.size());
        });
        verifyNoException(detectionFailure);
    }

    @Test(timeout = 30000)
    public void testConcurrentWorkers() throws Exception {
        int threadCount = 2 * Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor("test-executor", threadCount, 1);
        try {
            for (int i = 0; i < 20; i++) {
                TestSetup testSetup = new TestSetup(true);
                testSetup.setExecutor(executor);

                CountDownLatch sync = new CountDownLatch(threadCount);
                testSetup.setWorkerTask((cancelToken, workerIndex) -> {
                    sync.countDown();
                    try {
                        sync.await();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ex);
                    }
                });
                testSetup.startWorkers(Cancellation.UNCANCELABLE_TOKEN, threadCount);
                testSetup.waitForWorkers();
                testSetup.verifySuccessfulFinishedAll();
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    @Test
    public void testFailingExecutor() {
        TaskExecutor wrapped = ExecutorConverter.asTaskExecutor(command -> {
            throw new TestException();
        });
        TestSetup testSetup = new TestSetup(true);
        testSetup.setExecutor(wrapped);
        try {
            testSetup.startWorkers(Cancellation.UNCANCELABLE_TOKEN, 3);
            fail("Expected TestException");
        } catch (TestException ex) {
            // Expected
        }

        testSetup.verifyFinishedAll(failures -> {
            assertEquals("failures", Collections.emptyList(), failures);
        });
    }

    private static final class TestSetup {
        private TestExecutor manualExecutor;
        private ContextAwareTaskExecutor executorContext;

        private BackgroundWorkerManager workers;

        private final AtomicInteger workerCountRef;
        private final Runnable completionTask;
        private final List<Throwable> receivedFailures;
        private BiConsumer<? super CancellationToken, ? super Integer> workerTask;
        private Consumer<? super Throwable> failureHandler;

        private final AtomicReference<RuntimeException> outOfContextCallRef;

        public TestSetup(boolean eagerCancel) {
            this.manualExecutor = new TestExecutor(eagerCancel);
            this.executorContext = TaskExecutors.contextAware(manualExecutor);
            this.completionTask = mock(Runnable.class);
            this.receivedFailures = Collections.synchronizedList(new ArrayList<>());
            this.workerTask = (cancelToken, workerIndex) -> { };
            this.failureHandler = failure -> { };
            this.outOfContextCallRef = new AtomicReference<>();
            this.workerCountRef = new AtomicInteger(0);
            this.workers = null;
        }

        public void setWorkerTask(BiConsumer<? super CancellationToken, ? super Integer> workerTask) {
            this.workerTask = Objects.requireNonNull(workerTask, "workerTask");
        }

        public void setFailureHandler(Consumer<? super Throwable> failureHandler) {
            this.failureHandler = Objects.requireNonNull(failureHandler, "failureHandler");
        }

        public void setExecutor(TaskExecutor executor) {
            Objects.requireNonNull(executor, "executor");

            this.manualExecutor = null;
            this.executorContext = TaskExecutors.contextAware(executor);
        }

        public void startWorkers(CancellationToken cancelToken, int threadCount) {
            if (workers != null) {
                throw new AssertionError("Test workers can only be started once.");
            }

            workers = new BackgroundWorkerManager(executorContext, completionTask, failure -> {
                receivedFailures.add(failure);
                failureHandler.accept(failure);
            });
            workers.startWorkers(cancelToken, threadCount, taskCancelToken -> {
                int workerIndex = workerCountRef.getAndIncrement();
                if (!executorContext.isExecutingInThis()) {
                    setFirstException(outOfContextCallRef, "Worker " + workerIndex + " was called out of context");
                }
                workerTask.accept(taskCancelToken, workerIndex);
            });
        }

        public void runWorker(int workerIndex) {
            manualExecutor.releaseTask(workerIndex);
        }

        public void runAllWorkers() {
            manualExecutor.releaseAllTasks();
        }

        private List<Throwable> getCurrentReceivedFailures() {
            return Arrays.asList(receivedFailures.toArray(new Throwable[0]));
        }

        public void verifyUnfinished() {
            verifyZeroInteractions(completionTask);
            assertEquals("received-failures", Collections.emptyList(), getCurrentReceivedFailures());
            assertFalse("finishedAll", workers.isFinishedAll());
        }

        public void assertFinishedWorker(int expectedWorkerCount) {
            assertEquals("worker-count", expectedWorkerCount, workerCountRef.get());
        }

        public void verifySuccessfulFinishedAll() {
            verifyFinishedAll(failures -> {
                assertEquals("failure-count", 0, failures.size());
            });
        }


        public void verifyFinishedAll(Consumer<? super List<Throwable>> failuresCheck) {
            assertTrue("finishedAll", workers.isFinishedAll());
            verify(completionTask).run();
            failuresCheck.accept(getCurrentReceivedFailures());
            checkNoGeneralProblems();

        }

        public void waitForWorkers() {
            workers.waitForWorkers();
        }

        public void waitForFinishedWorkers() {
            assertTrue("finishedAll", workers.isFinishedAll());
            workers.waitForWorkers();
        }

        public void checkNoGeneralProblems() {
            verifyNoException(outOfContextCallRef);
        }
    }

    private static final class TestExecutor implements TaskExecutor {
        private final boolean eagerCancel;
        private final List<ManualTaskExecutor> wrappedExecutors;

        public TestExecutor(boolean eagerCancel) {
            this.eagerCancel = eagerCancel;
            this.wrappedExecutors = Collections.synchronizedList(new ArrayList<>());
        }

        public int getNumberOfStartedTasks() {
            return wrappedExecutors.size();
        }

        public void releaseTask(int index) {
            ManualTaskExecutor executor = wrappedExecutors.get(index);
            if (!executor.tryExecuteOne()) {
                throw new AssertionError("Already executed");
            }
        }

        public void releaseAllTasks() {
            wrappedExecutors.forEach(ManualTaskExecutor::tryExecuteOne);
        }

        private TaskExecutor forNewTask() {
            ManualTaskExecutor executor = new ManualTaskExecutor(eagerCancel);
            wrappedExecutors.add(executor);
            return executor;
        }

        @Override
        public <V> CompletionStage<V> executeFunction(
                CancellationToken cancelToken,
                CancelableFunction<? extends V> function) {

            return forNewTask().executeFunction(cancelToken, function);
        }

        @Override
        public void execute(Runnable command) {
            forNewTask().execute(command);
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestException() {
        }

        public TestException(String message) {
            super(message);
        }

        public TestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
