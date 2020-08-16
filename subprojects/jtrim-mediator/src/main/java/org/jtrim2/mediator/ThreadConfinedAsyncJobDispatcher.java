package org.jtrim2.mediator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.ReservablePollingQueues;
import org.jtrim2.collections.ReservedElementRef;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.concurrent.collections.TerminableQueue;
import org.jtrim2.concurrent.collections.TerminableQueues;
import org.jtrim2.concurrent.collections.TerminatedQueueException;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

final class ThreadConfinedAsyncJobDispatcher<T> implements AsyncJobDispatcher<T> {
    private final CancellationSource cancellation;
    private final int numberOfConsumers;
    private final JobConsumerFactory<T> batchJobProcessorFactory;
    private final TerminableQueue<T> batchQueue;
    private final AtomicReference<DispatcherState> dispatcherStateRef;
    private final TaskExecutor executor;
    private final AtomicReference<List<ProcessorFuture>> processorFuturesRef;
    private final ProcessingErrorRef errorRef;

    public ThreadConfinedAsyncJobDispatcher(
            int jobQueueSize,
            int numberOfConsumers,
            JobConsumerFactory<T> batchJobProcessorFactory,
            TaskExecutor executor) {

        this.cancellation = Cancellation.createCancellationSource();
        this.numberOfConsumers = ExceptionHelper
                .checkArgumentInRange(numberOfConsumers, 1, Integer.MAX_VALUE, "numberOfConsumers");
        this.batchJobProcessorFactory = Objects.requireNonNull(batchJobProcessorFactory, "batchJobProcessorFactory");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.batchQueue = TerminableQueues
                .withWrappedQueue(ReservablePollingQueues.createFifoQueue(jobQueueSize));
        this.dispatcherStateRef = new AtomicReference<>(DispatcherState.NOT_STARTED);
        // We need to initialize processorFuturesRef to store a new instance, because we rely on it
        // to check if shutting down was quicker than start or not.
        this.processorFuturesRef = new AtomicReference<>(new ArrayList<>());
        this.errorRef = new ProcessingErrorRef();
    }

    @Override
    public void cancelProcessing() {
        cancellation.getController().cancel();
    }

    private void verifyNotStopped() {
        if (dispatcherStateRef.get().isStopped()) {
            throw new IllegalStateException("Trying to add element after shutdown.");
        }
    }

    @Override
    public void dispatchJob(T job) {
        Objects.requireNonNull(job, "job");
        verifyNotStopped();

        batchQueue.put(cancellation.getToken(), job);

        if (dispatcherStateRef.compareAndSet(DispatcherState.NOT_STARTED, DispatcherState.STARTED)) {
            startProcessors();
        }
    }

    private void startProcessors() {
        CancellationToken cancelToken = cancellation.getToken();

        List<ProcessorFuture> prevProcessorFutures = processorFuturesRef.get();

        List<ProcessorFuture> startedFutures = new ArrayList<>(numberOfConsumers);
        for (int i = 0; i < numberOfConsumers; i++) {
            CompletionStage<Void> future = executor.execute(cancelToken, this::pollLoop);
            WaitableSignal doneSignal = new WaitableSignal();

            future.whenComplete((result, error) -> {
                try {
                    errorRef.updateStatus(toCompletionStatus(error));
                } finally {
                    doneSignal.signal();
                }
            });

            startedFutures.add(new ProcessorFuture(future, doneSignal));
        }

        if (!processorFuturesRef.compareAndSet(prevProcessorFutures, startedFutures)) {
            throw new IllegalStateException("The dispatcher was stopped concurrently with starting the processors.");
        }
    }

    private static ConsumerCompletionStatus toCompletionStatus(Throwable error) {
        if (error == null) {
            return ConsumerCompletionStatus.SUCCESS;
        }

        return ConsumerCompletionStatus.of(
                AsyncTasks.isCanceled(error),
                error
        );
    }

    private void pollLoop(CancellationToken cancelToken) {
        boolean success = false;
        try {
            Throwable processingError = null;
            JobConsumer<T> processor = null;

            try {
                processor = batchJobProcessorFactory.startConsumer(cancelToken);
                pollLoopUnsafe(cancelToken, processor);
            } catch (Throwable ex) {
                errorRef.updateStatus(ConsumerCompletionStatus.failed(ex));
                processingError = ex;
            }

            try {
                if (processor != null) {
                    processor.finishProcessing(toCompletionStatus(processingError));
                }
            } catch (Throwable ex) {
                errorRef.updateStatus(ConsumerCompletionStatus.failed(ex));
            }

            success = true;
        } finally {
            if (!success) {
                batchQueue.shutdown();
            }
        }
    }

