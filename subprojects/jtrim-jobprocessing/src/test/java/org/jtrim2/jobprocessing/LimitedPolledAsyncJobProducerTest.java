package org.jtrim2.jobprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.executor.ContextAwareTaskExecutor;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.SingleThreadedExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.TaskExecutors;
import org.junit.Test;

import static org.jtrim2.jobprocessing.ProducerConsumerTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LimitedPolledAsyncJobProducerTest {
    @Test(timeout = 30000)
    public void testInOrderTransfer() throws Exception {
        List<String> allJobs = testStrings(10000);
        int outstandingJobs = 10;

        TaskExecutorService consumerExecutor = new SingleThreadedExecutor("consumer-testInOrderTransfer");
        TaskExecutorService producerExecutor = new SingleThreadedExecutor("producer-testInOrderTransfer");
        try {
            TestSetup testSetup = new TestSetup(true, allJobs);

            testSetup.producer().setExecutor(producerExecutor);
            testSetup.consumer().setExecutor(consumerExecutor);

            testSetup.startTransferAndWait(Cancellation.UNCANCELABLE_TOKEN, outstandingJobs);

            testSetup.checkNoGeneralProblems();
            testSetup.expectCompleted((status, futureError) -> {
                assertTrue("success", status.isSuccess());
                assertNull("futureError", futureError);
            });
            assertEquals(allJobs, testSetup.consumer().getReceivedJobs());
        } finally {
            producerExecutor.shutdownAndCancel();
            consumerExecutor.shutdownAndCancel();

            producerExecutor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
            consumerExecutor.awaitTermination(Cancellation.UNCANCELABLE_TOKEN);
        }
    }

    public void testNormalTransfer(int outstandingJobs, List<String> allJobs) {
        TestSetup testSetup = new TestSetup(true, allJobs);
        testSetup.startTransfer(Cancellation.UNCANCELABLE_TOKEN, outstandingJobs);

        testSetup.checkNoGeneralProblems();
        testSetup.expectNotCompleted();

        testSetup.runAllExecutorTasks();

        testSetup.checkNoGeneralProblems();
        testSetup.expectCompleted((status, futureError) -> {
            assertTrue("success", status.isSuccess());
            assertNull("futureError", futureError);
        });
        testSetup.consumer().expectJobsWithoutOrder(allJobs);
    }

    @Test(timeout = 30000)
    public void testNormalTransfer1() {
        testNormalTransfer(1, testStrings(3));
    }

    @Test(timeout = 30000)
    public void testNormalTransfer2() {
        testNormalTransfer(2, testStrings(3));
    }

    @Test(timeout = 30000)
    public void testNormalTransfer3() {
        testNormalTransfer(3, testStrings(3));
    }

    @Test(timeout = 30000)
    public void testNormalTransfer4() {
        testNormalTransfer(4, testStrings(3));
    }

    @Test(timeout = 30000)
    public void testNormalTransferNoJobs() {
        testNormalTransfer(1, testStrings(0));
    }

    @Test(timeout = 30000)
    public void testImmediateCancellation() {
        int outstandingJobs = 2;
        List<String> allJobs = testStrings(3);

        TestSetup testSetup = new TestSetup(true, allJobs);

        testSetup.startTransfer(Cancellation.CANCELED_TOKEN, outstandingJobs);

        testSetup.runAllExecutorTasks();

        testSetup.checkNoGeneralProblems();
        testSetup.expectCanceled();
        testSetup.consumer().expectJobsWithoutOrder(Collections.emptyList());
    }

    @Test(timeout = 30000)
    public void testPostStartCancellation() {
        int outstandingJobs = 2;
        List<String> allJobs = testStrings(3);
        CancellationSource cancellation = Cancellation.createCancellationSource();

        TestSetup testSetup = new TestSetup(true, allJobs);

        testSetup.startTransfer(cancellation.getToken(), outstandingJobs);

        testSetup.checkNoGeneralProblems();
        testSetup.expectNotCompleted();

        cancellation.getController().cancel();
        testSetup.runAllExecutorTasks();

        testSetup.checkNoGeneralProblems();
        testSetup.expectCanceled();
        testSetup.consumer().expectJobsWithoutOrder(Collections.emptyList());
    }

    @Test(timeout = 30000)
    public void testPostFirstJobRetrievalCancellation() {
        int outstandingJobs = 2;
        List<String> allJobs = testStrings(3);
        CancellationSource cancellation = Cancellation.createCancellationSource();

        TestSetup testSetup = new TestSetup(true, allJobs);

        AtomicReference<String> uncanceledJob = new AtomicReference<>();
        testSetup.producer().setJobPreprocessor((processorCancelToken, job) -> {
            cancellation.getController().cancel();
            if (!processorCancelToken.isCanceled()) {
                uncanceledJob.compareAndSet(null, job);
            }
            return job;
        });

        testSetup.startTransfer(cancellation.getToken(), outstandingJobs);

        testSetup.checkNoGeneralProblems();
        testSetup.expectNotCompleted();

        testSetup.runAllExecutorTasks();

        testSetup.checkNoGeneralProblems();
        assertTrue("producer-started", testSetup.producer().isStarted());
        testSetup.expectCanceled();
        testSetup.consumer().expectJobsWithoutOrder(Collections.emptyList());
        assertNull("uncanceled-job", uncanceledJob.get());
    }

    @Test(timeout = 30000)
    public void testPostFirstJobConsumingCancellation() {
        int outstandingJobs = 2;
        List<String> allJobs = testStrings(3);
        CancellationSource cancellation = Cancellation.createCancellationSource();

        TestSetup testSetup = new TestSetup(true, allJobs);

        AtomicReference<String> uncanceledJob = new AtomicReference<>();
        testSetup.consumer().setJobHandler((jobCancelToken, job) -> {
            cancellation.getController().cancel();
            if (!jobCancelToken.isCanceled()) {
                uncanceledJob.compareAndSet(null, job);
            }
        });

        testSetup.startTransfer(cancellation.getToken(), outstandingJobs);

        testSetup.checkNoGeneralProblems();
        testSetup.expectNotCompleted();

        testSetup.runAllExecutorTasks();

        testSetup.checkNoGeneralProblems();
        assertTrue("producer-started", testSetup.producer().isStarted());
        assertTrue("consumer-started", testSetup.consumer().isStarted());
        testSetup.expectCanceled();
        assertNull("uncanceled-job", uncanceledJob.get());
    }

    private static final class TestSetup implements AutoCloseable {
        private final TestProducerFactory producer;
        private final TestConsumerFactory consumer;

        private final AtomicReference<Throwable> failureRef;
        private final AtomicLong completionCount;

        public TestSetup(boolean eagerCancel, List<? extends String> jobs) {
            this.producer = new TestProducerFactory(eagerCancel, jobs);
            this.consumer = new TestConsumerFactory(eagerCancel);

            this.failureRef = new AtomicReference<>();
            this.completionCount = new AtomicLong(0);
        }

        public TestProducerFactory producer() {
            return producer;
        }

        public TestConsumerFactory consumer() {
            return consumer;
        }

        public void runAllExecutorTasks() {
            while (consumer.executeCurrentlySubmitted() > 0) {
            }

            while (producer.executeCurrentlySubmitted() > 0) {
                while (consumer.executeCurrentlySubmitted() > 0) {
                }
            }
        }

        public void checkNoGeneralProblems() {
            producer.checkNoGeneralProblems();
            consumer.checkNoGeneralProblems();
        }

        public void expectNotCompleted() {
            producer.expectClosed(false);
            consumer.expectNotCompleted();

            assertEquals("completionCount", 0L, completionCount.get());
            assertNull("failure", failureRef.get());
        }

        public void expectFutureCompleted(Consumer<? super Throwable> completionChecker) {
            assertEquals("completionCount", 1L, completionCount.get());
            completionChecker.accept(failureRef.get());
        }

        public void expectCompleted(BiConsumer<? super ConsumerCompletionStatus, ? super Throwable> completionChecker) {
            expectCompleted(false, completionChecker);
        }

        public void expectCompleted(
                boolean maybeCanceled,
                BiConsumer<? super ConsumerCompletionStatus, ? super Throwable> completionChecker) {

            if (!maybeCanceled || producer.isStarted()) {
                producer.expectClosed(true);
            }

            expectFutureCompleted(futureError -> {
                if (!maybeCanceled || consumer.isStarted()) {
                    consumer.expectCompleted(status -> {
                        completionChecker.accept(status, futureError);
                    });
                }
            });
        }

        public void expectCanceled() {
            expectCompleted(true, (status, futureError) -> {
                assertTrue("canceled", status.isCanceled());
                if (!AsyncTasks.isCanceled(futureError)) {
                    throw new AssertionError("Expected cancelation exception", futureError);
                }
            });
        }

        public CompletionStage<Void> startTransfer(CancellationToken cancelToken, int outstandingJobCount) {
            LimitedPolledAsyncJobProducer<String> provider = new LimitedPolledAsyncJobProducer<>(
                    2,
                    producer.toAsync()
            );

            CompletionStage<Void> future = provider.startTransfer(cancelToken, consumer.toAsync());
            return future.whenComplete((result, failure) -> {
                failureRef.set(failure);
                completionCount.incrementAndGet();
            });
        }

        public void startTransferAndWait(
                CancellationToken cancelToken,
                int outstandingJobCount) throws InterruptedException, ExecutionException {

            CompletableFuture<Void> future = new CompletableFuture<>();
            startTransfer(cancelToken, outstandingJobCount)
                    .whenComplete(AsyncTasks.completeForwarder(future));
            future.get();
        }

        @Override
        public void close() {
            producer.close();
            consumer.close();
        }
    }

    private static final class TestConsumerFactory implements StatefulJobConsumerStarter<String>, AutoCloseable {
        private TaskExecutor consumerExecutor;
        private ContextAwareTaskExecutor consumerContext;

        private final AtomicReference<RuntimeException> outOfContextCallRef;
        private final AtomicReference<RuntimeException> finishCallErrorRef;
        private final AtomicReference<RuntimeException> accessAfterFinishRef;
        private volatile boolean finished;

        private final AtomicReference<ConsumerCompletionStatus> resultRef;

        private volatile boolean started;
        private final Collection<String> received;
        private BiConsumer<? super CancellationToken, ? super String> jobHandler;
        private Consumer<? super ConsumerCompletionStatus> finishHandler;

        public TestConsumerFactory(boolean eagerCancel) {
            this.consumerExecutor = new ManualTaskExecutor(eagerCancel);
            this.consumerContext = TaskExecutors.contextAware(consumerExecutor);

            this.outOfContextCallRef = new AtomicReference<>();
            this.finishCallErrorRef = new AtomicReference<>();
            this.accessAfterFinishRef = new AtomicReference<>();
            this.finished = false;

            this.resultRef = new AtomicReference<>();

            this.started = false;
            this.received = new ConcurrentLinkedQueue<>();
            this.jobHandler = (cancelToken, job) -> { };
            this.finishHandler = status -> { };
        }

        public AsyncJobConsumerStarter<String> toAsync() {
            return toAsync(consumerContext);
        }

        public ContextAwareTaskExecutor getConsumerContext() {
            return consumerContext;
        }

        public void setExecutor(TaskExecutor executor) {
            Objects.requireNonNull(executor, "executor");

            this.consumerExecutor = executor;
            this.consumerContext = TaskExecutors.contextAware(executor);
        }

        public void setJobHandler(BiConsumer<? super CancellationToken, ? super String> jobHandler) {
            this.jobHandler = Objects.requireNonNull(jobHandler, "jobHandler");
        }

        public void setFinishHandler(Consumer<? super ConsumerCompletionStatus> finishHandler) {
            this.finishHandler = Objects.requireNonNull(finishHandler, "finishHandler");
        }

        public void checkNoGeneralProblems() {
            verifyNoException(outOfContextCallRef);
            verifyNoException(finishCallErrorRef);
            verifyNoException(accessAfterFinishRef);
        }

        public void expectNotCompleted() {
            ConsumerCompletionStatus result = resultRef.get();
            if (result != null) {
                throw new AssertionError("Not expected completion of consumer, but received: " + result);
            }
        }

        public boolean isStarted() {
            return started;
        }

        public void expectCompleted(Consumer<? super ConsumerCompletionStatus> statusChecker) {
            ConsumerCompletionStatus result = resultRef.get();
            if (result == null) {
                throw new AssertionError("Expected completion of consumer.");
            }
            statusChecker.accept(result);
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

        public int executeCurrentlySubmitted() {
            if (consumerExecutor instanceof ManualTaskExecutor) {
                return ((ManualTaskExecutor)consumerExecutor).executeCurrentlySubmitted();
            } else {
                throw new IllegalStateException("Test error: Not manual executor");
            }
        }

        private void checkContext() {
            if (!consumerContext.isExecutingInThis()) {
                setFirstException(outOfContextCallRef, "Consumer was called out of context.");
            }
        }

        @Override
        public StatefulJobConsumer<String> startConsumer(CancellationToken cancelToken) {
            started = true;

            checkContext();

            return new StatefulJobConsumer<String>() {
                @Override
                public void processJob(String job) {
                    checkContext();
                    if (finished) {
                        accessAfterFinishRef.set(new RuntimeException("processJob after finishProcessing."));
                    }
                    received.add(job);
                    jobHandler.accept(cancelToken, job);
                }

                @Override
                public void finishProcessing(ConsumerCompletionStatus finalStatus) {
                    finished = true;

                    checkContext();

                    if (finalStatus == null) {
                        setFirstException(finishCallErrorRef, "Finished with null.");
                        return;
                    }

                    if (!resultRef.compareAndSet(null, finalStatus)) {
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

        @Override
        public void close() {
        }
    }

    private static final class TestProducerFactory implements PolledJobProducerStarter<String>, AutoCloseable {
        private final Deque<String> jobs;
        private final Runnable closeTrackerMock;
        private volatile boolean closed;

        private TaskExecutor producerExecutor;
        private ContextAwareTaskExecutor producerContext;

        private final AtomicReference<RuntimeException> outOfContextCallRef;
        private final AtomicReference<RuntimeException> accessAfterCloseRef;

        private final AtomicBoolean started;
        private final AtomicReference<RuntimeException> multipleStartRef;

        private BiFunction<? super CancellationToken, ? super String, ? extends String> jobPreprocessor;
        private Runnable closeAction;

        public TestProducerFactory(
                boolean eagerCancel,
                List<? extends String> elements) {

            this.jobs = new ConcurrentLinkedDeque<>(elements);
            this.closeTrackerMock = mock(Runnable.class);
            this.closed = false;

            this.producerExecutor = new ManualTaskExecutor(eagerCancel);
            this.producerContext = TaskExecutors.contextAware(producerExecutor);

            this.outOfContextCallRef = new AtomicReference<>();
            this.accessAfterCloseRef = new AtomicReference<>();

            this.started = new AtomicBoolean(false);
            this.multipleStartRef = new AtomicReference<>();

            this.jobPreprocessor = (cancelToken, job) -> job;
            this.closeAction = Tasks.noOpTask();
        }

        public AsyncPolledJobProducerStarter<String> toAsync() {
            return toAsync(producerContext);
        }

        public TaskExecutor getProducerContext() {
            return producerContext;
        }

        public void setExecutor(TaskExecutor executor) {
            Objects.requireNonNull(executor, "executor");

            this.producerExecutor = executor;
            this.producerContext = TaskExecutors.contextAware(executor);
        }

        public void setJobPreprocessor(
                BiFunction<? super CancellationToken, ? super String, ? extends String> jobPreprocessor) {

            this.jobPreprocessor = Objects.requireNonNull(jobPreprocessor, "jobPreprocessor");
        }

        public void setCloseAction(Runnable closeAction) {
            this.closeAction = Objects.requireNonNull(closeAction, "closeAction");
        }

        public boolean isStarted() {
            return started.get();
        }

        public void checkNoGeneralProblems() {
            verifyNoException(multipleStartRef);
            verifyNoException(outOfContextCallRef);
            verifyNoException(accessAfterCloseRef);
        }

        public void expectClosed(boolean expectedStatus) {
            verify(closeTrackerMock, times(expectedStatus ? 1 : 0)).run();
        }

        public int executeCurrentlySubmitted() {
            if (producerExecutor instanceof ManualTaskExecutor) {
                return ((ManualTaskExecutor)producerExecutor).executeCurrentlySubmitted();
            } else {
                throw new IllegalStateException("Test error: Not manual executor");
            }
        }

        private void checkContext() {
            if (!producerContext.isExecutingInThis()) {
                setFirstException(outOfContextCallRef, "Producer was called out of context.");
            }
        }

        @Override
        public PolledJobProducer<String> startProducer(CancellationToken cancelToken) throws Exception {
            if (!started.compareAndSet(false, true)) {
                setFirstException(multipleStartRef, "Test producer does not support multiple calls");
            }

            checkContext();

            return new PolledJobProducer<String>() {
                @Override
                public String getNextJob() {
                    checkContext();
                    if (closed) {
                        setFirstException(accessAfterCloseRef, "getNextJob after close.");
                    }
                    return jobPreprocessor.apply(cancelToken, jobs.pollFirst());
                }

                @Override
                public void close() {
                    closed = true;
                    checkContext();
                    closeTrackerMock.run();
                    closeAction.run();
                }
            };
        }

        @Override
        public void close() {
        }
    }

}
