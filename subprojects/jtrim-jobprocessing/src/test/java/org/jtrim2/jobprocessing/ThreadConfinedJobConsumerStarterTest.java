package org.jtrim2.jobprocessing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jtrim2.cancel.CancelableWaits;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.concurrent.TaskExecutionException;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.jtrim2.executor.ContextAwareTaskExecutorService;
import org.jtrim2.executor.ExecutorConverter;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.executor.ThreadPoolTaskExecutor;
import org.jtrim2.testutils.TestParameterRule;
import org.jtrim2.testutils.UnsafeConsumer;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Rule;
import org.junit.Test;

import static org.jtrim2.jobprocessing.ProducerConsumerTestUtils.*;
import static org.junit.Assert.*;

public class ThreadConfinedJobConsumerStarterTest {
    @Rule
    public final TestParameterRule managedExecutorRule = new TestParameterRule(
            ExecutorManagement.class,
            Arrays.asList(false, true),
            this::setManagedExecutor
    );

    @Rule
    public final TestParameterRule queueSizeRule = new TestParameterRule(
            ConcurrencyParameters.class,
            Arrays.asList(0, 1, Runtime.getRuntime().availableProcessors()),
            this::setQueueSize
    );

    @Rule
    public final TestParameterRule consumerThreadCountRule = new TestParameterRule(
            ConcurrencyParameters.class,
            Arrays.asList(1, 2 * Runtime.getRuntime().availableProcessors()),
            this::setConsumerThreadCount
    );

    @Rule
    public final TestParameterRule jobCountRule = new TestParameterRule(
            ConcurrencyParameters.class,
            Arrays.asList(1, 2, 2 * Runtime.getRuntime().availableProcessors()),
            this::setJobCount
    );

    @Rule
    public final TestParameterRule submitTasksLogicRule = new TestParameterRule(
            SubmitTaskLogic.class,
            Arrays.asList(SubTaskLogicImpl.values()),
            this::setSubmitTasksLogic
    );

    @Rule
    public final TestParameterRule testExceptionTypeRule = new TestParameterRule(
            TestExceptionType.class,
            Arrays.asList(TestException.class, OperationCanceledException.class),
            this::setTestExceptionType
    );

    private boolean managedExecutor = false;
    private int queueSize = 1;
    private int consumerThreadCount = 1;
    private int jobCount = 1;
    private SubTaskLogicImpl submitTasksLogic = SubTaskLogicImpl.SYNC;
    private Class<? extends RuntimeException> testExceptionType = TestException.class;

    private void setManagedExecutor(boolean managedExecutor) {
        this.managedExecutor = managedExecutor;
    }

    private void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    private void setConsumerThreadCount(int consumerThreadCount) {
        this.consumerThreadCount = consumerThreadCount;
    }

    private void setJobCount(int jobCount) {
        this.jobCount = jobCount;
    }

    private void setSubmitTasksLogic(SubTaskLogicImpl submitTasksLogic) {
        this.submitTasksLogic = submitTasksLogic;
    }

    public void setTestExceptionType(Class<? extends RuntimeException> testExceptionType) {
        this.testExceptionType = testExceptionType;
    }

    private void verifySuccessfulCompletion(
            TestConsumerFactory consumerFactory,
            List<String> jobs) {

        consumerFactory.verifyStartedAllThreads();
        consumerFactory.verifyCompletion((id, status) -> assertTrue("status-success", status.isSuccess()));

        consumerFactory.checkNoGeneralProblems();
        consumerFactory.expectJobsWithoutOrder(jobs);
    }

    private void setupDefaults(TestConsumerFactory consumerFactory) {
        consumerFactory.setManagedExecutor(managedExecutor);
        consumerFactory.setQueueSize(queueSize);
        consumerFactory.setThreadCount(consumerThreadCount);
    }

    @SubmitTaskLogic
    @ExecutorManagement
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testNormalSingleProducerRun() throws Exception {
        List<String> jobs = testStrings(jobCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setSyncConsumerThreads(true);

            StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(Cancellation.UNCANCELABLE_TOKEN);
            try {
                submitTasksLogic.submitJobs(consumer, jobs);
            } finally {
                consumer.finishProcessing(ConsumerCompletionStatus.SUCCESS);
            }

            verifySuccessfulCompletion(consumerFactory, jobs);
        }
    }