    private void pollLoopUnsafe(CancellationToken cancelToken, JobConsumer<T> processor) throws Exception {
        Objects.requireNonNull(processor, "processor");

        while (!cancelToken.isCanceled()) {
            ReservedElementRef<T> jobRef;
            try {
                jobRef = batchQueue.takeButKeepReserved(cancelToken);
            } catch (TerminatedQueueException ex) {
                break;
            }

            try {
                T job = jobRef.element();
                processor.processJob(cancelToken, job);
            } finally {
                jobRef.release();
            }
        }
    }

    @Override
    public void shutdown() {
        batchQueue.shutdown();
        dispatcherStateRef.set(DispatcherState.SHUTTED_DOWN);
    }

    @Override
    public void shutdownAndWait(CancellationToken cancelToken) {
        shutdown();
        waitForAllProcessors(cancelToken);
    }

    @Override
    public void close() {
        shutdown();

        if (!processorFuturesRef.get().isEmpty()) {
            errorRef.updateStatus(ConsumerCompletionStatus.failed(
                    new IllegalStateException("Processing started, but did not wait for processors to stop.")));
        }

        errorRef.throwExceptionOnFailure();
    }

    private void waitForAllProcessors(CancellationToken cancelToken) {
        assert dispatcherStateRef.get() == DispatcherState.SHUTTED_DOWN;

        List<ProcessorFuture> currentFutures = processorFuturesRef.get();
        currentFutures.forEach(future -> {
            future.getDoneSignal().waitSignal(cancelToken);
        });
        processorFuturesRef.set(Collections.emptyList());
    }

    private static final class ProcessorFuture {
        private final CompletionStage<?> future;
        private final WaitableSignal doneSignal;

        public ProcessorFuture(CompletionStage<?> future, WaitableSignal doneSignal) {
            this.future = Objects.requireNonNull(future, "future");
            this.doneSignal = Objects.requireNonNull(doneSignal, "doneSignal");
        }

        public CompletionStage<?> getFuture() {
            return future;
        }

        public WaitableSignal getDoneSignal() {
            return doneSignal;
        }
    }

    private static final class ProcessingErrorRef {
        private final Lock statusLock;
        private volatile ConsumerCompletionStatus lastSetStatus;

        public ProcessingErrorRef() {
            this.statusLock = new ReentrantLock();
            this.lastSetStatus = ConsumerCompletionStatus.SUCCESS;
        }

        public void throwExceptionOnFailure() {
            ConsumerCompletionStatus status = lastSetStatus;
            if (status.isSuccess()) {
                return;
            }

            if (status.isCanceled()) {
                throw new OperationCanceledException("Processing was canceled.", status.tryGetError());
            }

            // As is now, there is always an error if we get here, but whatever.
            status.getErrorIfFailed()
                    .ifPresent(currentError -> {
                        throw new AsyncJobProcessingException(currentError);
                    });
        }

        private static ConsumerCompletionStatus mergedStatus(
                ConsumerCompletionStatus base,
                ConsumerCompletionStatus other) {

            Throwable otherError = other.tryGetError();
            if (otherError != null) {
                Throwable baseError = base.tryGetError();
                if (baseError != null) {
                    baseError.addSuppressed(otherError);
                    return base;
                } else {
                    return ConsumerCompletionStatus.of(base.isCanceled(), otherError);
                }
            } else {
                return base;
            }
        }

        private boolean preferPrevStatus(ConsumerCompletionStatus prevStatus, ConsumerCompletionStatus newStatus) {
            return prevStatus.isFailed() || newStatus.isSuccess();
        }

        public void updateStatus(ConsumerCompletionStatus newStatus) {
            statusLock.lock();
            try {
                ConsumerCompletionStatus prevStatus = lastSetStatus;
                if (preferPrevStatus(prevStatus, newStatus)) {
                    lastSetStatus = mergedStatus(prevStatus, newStatus);
                } else {
                    lastSetStatus = mergedStatus(newStatus, prevStatus);
                }
            } finally {
                statusLock.unlock();
            }
        }
    }

    private enum DispatcherState {
        NOT_STARTED,
        STARTED,
        SHUTTED_DOWN;

        public boolean isStopped() {
            return SHUTTED_DOWN == this;
        }
    }
}
