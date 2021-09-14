package org.jtrim2.stream;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import org.jtrim2.executor.SyncTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutorService;
import org.jtrim2.executor.TaskExecutors;
import org.jtrim2.executor.ThreadPoolTaskExecutor;
import org.jtrim2.testutils.TestParameterRule;
import org.jtrim2.testutils.UnsafeConsumer;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.Rule;
import org.junit.Test;

import static org.jtrim2.stream.ProducerConsumerTestUtils.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ParallelSeqGroupProducerTest {
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
    public final TestParameterRule elementCountRule = new TestParameterRule(
            ConcurrencyParameters.class,
            Arrays.asList(1, 2, 2 * Runtime.getRuntime().availableProcessors()),
            this::setElementCount
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
            Arrays.asList(TestException.class, TestCheckedException.class, OperationCanceledException.class),
            this::setTestExceptionType
    );

    private boolean managedExecutor = false;
    private int queueSize = 1;
    private int consumerThreadCount = 1;
    private int elementCount = 1;
    private SubTaskLogicImpl submitTasksLogic = SubTaskLogicImpl.SYNC;
    private Class<? extends Exception> testExceptionType = TestException.class;

    private void setManagedExecutor(boolean managedExecutor) {
        this.managedExecutor = managedExecutor;
    }

    private void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    private void setConsumerThreadCount(int consumerThreadCount) {
        this.consumerThreadCount = consumerThreadCount;
    }

    private void setElementCount(int elementCount) {
        this.elementCount = elementCount;
    }

    private void setSubmitTasksLogic(SubTaskLogicImpl submitTasksLogic) {
        this.submitTasksLogic = submitTasksLogic;
    }

    public void setTestExceptionType(Class<? extends Exception> testExceptionType) {
        this.testExceptionType = testExceptionType;
    }

    private void verifySuccessfulCompletion(
            TestConsumerFactory consumerFactory,
            List<String> elements) {

        consumerFactory.verifyStartedAllThreads();
        consumerFactory.verifyCompletedAllThreads();

        consumerFactory.checkNoGeneralProblems();
        consumerFactory.expectElementsWithoutOrder(elements);
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
        List<String> elements = testStrings(elementCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setSyncConsumerThreads(true);

            consumerFactory.startProducerTest(Cancellation.UNCANCELABLE_TOKEN, submitTasksLogic.producer(elements));

            verifySuccessfulCompletion(consumerFactory, elements);
        }
    }

    @Test(timeout = 30000)
    public void testNormalRunWithLateSubmit() throws Exception {
        List<String> elements = testStrings(elementCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setSyncConsumerThreads(true);

            SeqGroupProducer<String> producer = submitTasksLogic.producer(elements);
            AtomicReference<SeqConsumer<? super String>> consumerRef = new AtomicReference<>();
            consumerFactory.startProducerTest(Cancellation.UNCANCELABLE_TOKEN, (cancelToken, consumer) -> {
                consumerRef.compareAndSet(null, consumer);
                producer.transferAll(cancelToken, consumer);
            });

            verifySuccessfulCompletion(consumerFactory, elements);

            SeqConsumer<? super String> serialConsumer = Objects.requireNonNull(consumerRef.get(), "consumer");
            try {
                serialConsumer.consumeAll(Cancellation.UNCANCELABLE_TOKEN, (producerCancelToken, consumer) -> {
                    consumer.processElement("X");
                });
                fail("Expected");
            } catch (Exception ex) {
                // Expected
            }
        }
    }

    private void verifyCompletelyCanceledCompletion(TestConsumerFactory consumerFactory) {
        consumerFactory.verifyStartedThreadCount(0);
        consumerFactory.verifyCompletedAllThreads();

        consumerFactory.checkNoGeneralProblems();
        consumerFactory.expectElementsWithoutOrder(Collections.emptyList());
    }

    @ExecutorManagement
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testImmediateCancellation() throws Exception {
        List<String> elements = testStrings(elementCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);

            try {
                consumerFactory.startProducerTest(
                        Cancellation.CANCELED_TOKEN,
                        SubTaskLogicImpl.SYNC.producer(elements)
                );
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
    public void testCancellationOnStartup() throws Exception {
        CancellationSource cancel = Cancellation.createCancellationSource();
        List<String> elements = testStrings(elementCount);

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
                consumerFactory.startProducerTest(cancel.getToken(), SubTaskLogicImpl.SYNC.producer(elements));
                fail("Expected OperationCanceledException");
            } catch (OperationCanceledException ex) {
                // Expected
            }

            verifyNoException(cancelNotDetectedRef);
            consumerFactory.checkNoGeneralProblems();
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

        List<String> elements = testStrings(elementCount);
        AtomicReference<RuntimeException> unexpectedProblemRef = new AtomicReference<>();
        Set<Long> threadsCanceled = Collections.newSetFromMap(new ConcurrentHashMap<>());
        AtomicInteger producerStartCount = new AtomicInteger(0);
        AtomicInteger producerFinishCount = new AtomicInteger(0);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setSyncConsumerThreads(true);
            consumerFactory.setConsumerExceptionHandler(ex -> {
                boolean finishedQuickly = producerStartCount.get() > 0
                        && (producerStartCount.get() == producerFinishCount.get());
                if ((ex == null && !finishedQuickly) || (ex != null && !(ex instanceof OperationCanceledException))) {
                    setFirstException(
                            unexpectedProblemRef,
                            "Expected exception " + testExceptionType + " but received cause.",
                            ex
                    );
                }
            });

            consumerFactory.setElementHandler((cancelToken, element) -> {
                cancel.getController().cancel();
                if (!cancelToken.isCanceled()) {
                    setFirstException(unexpectedProblemRef, "Missed cancellation for " + element);
                }
                threadsCanceled.add(Thread.currentThread().getId());
                throw new OperationCanceledException("canceled-" + element);
            });

            AtomicInteger processedElementCountRef = new AtomicInteger(0);
            try {
                SeqGroupProducer<String> producer = ElementProducers.toSingleGroupProducer((cancelToken, consumer) -> {
                    for (String element: elements) {
                        consumer.processElement(element);
                        processedElementCountRef.getAndIncrement();
                    }
                });
                producer = trackedProducer(producer, producerStartCount, producerFinishCount);
                consumerFactory.startProducerTest(cancel.getToken(), producer);
                fail("Expected OperationCanceledException");
            } catch (OperationCanceledException ex) {
                // expected
            }

            consumerFactory.verifyCompletedAllThreads();

            verifyNoException(unexpectedProblemRef);
            consumerFactory.checkNoGeneralProblems();
        }
    }

    private static void expectedException(Class<? extends Throwable> expectedType, Throwable received) {
        if (received.getClass() != expectedType) {
            throw new AssertionError("Expected " + expectedType + ", but received cause.", received);
        }
    }

    @TestExceptionType
    @Test(timeout = 30000)
    public void testFailedStart() throws Exception {
        List<String> elements = testStrings(10);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            consumerFactory.setThreadCount(2);
            consumerFactory.setQueueSize(2);
            consumerFactory.setSyncConsumerThreads(true);

            AtomicReference<RuntimeException> unexpectedTestError = new AtomicReference<>();
            consumerFactory.setStartHandler(cancelToken -> {
                Exception startFailure;
                try {
                    startFailure = testExceptionType.newInstance();
                } catch (Throwable ex) {
                    setFirstException(unexpectedTestError, "Unexpected exception creation failure.");
                    throw new AssertionError(ex);
                }
                throw startFailure;
            });

            try {
                consumerFactory.startProducerTest(
                        Cancellation.UNCANCELABLE_TOKEN,
                        SubTaskLogicImpl.SYNC.producer(elements)
                );
                fail("Expected Exception");
            } catch (Exception ex) {
                expectedException(testExceptionType, ex);
            }

            verifyNoException(unexpectedTestError);
            consumerFactory.checkNoGeneralProblems();
        }
    }

    @Test(timeout = 30000)
    public void testFailedExecutor() throws Exception {
        List<String> elements = testStrings(10);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            consumerFactory.setThreadCount(2);
            consumerFactory.setQueueSize(2);
            consumerFactory.setSyncConsumerThreads(false);
            consumerFactory.setFailingExecutor(true);

            try {
                consumerFactory.startProducerTest(
                        Cancellation.UNCANCELABLE_TOKEN,
                        SubTaskLogicImpl.SYNC.producer(elements)
                );
                fail("Expected TestException");
            } catch (TestException ex) {
                // Expected
            }

            consumerFactory.checkNoGeneralProblems();
        }
    }

    @SubmitTaskLogic
    @TestExceptionType
    @ExecutorManagement
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testFailSingleElement() throws Exception {
        List<String> elements = testStrings(elementCount);

        String elementToFail = Integer.toString(Math.min(3, elementCount - 1));
        AtomicReference<ElementFailureDef> elementFailureDefRef = new AtomicReference<>();
        AtomicReference<RuntimeException> unexpectedTestError = new AtomicReference<>();
        AtomicInteger producerStartCount = new AtomicInteger(0);
        AtomicInteger producerFinishCount = new AtomicInteger(0);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setSyncConsumerThreads(true);
            consumerFactory.setConsumerExceptionHandler(ex -> {
                // FIXME: We should fail all started ones.
                boolean finishedQuickly = producerStartCount.get() > 0
                        && (producerStartCount.get() == producerFinishCount.get());
                if ((ex == null && !finishedQuickly) || (ex != null && !testExceptionType.isInstance(ex))) {
                    setFirstException(
                            unexpectedTestError,
                            "Expected exception " + testExceptionType + " but received cause.",
                            ex
                    );
                }
            });

            consumerFactory.setElementHandler((cancelToken, element) -> {
                if (elementToFail.equals(element)) {
                    elementFailureDefRef.set(new ElementFailureDef(elementToFail, Thread.currentThread().getId()));

                    Exception elementFailure;
                    try {
                        elementFailure = testExceptionType.getConstructor(String.class).newInstance(element);
                    } catch (Throwable ex) {
                        setFirstException(unexpectedTestError, "Unexpected exception creation failure.");
                        throw new RuntimeException(ex);
                    }
                    throw elementFailure;
                }
            });

            try {
                consumerFactory.startProducerTest(
                        Cancellation.UNCANCELABLE_TOKEN,
                        trackedProducer(submitTasksLogic.producer(elements), producerStartCount, producerFinishCount)
                );
                fail("Expected exception");
            } catch (Exception ex) {
                expectedException(testExceptionType, ex);
                assertEquals("element-fail-message", elementToFail, ex.getMessage());
            }

            verifyNoException(unexpectedTestError);

            consumerFactory.checkNoGeneralProblems();
            consumerFactory.verifyStartedAllThreads();

            ElementFailureDef elementFailureDef = elementFailureDefRef.get();
            assertNotNull("elementFailureDef", elementFailureDef);

            consumerFactory.verifyCompletedAllThreads();
        }
    }

    @ExecutorManagement
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testFailingWrappedProducer() throws Exception {
        TestException producerException = new TestException("producer-exception");
        SeqGroupProducer<String> failingProducer = (cancelToken, seqConsumer) -> {
            seqConsumer.consumeAll(cancelToken, (seqCancelToken, consumer) -> {
                throw producerException;
            });
        };

        Runnable selfVerification;
        AtomicReference<RuntimeException> missingExceptionRef = new AtomicReference<>();
        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setSyncConsumerThreads(true);
            selfVerification = consumerFactory.setExpectedConsumerException(missingExceptionRef, producerException);
            try {
                consumerFactory.startProducerTest(Cancellation.UNCANCELABLE_TOKEN, failingProducer);
                fail("Expected failure.");
            } catch (TestException ex) {
                assertSame(producerException, ex);
            }
        }

        verify(selfVerification, atLeastOnce()).run();
        verifyNoException(missingExceptionRef);
    }

    @SubmitTaskLogic
    @ConcurrencyParameters
    @Test(timeout = 30000)
    public void testSyncExecutorFails() throws Exception {
        List<String> elements = testStrings(elementCount);

        try (TestConsumerFactory consumerFactory = new TestConsumerFactory()) {
            setupDefaults(consumerFactory);
            consumerFactory.setExecutor(new SyncTaskExecutor());
            consumerFactory.setSyncConsumerThreads(false);

            try {
                consumerFactory.startProducerTest(Cancellation.UNCANCELABLE_TOKEN, submitTasksLogic.producer(elements));
                fail("Expected IllegalStateException");
            } catch (IllegalStateException ex) {
                // Expected
            }
        }
    }

    private static <T> SeqGroupProducer<T> trackedProducer(
            SeqGroupProducer<T> src,
            AtomicInteger startCount,
            AtomicInteger finishCount) {

        return (cancelToken, seqConsumer) -> {
            startCount.incrementAndGet();
            src.transferAll(cancelToken, seqConsumer);
            finishCount.incrementAndGet();
        };
    }

    private static final class TestConsumerFactory implements SeqConsumer<String>, AutoCloseable {
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
        private final AtomicReference<RuntimeException> unexpectedExceptionRef;
        private final List<String> received;

        private UnsafeConsumer<? super CancellationToken> startHandler;
        private UnsafeConsumer<? super Throwable> consumerExceptionHandler;
        private TestElementHandler elementHandler;

        private CountDownLatch startLatch;
        private final AtomicLong startedThreadCount;
        private final AtomicLong finishedThreadCount;

        public TestConsumerFactory() {
            this.testCancellation = Cancellation.createCancellationSource();
            this.outOfContextCallRef = new AtomicReference<>();
            this.threadConfinementBreachRef = new AtomicReference<>();
            this.unexpectedExceptionRef = new AtomicReference<>();
            this.received = Collections.synchronizedList(new ArrayList<>());
            this.startHandler = token -> { };
            this.elementHandler = (token, element) -> { };
            this.consumerExceptionHandler = null;
            this.startedThreadCount = new AtomicLong(0);
            this.finishedThreadCount = new AtomicLong(0);
            this.threadCount = 1;
            this.syncConsumerThreads = false;
            this.queueSize = 0;
            this.failingExecutor = false;
        }

        public void setManagedExecutor(boolean managedExecutor) {
            if (!managedExecutor) {
                this.executor = null;
            }
            this.managedExecutor = managedExecutor;
        }

        public void setExecutor(TaskExecutorService executor) {
            this.executor = executor;
            this.managedExecutor = false;
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

        public void setStartHandler(UnsafeConsumer<? super CancellationToken> startHandler) {
            this.startHandler = Objects.requireNonNull(startHandler, "startHandler");
        }

        public void setElementHandler(TestElementHandler elementHandler) {
            this.elementHandler = Objects.requireNonNull(elementHandler, "elementHandler");
        }

        public void setConsumerExceptionHandler(UnsafeConsumer<? super Throwable> consumerExceptionHandler) {
            this.consumerExceptionHandler = consumerExceptionHandler;
        }

        public Runnable setExpectedConsumerException(
                AtomicReference<RuntimeException> unexpectedExceptionRef,
                Class<? extends Throwable> expectedType) {

            return setExpectedConsumerException(unexpectedExceptionRef, expectedType, expectedType::isInstance);
        }

        public Runnable setExpectedConsumerException(
                AtomicReference<RuntimeException> unexpectedExceptionRef,
                Throwable expectedException) {

            return setExpectedConsumerException(
                    unexpectedExceptionRef,
                    expectedException,
                    ex -> ex == expectedException
            );
        }

        public Runnable setExpectedConsumerException(
                AtomicReference<RuntimeException> unexpectedExceptionRef,
                Object exceptionDescr,
                Predicate<? super Throwable> exceptionTest) {

            Runnable selfTest = mock(Runnable.class);
            setConsumerExceptionHandler(ex -> {
                selfTest.run();
                if (ex == null) {
                    setFirstException(
                            unexpectedExceptionRef,
                            "Expected exception " + exceptionDescr + " but received none.",
                             new RuntimeException("stack-trace")
                    );
                } else if (!exceptionTest.test(ex)) {
                    setFirstException(
                            unexpectedExceptionRef,
                            "Expected exception " + exceptionDescr + " but received cause.",
                            ex
                    );
                }
            });
            return selfTest;
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
        public void consumeAll(
                CancellationToken cancelToken,
                SeqProducer<? extends String> seqProducer) throws Exception {

            startedThreadCount.getAndIncrement();

            long expectedId = Thread.currentThread().getId();
            checkContext(expectedId);

            if (syncConsumerThreads) {
                startLatch.countDown();
                CancelableWaits.await(testCancellation.getToken(), startLatch::await);
            }

            Throwable consumerEx = null;
            startHandler.accept(cancelToken);
            try {
                seqProducer.transferAll(cancelToken, element -> {
                    checkContext(expectedId);
                    elementHandler.handleElement(cancelToken, element);
                    received.add(element);
                });
            } catch (Throwable ex) {
                consumerEx = ex;
                if (consumerExceptionHandler == null) {
                    setFirstException(outOfContextCallRef, "unexpected transfer exception", ex);
                }
                throw ex;
            } finally {
                finishedThreadCount.getAndIncrement();
                if (consumerExceptionHandler != null) {
                    consumerExceptionHandler.accept(consumerEx);
                }
            }
        }

        private void initExecutor() {
            if (executor != null) {
                return;
            }

            if (failingExecutor) {
                TaskExecutor wrapped = ExecutorConverter.asTaskExecutor(command -> {
                    throw new TestException();
                });
                executorContext = TaskExecutors.contextAware(TaskExecutors.upgradeToStoppable(wrapped));
                executor = TaskExecutors.upgradeToStoppable(executorContext);
            } else {
                ContextAwareTaskExecutorService threadPool = new ThreadPoolTaskExecutor(
                        "ParallelSeqGroupProducerTest-consumer",
                        threadCount
                );
                executorContext = threadPool;
                executor = threadPool;
            }
        }

        public void startProducerTest(
                CancellationToken cancelToken,
                SeqGroupProducer<? extends String> producer) throws Exception {

            if (startLatch != null) {
                throw new AssertionError("This is a single use object for testing.");
            }

            startLatch = new CountDownLatch(threadCount);
            initExecutor();

            ParallelSeqGroupProducer<String> parallelProducer = new ParallelSeqGroupProducer<>(
                    getExecutorRef(),
                    threadCount,
                    queueSize,
                    producer
            );

            parallelProducer.transferAll(cancelToken, this);
        }

        public List<String> getReceivedElements() {
            return Arrays.asList(received.toArray(new String[0]));
        }

        public void expectElementsWithoutOrder(Collection<? extends String> expectedElements) {
            List<String> expectedElementsSorted = new ArrayList<>(expectedElements);
            expectedElementsSorted.sort(null);

            List<String> receivedElements = new ArrayList<>(getReceivedElements());
            receivedElements.sort(null);

            assertEquals(expectedElementsSorted, receivedElements);
        }

        public void checkNoGeneralProblems() {
            verifyNoException(outOfContextCallRef);
            verifyNoException(threadConfinementBreachRef);
            verifyNoException(unexpectedExceptionRef);
        }

        public void verifyStartedAllThreads() {
            verifyStartedThreadCount(threadCount);
        }

        public void verifyStartedThreadCount(int expectedThreadCount) {
            assertEquals("Started thread count", expectedThreadCount, startedThreadCount.get());
            assertEquals("Finished thread count", expectedThreadCount, finishedThreadCount.get());
        }

        public void verifyCompletedAllThreads() {
            assertEquals("Started = Finished", startedThreadCount.get(), finishedThreadCount.get());
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

    private interface TestElementHandler {
        void handleElement(CancellationToken cancelToken, String element) throws Exception;
    }

    private static final class ElementFailureDef {
        private final String elementName;
        private final long threadId;

        public ElementFailureDef(String elementName, long threadId) {
            this.elementName = Objects.requireNonNull(elementName, "elementName");
            this.threadId = threadId;
        }

        public boolean isSameElement(String candidateElement) {
            return elementName.equals(candidateElement);
        }

        public long getElementThreadId() {
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

    private static class TestCheckedException extends Exception {
        private static final long serialVersionUID = 1L;

        public TestCheckedException() {
        }

        public TestCheckedException(String message) {
            super(message);
        }
    }

    private static class ExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }

    private enum SubTaskLogicImpl {
        SYNC {
            @Override
            public SeqProducer<String> serialProducer(List<String> elements) throws Exception {
                return (producerCancelToken, consumer) -> {
                    // FIXME: We should add a test to test producerCancelToken
                    for (String element : elements) {
                        consumer.processElement(element);
                    }
                };
            }
        },
        ALL_ELEMENTS_CONCURRENT {
            @Override
            public SeqProducer<String> serialProducer(List<String> elements) throws Exception {
                return (cancelToken, consumer) -> {
                    List<Runnable> elementSubmitters = elements
                            .stream()
                            .map(element -> elementProcessTask(consumer, element))
                            .collect(Collectors.toList());

                    try {
                        Tasks.runConcurrently(elementSubmitters);
                    } catch (TaskExecutionException ex) {
                        Throwable cause = ex.getCause();
                        if (cause instanceof ExceptionWrapper) {
                            throw ExceptionHelper.throwChecked(cause.getCause(), Exception.class);
                        } else {
                            throw ExceptionHelper.throwChecked(cause, Exception.class);
                        }
                    }
                };
            }
        };

        private static Runnable elementProcessTask(ElementConsumer<? super String> consumer, String element) {
            return () -> {
                try {
                    consumer.processElement(element);
                } catch (RuntimeException | Error ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new ExceptionWrapper(ex);
                }
            };
        }

        public abstract SeqProducer<String> serialProducer(List<String> elements) throws Exception;

        public SeqGroupProducer<String> producer(List<String> elements) throws Exception {
            return ElementProducers.toSingleGroupProducer(serialProducer(elements));
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