    @Test(timeout = 30000)
    public void testNormalRunButIllegalFinishProcessing() throws Exception {
        List<String> jobs = testStrings(jobCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(Cancellation.UNCANCELABLE_TOKEN);
            NullPointerException expectedFailure;
            try {
                submitTasksLogic.submitJobs(consumer, jobs);
            } finally {
                try {
                    consumer.finishProcessing(null);
                    fail("Expected NullPointerException");
                    expectedFailure = null; // Pleasing the compiler
                } catch (NullPointerException ex) {
                    expectedFailure = ex;
                }
            }

            consumerFactory.verifyStartedAllThreads();
            NullPointerException expectedFailureCapture = expectedFailure;
            consumerFactory.verifyCompletion((id, status) -> {
                assertEquals("status-error", expectedFailureCapture, status.tryGetError());
            });

            consumerFactory.checkNoGeneralProblems();
            consumerFactory.expectJobsWithoutOrder(jobs);
        }
    }

    @Test(timeout = 30000)
    public void testFailedRunAndIllegalFinishProcessing() throws Exception {
        List<String> jobs = testStrings(jobCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {

            consumerFactory.setJobHandler((cancelToken, job) -> {
                throw new TestException();
            });

            StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(Cancellation.UNCANCELABLE_TOKEN);
            try {
                submitTasksLogic.submitJobs(consumer, jobs);
            } finally {
                try {
                    consumer.finishProcessing(null);
                    fail("Expected NullPointerException");
                } catch (NullPointerException ex) {
                    List<Class<?>> suppressedTypes = Stream
                            .of(ex.getSuppressed())
                            .map(Object::getClass)
                            .collect(Collectors.toList());
                    assertEquals("status-error-suppressed",
                            Collections.singletonList(TestException.class),
                            suppressedTypes
                    );
                }
            }

            consumerFactory.verifyStartedAllThreads();
            consumerFactory.verifyCompletion((id, status) -> {
                Throwable failure = status.tryGetError();
                assertNotNull("status-error", failure);
                assertEquals("status-error", TestException.class, failure.getClass());
            });

            consumerFactory.checkNoGeneralProblems();
            consumerFactory.expectJobsWithoutOrder(jobs);
        }
    }

    @Test(timeout = 30000)
    public void testNormalRunWithLateSubmit() throws Exception {
        List<String> jobs = testStrings(jobCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setSyncConsumerThreads(true);

            StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(Cancellation.UNCANCELABLE_TOKEN);
            submitTasksLogic.submitJobsAndComplete(consumer, jobs, ConsumerCompletionStatus.SUCCESS);

            verifySuccessfulCompletion(consumerFactory, jobs);

            try {
                consumer.processJob("X");
                fail("Expected IllegalStateException");
            } catch (IllegalStateException ex) {
                // Expected
            }
        }
    }

    private void verifyCompletelyCanceledCompletion(TestConsumerFactory consumerFactory) {
        consumerFactory.verifyStartedThreadCount(0);
        consumerFactory.verifyCompletion((id, status) -> assertTrue("status-canceled", status.isCanceled()));

        consumerFactory.checkNoGeneralProblems();
        consumerFactory.expectJobsWithoutOrder(Collections.emptyList());
    }

    @ExecutorManagement
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testImmediateCancellation() throws Exception {
        List<String> jobs = testStrings(jobCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);

            StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(Cancellation.CANCELED_TOKEN);
            try {
                SubTaskLogicImpl.SYNC.submitJobsAndComplete(consumer, jobs, ConsumerCompletionStatus.SUCCESS);
                fail("Expected OperationCanceledException");
            } catch (OperationCanceledException ex) {
                // Expected
            }

            verifyCompletelyCanceledCompletion(consumerFactory);
        }
    }

