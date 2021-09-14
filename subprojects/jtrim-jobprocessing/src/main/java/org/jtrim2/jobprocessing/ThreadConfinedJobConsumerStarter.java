package org.jtrim2.jobprocessing;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.ReservablePollingQueues;
import org.jtrim2.collections.ReservedElementRef;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.concurrent.collections.TerminableQueue;
import org.jtrim2.concurrent.collections.TerminableQueues;
import org.jtrim2.concurrent.collections.TerminatedQueueException;
import org.jtrim2.utils.ExceptionHelper;

final class ThreadConfinedJobConsumerStarter<T> implements StatefulJobConsumerStarter<T> {
    private static final Logger LOGGER = Logger.getLogger(ThreadConfinedJobConsumerStarter.class.getName());

    private final StatefulJobConsumerStarter<? super T> wrappedFactory;
    private final Supplier<ExecutorRef> executorProvider;
    private final int consumerThreadCount;
    private final int totalQueueCapacity;

    public ThreadConfinedJobConsumerStarter(
            Supplier<ExecutorRef> executorProvider,
            int consumerThreadCount,
            int extraQueueCapacity,
            StatefulJobConsumerStarter<? super T> wrappedFactory) {

        this.wrappedFactory = Objects.requireNonNull(wrappedFactory, "wrappedFactory");
        this.executorProvider = Objects.requireNonNull(executorProvider, "executorProvider");
        this.consumerThreadCount = ExceptionHelper
                .checkArgumentInRange(consumerThreadCount, 1, Integer.MAX_VALUE, "consumerThreadCount");
        this.totalQueueCapacity = consumerThreadCount + ExceptionHelper
                .checkArgumentInRange(extraQueueCapacity, 0, Integer.MAX_VALUE, "extraQueueCapacity");
    }

    @Override
    public StatefulJobConsumer<T> startConsumer(CancellationToken cancelToken) throws Exception {
        Objects.requireNonNull(cancelToken, "cancelToken");

        ExecutorRef executorRef = executorProvider.get();

        JobConsumerImpl<T> result = new JobConsumerImpl<>(cancelToken, executorRef, totalQueueCapacity, wrappedFactory);
        result.start(consumerThreadCount);
        return result;
    }

    private static final class JobConsumerImpl<T> implements StatefulJobConsumer<T> {
        private final CancellationToken externalCancelToken;
        private final CancellationSource startCancellation;
        private final CancellationToken startCancelToken;
        private final ExecutorRef executorRef;
        private final TerminableQueue<T> queue;
        private final StatefulJobConsumerStarter<? super T> wrappedFactory;
        private final BackgroundWorkerManager consumerManager;
        private ConsumerCompletionStatus receivedFinalStatus;
        private final AtomicReference<Throwable> firstConsumerFailureRef;

        public JobConsumerImpl(
                CancellationToken cancelToken,
                ExecutorRef executorRef,
                int queueCapacity,
                StatefulJobConsumerStarter<? super T> wrappedFactory) {

            this.externalCancelToken = Objects.requireNonNull(cancelToken, "cancelToken");
            this.startCancellation = Cancellation.createChildCancellationSource(cancelToken);
            this.startCancelToken = startCancellation.getToken();
            this.executorRef = Objects.requireNonNull(executorRef, "executorRef");
            this.queue = TerminableQueues.withWrappedQueue(ReservablePollingQueues.createFifoQueue(queueCapacity));
            this.wrappedFactory = Objects.requireNonNull(wrappedFactory, "wrappedFactory");
            this.firstConsumerFailureRef = new AtomicReference<>();
            this.consumerManager = new BackgroundWorkerManager(
                    executorRef.getExecutor(),
                    queue::shutdown,
                    this::setConsumerFailure
            );
        }

        public void start(int consumerThreadCount) {
            consumerManager.startWorkers(startCancelToken, consumerThreadCount, this::pollLoop);
        }

        private void pollLoop(CancellationToken cancelToken) throws Exception {
            Throwable failure = null;

            StatefulJobConsumer<? super T> consumer = wrappedFactory.startConsumer(cancelToken);
            try {
                pollLoopUnsafe(cancelToken, consumer);
            } catch (Throwable ex) {
                setConsumerFailure(ex);
                failure = ex;
            }
            finishConsumer(consumer, failure);
        }