    @ExecutorManagement
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testSuccessButCancelOnFinish() throws Exception {
        List<String> jobs = testStrings(jobCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);

            StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(Cancellation.UNCANCELABLE_TOKEN);
            SubTaskLogicImpl.SYNC.submitJobsAndComplete(consumer, jobs, ConsumerCompletionStatus.CANCELED);

            consumerFactory.verifyStartedAllThreads();
            consumerFactory.verifyCompletion((id, status) -> assertTrue("status-canceled", status.isCanceled()));

            consumerFactory.checkNoGeneralProblems();
            consumerFactory.expectJobsWithoutOrder(jobs);
        }
    }

    @ExecutorManagement
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testCancellationOnStartup() throws Exception {
        CancellationSource cancel = Cancellation.createCancellationSource();
        List<String> jobs = testStrings(jobCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setSyncConsumerThreads(true);

            AtomicReference<RuntimeException> cancelNotDetectedRef = new AtomicReference<>();
            consumerFactory.setStartHandler(cancelToken -> {
                cancel.getController().cancel();
                if (!cancelToken.isCanceled()) {
                    setFirstException(cancelNotDetectedRef, "Missed cancellation.");
                }
                throw new OperationCanceledException();
            });

            try {
                StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(cancel.getToken());
                SubTaskLogicImpl.SYNC.submitJobsAndComplete(consumer, jobs, ConsumerCompletionStatus.SUCCESS);
                fail("Expected OperationCanceledException");
            } catch (OperationCanceledException ex) {
                // Expected
            }

            consumerFactory.verifyStartedAllThreads();
            verifyNoException(cancelNotDetectedRef);
            consumerFactory.checkNoGeneralProblems();
        }
    }

    @SubmitTaskLogic
    @TestExceptionType
    @ExecutorManagement
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testCancellationButFailedResult() throws Exception {
        for (int i = 0; i < 15; i++) {
            testCancellationButFailedResult0();
        }
    }

    private void testCancellationButFailedResult0() throws Exception {
        CancellationSource cancel = Cancellation.createCancellationSource();
        List<String> jobs = testStrings(jobCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setSyncConsumerThreads(true);

            consumerFactory.setJobHandler((cancelToken, job) -> {
                cancel.getController().cancel();
                throw new OperationCanceledException();
            });

            TestException finalFailure = new TestException();

            try {
                StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(cancel.getToken());
                SubTaskLogicImpl.SYNC
                        .submitJobsAndComplete(consumer, jobs, ConsumerCompletionStatus.failed(finalFailure));
                fail("Expected OperationCanceledException");
            } catch (OperationCanceledException ex) {
                // Expected
            }

            consumerFactory.verifyStartedAllThreads();
            consumerFactory.checkNoGeneralProblems();
            consumerFactory.verifyCompletion((id, status) -> {
                assertFalse("!status-success", status.isSuccess());
                status.getErrorIfFailed().ifPresent(failure -> {
                    assertEquals("cause", finalFailure, failure);
                });
            });
        }
    }

    @ExecutorManagement
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testCancellationIsDelivered() throws Exception {
        for (int i = 0; i < 20; i++) {
            testCancellationIsDelivered0();
        }
    }

    private void testCancellationIsDelivered0() throws Exception {
        CancellationSource cancel = Cancellation.createCancellationSource();

        List<String> jobs = testStrings(jobCount);
        AtomicReference<RuntimeException> cancelNotDetectedRef = new AtomicReference<>();
        Set<Long> threadsCanceled = Collections.newSetFromMap(new ConcurrentHashMap<>());

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setSyncConsumerThreads(true);

            consumerFactory.setJobHandler((cancelToken, job) -> {
                cancel.getController().cancel();
                if (!cancelToken.isCanceled()) {
                    setFirstException(cancelNotDetectedRef, "Missed cancellation for " + job);
                }
                threadsCanceled.add(Thread.currentThread().getId());
                throw new OperationCanceledException("canceled-" + job);
            });

            StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(cancel.getToken());
            int processedJobCount = 0;
            try {
                try {
                    for (String job: jobs) {
                        consumer.processJob(job);
                        processedJobCount++;
                    }
                } finally {
                    consumer.finishProcessing(ConsumerCompletionStatus.SUCCESS);
                }
                fail("Expected OperationCanceledException");
            } catch (OperationCanceledException ex) {
                // expected
            }

            int[] cancelStatusRef = new int[1];
            int[] successStatusRef = new int[1];

            consumerFactory.verifyCompletion((id, status) -> {
                if (status.isSuccess()) {
                    successStatusRef[0]++;
                } else if (status.isCanceled()) {
                    cancelStatusRef[0]++;
                } else {
                    throw new AssertionError(
                            "Received unexpected status: " + status + " for thread: " + id,
                            status.tryGetError()
                    );
                }
            });

            int cancelCount = cancelStatusRef[0];
            if (processedJobCount >= consumerThreadCount) {
                int expectedCancelCount = threadsCanceled.size();
                assertEquals("cancel-count", expectedCancelCount, cancelCount);
            } else {
                int expectedMinCancelCount = processedJobCount;
                if (cancelCount < expectedMinCancelCount) {
                    throw new AssertionError("Expected at least " + expectedMinCancelCount
                            + ", but received: " + cancelCount);
                }
            }

            int expectedSuccessCount = consumerThreadCount - cancelCount;
            assertEquals("success-count", expectedSuccessCount, successStatusRef[0]);

            verifyNoException(cancelNotDetectedRef);
            consumerFactory.checkNoGeneralProblems();
        }
    }

    private static Class<? extends Throwable> tryGetCauseClass(Throwable ex) {
        Throwable cause = ex.getCause();
        return cause != null ? cause.getClass() : null;
    }

    @TestExceptionType
    @Test(timeout = 30000)
    public void testFailedStart() throws Exception {
        List<String> jobs = testStrings(10);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            consumerFactory.setThreadCount(2);
            consumerFactory.setQueueSize(2);
            consumerFactory.setSyncConsumerThreads(true);

            AtomicReference<RuntimeException> unexpectedTestError = new AtomicReference<>();
            consumerFactory.setStartHandler(cancelToken -> {
                RuntimeException startFailure;
                try {
                    startFailure = testExceptionType.newInstance();
                } catch (Throwable ex) {
                    setFirstException(unexpectedTestError, "Unexpected exception creation failure.");
                    throw new AssertionError(ex);
                }
                throw startFailure;
            });

            try {
                StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(Cancellation.UNCANCELABLE_TOKEN);
                SubTaskLogicImpl.SYNC.submitJobsAndComplete(consumer, jobs, ConsumerCompletionStatus.SUCCESS);
                fail("Expected job processing exception");
            } catch (JobProcessingException ex) {
                assertEquals(testExceptionType, tryGetCauseClass(ex));
            }

            verifyNoException(unexpectedTestError);
            consumerFactory.checkNoGeneralProblems();
        }
    }

    @Test(timeout = 30000)
    public void testFailedExecutor() throws Exception {
        List<String> jobs = testStrings(10);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            consumerFactory.setThreadCount(2);
            consumerFactory.setQueueSize(2);
            consumerFactory.setSyncConsumerThreads(false);
            consumerFactory.setFailingExecutor(true);

            try {
                StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(Cancellation.UNCANCELABLE_TOKEN);
                SubTaskLogicImpl.SYNC.submitJobsAndComplete(consumer, jobs, ConsumerCompletionStatus.SUCCESS);
                fail("Expected TestException");
            } catch (TestException ex) {
                // Expected
            }

            consumerFactory.checkNoGeneralProblems();
        }
    }

    private void testFailSingleJob(
            ConsumerCompletionStatus completionStatus,
            UnsafeConsumer<? super TestConsumerFactory> finalValidations) throws Exception {

        List<String> jobs = testStrings(jobCount);

        String jobToFail = Integer.toString(Math.min(3, jobCount - 1));
        AtomicReference<JobFailureDef> jobFailureDefRef = new AtomicReference<>();
        AtomicReference<RuntimeException> unexpectedTestError = new AtomicReference<>();

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setSyncConsumerThreads(true);

            consumerFactory.setJobHandler((cancelToken, job) -> {
                if (jobToFail.equals(job)) {
                    jobFailureDefRef.set(new JobFailureDef(jobToFail, Thread.currentThread().getId()));

                    RuntimeException jobFailure;
                    try {
                        jobFailure = testExceptionType.getConstructor(String.class).newInstance(job);
                    } catch (Throwable ex) {
                        setFirstException(unexpectedTestError, "Unexpected exception creation failure.");
                        throw new RuntimeException(ex);
                    }
                    throw jobFailure;
                }
            });

            try {
                StatefulJobConsumer<String> consumer = consumerFactory.startTestedConsumer(Cancellation.UNCANCELABLE_TOKEN);
                submitTasksLogic.submitJobsAndComplete(consumer, jobs, completionStatus);
                fail("Expected JobProcessingException");
            } catch (JobProcessingException ex) {
                assertEquals(testExceptionType, tryGetCauseClass(ex));
                assertEquals("job-fail-message", jobToFail, ex.getCause().getMessage());
            }

            verifyNoException(unexpectedTestError);

            consumerFactory.checkNoGeneralProblems();
            consumerFactory.verifyStartedAllThreads();

            JobFailureDef jobFailureDef = jobFailureDefRef.get();
            assertNotNull("jobFailureDef", jobFailureDef);

            AtomicReference<ConsumerCompletionStatus> failedJobStatusRef = new AtomicReference<>();

            consumerFactory.verifyCompletion((id, status) -> {
                if (jobFailureDef.getJobThreadId() == id) {
                    failedJobStatusRef.set(status);
                }
            });

            ConsumerCompletionStatus failedJobStatus = failedJobStatusRef.get();
            assertNotNull("failedJobStatus", failedJobStatus);

            Throwable reportedJobFailure = failedJobStatus.tryGetError();
            assertNotNull("failedJobFailure", reportedJobFailure);
            assertEquals("failedJobFailure.class", testExceptionType, reportedJobFailure.getClass());
            assertEquals("failedJobFailure.message", jobToFail, reportedJobFailure.getMessage());
            assertTrue("failedJobFailure.failed", failedJobStatus.isFailed());

            finalValidations.accept(consumerFactory);
        }
    }

    @SubmitTaskLogic
    @TestExceptionType
    @ExecutorManagement
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testFailSingleJobAndSuccessfulFinish() throws Exception {
        testFailSingleJob(ConsumerCompletionStatus.SUCCESS, consumerFactory -> {
            consumerFactory.verifyCompletion((id, status) -> {
                status.getErrorIfFailed().ifPresent(failure -> {
                    assertEquals("failedJobFailure.class", testExceptionType, failure.getClass());
                });
                if (status.isCanceled()) {
                    throw new AssertionError("Unexpected cancellation.");
                }
            });
        });
    }

    @SubmitTaskLogic
    @TestExceptionType
    @ExecutorManagement
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testFailSingleJobAndCanceledFinish() throws Exception {
        testFailSingleJob(ConsumerCompletionStatus.CANCELED, consumerFactory -> {
            consumerFactory.verifyCompletion((id, status) -> {
                status.getErrorIfFailed().ifPresent(failure -> {
                    assertEquals("failedJobFailure.class", testExceptionType, failure.getClass());
                });
            });
        });
    }

    @SubmitTaskLogic
    @ExecutorManagement
    @TestExceptionType
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testFailSingleJobAndFailedFinish() throws Exception {
        RuntimeException finalFailure = new RuntimeException();
        ConsumerCompletionStatus finalStatus = ConsumerCompletionStatus.failed(finalFailure);
        testFailSingleJob(finalStatus, consumerFactory -> {
            consumerFactory.verifyCompletion((id, status) -> {
                status.getErrorIfFailed().ifPresent(failure -> {
                    if (finalFailure != failure) {
                        assertEquals("failedJobFailure.class", testExceptionType, failure.getClass());
                    }
                });
                if (status.isCanceled()) {
                    throw new AssertionError("Unexpected cancellation.");
                }
            });
        });
    }

    private static final class TestConsumerFactory implements StatefulJobConsumerStarter<String>, AutoCloseable {
        private final CancellationSource testCancellation;
        private TaskExecutorService executor;
        private ContextAwareTaskExecutor executorContext;
        private boolean managedExecutor;
        private int threadCount;
        private boolean syncConsumerThreads;
        private int queueSize;
        private boolean failingExecutor;

        private final AtomicReference<RuntimeException> outOfContextCallRef;
        private final AtomicReference<RuntimeException> threadConfinementBreachRef;
        private final AtomicReference<RuntimeException> accessAfterFinishRef;
        private final AtomicReference<RuntimeException> finishCallErrorRef;
        private volatile boolean finished;
        private final Collection<String> received;

        private Consumer<? super CancellationToken> startHandler;
        private BiConsumer<? super CancellationToken, ? super String> jobHandler;
        private Consumer<? super ConsumerCompletionStatus> finishHandler;

        private CountDownLatch startLatch;
        private final AtomicLong startedThreadCount;
        private final ConcurrentMap<Long, ConsumerCompletionStatus> resultRefs;
        private StatefulJobConsumerStarter<String> wrappedFactory;

        public TestConsumerFactory() {
            this.testCancellation = Cancellation.createCancellationSource();
            this.outOfContextCallRef = new AtomicReference<>();
            this.threadConfinementBreachRef = new AtomicReference<>();
            this.accessAfterFinishRef = new AtomicReference<>();
            this.finishCallErrorRef = new AtomicReference<>();
            this.finished = false;
            this.received = new ConcurrentLinkedQueue<>();
            this.startHandler = token -> { };
            this.jobHandler = (token, job) -> { };
            this.finishHandler = status -> { };
            this.startedThreadCount = new AtomicLong(0);
            this.resultRefs = new ConcurrentHashMap<>();
            this.threadCount = 1;
            this.syncConsumerThreads = false;
            this.queueSize = 0;
            this.failingExecutor = false;
        }

        public void setManagedExecutor(boolean managedExecutor) {
            this.managedExecutor = managedExecutor;
        }

        public void setThreadCount(int threadCount) {
            this.threadCount = threadCount;
        }

        public void setSyncConsumerThreads(boolean syncConsumerThreads) {
            this.syncConsumerThreads = syncConsumerThreads;
        }

        public void setQueueSize(int queueSize) {
            this.queueSize = queueSize;
        }

        public void setFailingExecutor(boolean failingExecutor) {
            this.failingExecutor = failingExecutor;
        }

        public void setStartHandler(Consumer<? super CancellationToken> startHandler) {
            this.startHandler = Objects.requireNonNull(startHandler, "startHandler");
        }

        public void setJobHandler(BiConsumer<? super CancellationToken, ? super String> jobHandler) {
            this.jobHandler = Objects.requireNonNull(jobHandler, "jobHandler");
        }

        public void setFinishHandler(Consumer<? super ConsumerCompletionStatus> finishHandler) {
            this.finishHandler = Objects.requireNonNull(finishHandler, "finishHandler");
        }

        private Supplier<ExecutorRef> getExecutorRef() {
            if (managedExecutor) {
                return ExecutorRef.owned(() -> executor);
            } else {
                return ExecutorRef.external(executor);
            }
        }

        private void checkContext(long expectedId) {
            if (executorContext.isExecutingInThis()) {
                if (expectedId != Thread.currentThread().getId()) {
                    setFirstException(
                            threadConfinementBreachRef,
                            "Running on appropriate executor, but breached thread confinement."
                    );
                }
            } else {
                setFirstException(outOfContextCallRef, "Consumer was called out of context.");
            }
        }

        @Override
        public StatefulJobConsumer<String> startConsumer(CancellationToken cancelToken) throws Exception {
            startedThreadCount.incrementAndGet();

            long expectedId = Thread.currentThread().getId();
            checkContext(expectedId);

            if (syncConsumerThreads) {
                startLatch.countDown();
                CancelableWaits.await(testCancellation.getToken(), startLatch::await);
            }

            startHandler.accept(cancelToken);

            return new StatefulJobConsumer<String>() {
                @Override
                public void processJob(String job) throws Exception {
                    checkContext(expectedId);
                    if (finished) {
                        accessAfterFinishRef.set(new RuntimeException("processJob after finishProcessing."));
                    }
                    received.add(job);
                    jobHandler.accept(cancelToken, job);
                }

                @Override
                public void finishProcessing(ConsumerCompletionStatus finalStatus) throws Exception {
                    checkContext(expectedId);

                    if (finalStatus == null) {
                        setFirstException(finishCallErrorRef, "Finished with null.");
                        return;
                    }

                    if (resultRefs.putIfAbsent(Thread.currentThread().getId(), finalStatus) != null) {
                        setFirstException(
                                finishCallErrorRef,
                                "Multiple finish processing calls: " + finalStatus,
                                finalStatus.getErrorIfFailed().orElse(null)
                        );
                    }

                    finishHandler.accept(finalStatus);
                }
            };
        }

        private void initExecutor() {
            if (failingExecutor) {
                TaskExecutor wrapped = ExecutorConverter.asTaskExecutor(command -> {
                    throw new TestException();
                });
                executorContext = TaskExecutors.contextAware(TaskExecutors.upgradeToStoppable(wrapped));
                executor = TaskExecutors.upgradeToStoppable(executorContext);
            } else {
                ContextAwareTaskExecutorService threadPool = new ThreadPoolTaskExecutor(
                        "ThreadConfinedJobConsumerFactoryTest-consumer",
                        threadCount
                );
                executorContext = threadPool;
                executor = threadPool;
            }
        }

        public StatefulJobConsumer<String> startTestedConsumer(CancellationToken cancelToken) throws Exception {
            if (wrappedFactory != null) {
                throw new AssertionError("This is a single use object for testing.");
            }

            startLatch = new CountDownLatch(threadCount);
            initExecutor();

            wrappedFactory = new ThreadConfinedJobConsumerStarter<>(
                    getExecutorRef(),
                    threadCount,
                    queueSize,
                    this
            );

            return wrappedFactory.startConsumer(cancelToken);
        }

        public List<String> getReceivedJobs() {
            return Arrays.asList(received.toArray(new String[0]));
        }

        public void expectJobsWithoutOrder(Collection<? extends String> expectedJobs) {
            List<String> expectedjobsSorted = new ArrayList<>(expectedJobs);
            expectedjobsSorted.sort(null);

            List<String> receivedJobs = new ArrayList<>(getReceivedJobs());
            receivedJobs.sort(null);

            assertEquals(expectedjobsSorted, receivedJobs);
        }

        public void checkNoGeneralProblems() {
            verifyNoException(outOfContextCallRef);
            verifyNoException(threadConfinementBreachRef);
            verifyNoException(accessAfterFinishRef);
            verifyNoException(finishCallErrorRef);
        }

        public void verifyStartedAllThreads() {
            verifyStartedThreadCount(threadCount);
        }

        public void verifyStartedThreadCount(int expectedThreadCount) {
            assertEquals("Started thread count", expectedThreadCount, startedThreadCount.get());
        }

        public void verifyCompletion(BiConsumer<? super Long, ? super ConsumerCompletionStatus> statusChecker) {
            assertEquals("finish count", startedThreadCount.get(), resultRefs.size());
            resultRefs.forEach(statusChecker);
        }

        @Override
        public void close() {
            boolean wasShutdown = executor.isShutdown();
            boolean wasTerminated = executor.isTerminated();

            testCancellation.getController().cancel();

            if (!wasTerminated) {
                executor.shutdownAndCancel();
                executor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
                if (managedExecutor) {
                    throw new AssertionError("Executor should have been terminated by the consumer.");
                }
            }

            if (!managedExecutor && wasShutdown) {
                throw new AssertionError("Unmanaged executor was terminated.");
            }
        }
    }

    private static final class JobFailureDef {
        private final String jobName;
        private final long threadId;

        public JobFailureDef(String jobName, long threadId) {
            this.jobName = Objects.requireNonNull(jobName, "jobName");
            this.threadId = threadId;
        }

        public boolean isSameJob(String candidateJob) {
            return jobName.equals(candidateJob);
        }

        public long getJobThreadId() {
            return threadId;
        }
    }

    private static class TestException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestException() {
        }

        public TestException(String message) {
            super(message);
        }
    }

    private enum SubTaskLogicImpl {
        SYNC {
            @Override
            public void submitJobs(StatefulJobConsumer<String> consumer, List<String> jobs) throws Exception {
                for (String job: jobs) {
                    consumer.processJob(job);
                }
            }
        },
        ALL_JOBS_CONCURRENT {
            @Override
            public void submitJobs(StatefulJobConsumer<String> consumer, List<String> jobs) throws Exception {
                List<Runnable> jobSubmitters = jobs
                        .stream()
                        .map(job -> jobProcessTask(consumer, job))
                        .collect(Collectors.toList());

                try {
                    Tasks.runConcurrently(jobSubmitters);
                } catch (TaskExecutionException ex) {
                    Throwable cause = ex.getCause();
                    throw ExceptionHelper.throwChecked(cause, Exception.class);
                }
            }
        };

        private static Runnable jobProcessTask(StatefulJobConsumer<String> consumer, String job) {
            return () -> {
                try {
                    consumer.processJob(job);
                } catch (RuntimeException | Error ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            };
        }

        public abstract void submitJobs(StatefulJobConsumer<String> consumer, List<String> jobs) throws Exception;

        public void submitJobsAndComplete(
                StatefulJobConsumer<String> consumer,
                List<String> jobs,
                ConsumerCompletionStatus completionStatus) throws Exception {

            Throwable toThrow = null;
            try {
                submitJobs(consumer, jobs);
            } catch (Throwable ex) {
                toThrow = ex;
            }

            try {
                consumer.finishProcessing(completionStatus);
            } catch (Throwable ex) {
                toThrow = ExceptionCollector.updateException(toThrow, ex);
            }

            if (toThrow instanceof Exception) {
                throw (Exception) toThrow;
            }
            ExceptionHelper.rethrowIfNotNull(toThrow);
        }
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestExceptionType {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ExecutorManagement {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface ConcurrencyParameters {
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface SubmitTaskLogic {
    }
}