        private void pollLoopUnsafe(
                CancellationToken cancelToken,
                StatefulJobConsumer<? super T> consumer) throws Exception {

            while (true) {
                ReservedElementRef<T> jobRef;
                try {
                    jobRef = queue.takeButKeepReserved(cancelToken);
                } catch (TerminatedQueueException ex) {
                    return;
                }

                try {
                    T job = jobRef.element();
                    consumer.processJob(job);
                } finally {
                    jobRef.release();
                }
            }
        }

        private void setConsumerFailure(Throwable failure) {
            try {
                boolean updated = firstConsumerFailureRef.compareAndSet(null, failure);
                queue.shutdown();
                startCancellation.getController().cancel();

                if (!updated && AsyncTasks.isError(failure)) {
                    Throwable firstConsumerFailure = firstConsumerFailureRef.get();
                    if (AsyncTasks.isCanceled(firstConsumerFailure)) {
                        LOGGER.log(Level.SEVERE, "Cancellation exception hiding other exception.", failure);
                    }
                }
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Failed to shutdown consumers.", ex);
            }
        }

        private void finishConsumer(
                StatefulJobConsumer<?> consumer,
                Throwable threadFailure) throws Exception {

            ConsumerCompletionStatus status = selectStatus(threadFailure);
            consumer.finishProcessing(status);
        }

        private Throwable tryGetFailure(Throwable mainFailure) {
            if (AsyncTasks.isError(mainFailure)) {
                return mainFailure;
            }

            Throwable consumerFailure = firstConsumerFailureRef.get();
            return consumerFailure != null ? consumerFailure : mainFailure;
        }

        private ConsumerCompletionStatus selectStatus(Throwable threadFailure) {
            ConsumerCompletionStatus totalStatus = receivedFinalStatus;

            Throwable consumerFailure = tryGetFailure(threadFailure);

            // No need to add startFailure as suppressed, because it was rethrown.
            ConsumerCompletionStatus threadStatus = ConsumerCompletionStatus.of(
                    AsyncTasks.isCanceled(consumerFailure) && externalCancelToken.isCanceled(),
                    consumerFailure
            );

            if (totalStatus == null) {
                return threadStatus;
            }

            if (threadStatus.isSuccess() && !totalStatus.isSuccess()) {
                return totalStatus;
            }

            if (totalStatus.isFailed() && !threadStatus.isFailed()) {
                return ConsumerCompletionStatus.of(false, totalStatus.tryGetError());
            } else if (threadFailure != null) {
                totalStatus
                        .getErrorIfFailed()
                        .ifPresent(threadFailure::addSuppressed);
            }

            return threadStatus;
        }

        @Override
        public void processJob(T job) {
            try {
                queue.put(externalCancelToken, job);
            } catch (TerminatedQueueException ex) {
                Throwable consumerFailure = tryGetFailure(null);
                if (AsyncTasks.isError(consumerFailure)) {
                    throw new JobProcessingException(consumerFailure);
                }

                Throwable cause = consumerFailure != null ? consumerFailure : ex;

                if (externalCancelToken.isCanceled()) {
                    throw new OperationCanceledException(cause);
                } else if (receivedFinalStatus != null) {
                    throw new IllegalStateException(cause);
                } else {
                    throw new JobProcessingException(cause);
                }
            }
        }

        @Override
        public void finishProcessing(ConsumerCompletionStatus finalStatus) {
            RuntimeException wrongArgumentException;

            ConsumerCompletionStatus fixedStatus;
            if (finalStatus == null) {
                // This is an error of the caller, but we still want to stop the jobs to avoid
                // stucking threads.
                wrongArgumentException = new NullPointerException("finalStatus");
                fixedStatus = ConsumerCompletionStatus.failed(wrongArgumentException);
            } else {
                wrongArgumentException = null;
                fixedStatus = finalStatus;
            }

            receivedFinalStatus = fixedStatus;
            queue.shutdown();
            consumerManager.waitForWorkers();
            queue.clear();
            executorRef.finishUsage();

            Throwable consumerFailure = tryGetFailure(null);
            if (wrongArgumentException != null) {
                if (consumerFailure != null) {
                    wrongArgumentException.addSuppressed(consumerFailure);
                }
                throw wrongArgumentException;
            }

            if (consumerFailure != null) {
                if (AsyncTasks.isCanceled(consumerFailure) && externalCancelToken.isCanceled()) {
                    throw new OperationCanceledException(consumerFailure);
                }
                throw new JobProcessingException(consumerFailure);
            }
        }
    }

}
