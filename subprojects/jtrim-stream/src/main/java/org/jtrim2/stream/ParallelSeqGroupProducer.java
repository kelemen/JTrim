package org.jtrim2.stream;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationController;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.collections.ReservablePollingQueues;
import org.jtrim2.collections.ReservedElementRef;
import org.jtrim2.concurrent.collections.TerminableQueue;
import org.jtrim2.concurrent.collections.TerminableQueues;
import org.jtrim2.concurrent.collections.TerminatedQueueException;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

final class ParallelSeqGroupProducer<T> implements SeqGroupProducer<T> {
    private static final Logger LOGGER = Logger.getLogger(ParallelSeqGroupProducer.class.getName());

    private final SeqGroupProducer<? extends T> srcSeqGroupProducer;
    private final Supplier<ExecutorRef> executorProvider;
    private final int consumerThreadCount;
    private final int totalQueueCapacity;

    public ParallelSeqGroupProducer(
            Supplier<ExecutorRef> executorProvider,
            int backgroundThreadCount,
            int extraQueueCapacity,
            SeqGroupProducer<? extends T> srcSeqGroupProducer) {

        this.srcSeqGroupProducer = Objects.requireNonNull(srcSeqGroupProducer, "srcSeqGroupProducer");
        this.executorProvider = Objects.requireNonNull(executorProvider, "executorProvider");
        this.consumerThreadCount = ExceptionHelper
                .checkArgumentInRange(backgroundThreadCount, 1, Integer.MAX_VALUE, "backgroundThreadCount");
        this.totalQueueCapacity = backgroundThreadCount + ExceptionHelper
                .checkArgumentInRange(extraQueueCapacity, 0, Integer.MAX_VALUE, "extraQueueCapacity");
    }

    @Override
    public void transferAll(CancellationToken cancelToken, SeqConsumer<? super T> seqConsumer) throws Exception {
        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(seqConsumer, "seqConsumer");

        Throwable toThrow = null;
        ExecutorRef executorRef = executorProvider.get();
        try {
            CancellationSource cancellation = Cancellation.createChildCancellationSource(cancelToken);
            SeqGroupProducer<T> parallelProducer = new UnsafeParallelSeqGroupProducer<>(
                    srcSeqGroupProducer,
                    cancellation.getController(),
                    executorRef.getExecutor(),
                    consumerThreadCount,
                    totalQueueCapacity
            );

            parallelProducer.transferAll(cancellation.getToken(), seqConsumer);
        } catch (Throwable ex) {
            toThrow = ex;
        }

        try {
            executorRef.finishUsage();
        } catch (Throwable ex) {
            toThrow = ExceptionCollector.updateException(toThrow, ex);
        }

        ExceptionHelper.rethrowCheckedIfNotNull(toThrow, Exception.class);
    }

    private static final class UnsafeParallelSeqGroupProducer<T> implements SeqGroupProducer<T> {
        private final SeqGroupProducer<? extends T> srcSeqGroupProducer;
        private final CancellationController cancelController;
        private final int consumerThreadCount;
        private final TerminableQueue<T> queue;
        private final BackgroundWorkerManager queuePollerManager;
        private final ExceptionCollector consumerFailureRef;
        private volatile Throwable producerFailure;
        private volatile boolean producerFinishedNormally;

        public UnsafeParallelSeqGroupProducer(
                SeqGroupProducer<? extends T> srcSeqGroupProducer,
                CancellationController cancelController,
                TaskExecutor executor,
                int consumerThreadCount,
                int totalQueueCapacity) {

            this.srcSeqGroupProducer = Objects.requireNonNull(srcSeqGroupProducer, "srcSeqGroupProducer");
            this.cancelController = Objects.requireNonNull(cancelController, "cancelController");
            this.consumerThreadCount = consumerThreadCount;
            this.queue = TerminableQueues
                    .withWrappedQueue(ReservablePollingQueues.createFifoQueue(totalQueueCapacity));
            this.queuePollerManager = new BackgroundWorkerManager(executor, queue::shutdown, this::setConsumerFailure);
            this.consumerFailureRef = new ExceptionCollector();
            this.producerFailure = null;
            this.producerFinishedNormally = false;
        }

        private void setConsumerFailure(Throwable failure) {
            try {
                consumerFailureRef.setFirstFailure(failure);
                queue.shutdown();
                cancelController.cancel();
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Failed to shutdown consumers.", ex);
            }
        }

        private SeqProducer<T> pollLoop() throws Exception {
            return (cancelToken, consumer) -> {
                while (true) {
                    ReservedElementRef<T> jobRef;
                    try {
                        jobRef = queue.takeButKeepReserved(cancelToken);
                    } catch (TerminatedQueueException ex) {
                        break;
                    }

                    try {
                        consumer.processElement(jobRef.element());
                    } finally {
                        jobRef.release();
                    }
                }

                ExceptionHelper.rethrowCheckedIfNotNull(producerFailure, Exception.class);
                ExceptionHelper.rethrowCheckedIfNotNull(consumerFailureRef.getLatest(), Exception.class);
                if (!producerFinishedNormally) {
                    throw new AssertionError("Internal-error: Unfinished producer.");
                }
            };
        }

        public void consume(CancellationToken cancelToken) throws Exception {
            srcSeqGroupProducer.transferAllSimple(cancelToken, job -> {
                try {
                    queue.put(cancelToken, job);
                } catch (OperationCanceledException ex) {
                    // If there was a failure, then we are cancelling the process, so cancellation exceptions
                    // are no longer relevant.
                    ExceptionHelper.rethrowCheckedIfNotNull(consumerFailureRef.getLatest(), Exception.class);
                    throw ex;
                } catch (TerminatedQueueException ex) {
                    ExceptionHelper.rethrowCheckedIfNotNull(consumerFailureRef.getLatest(), Exception.class);

                    // FIXME: This could happen if there was a late call to this method, or if the consumers did
                    // start pulling jobs. We should have a better exception for the latter case.
                    throw new Exception("Consumer did not pull jobs.");
                }
            });
        }

        @Override
        public void transferAll(
                CancellationToken cancelToken,
                SeqConsumer<? super T> seqConsumer) throws Exception {

            Objects.requireNonNull(cancelToken, "cancelToken");
            Objects.requireNonNull(seqConsumer, "serialConsumer");

            Throwable toThrow = null;
            try {
                Thread mainThread = Thread.currentThread();
                queuePollerManager.startWorkers(cancelToken, consumerThreadCount, taskCancelToken -> {
                    if (Thread.currentThread() == mainThread) {
                        String message = "Executor must not execute jobs synchronously to avoid dead-lock.";
                        setConsumerFailure(new IllegalStateException(message));
                        return;
                    }

                    seqConsumer.consumeAll(taskCancelToken, pollLoop());
                });

                consume(cancelToken);
                producerFinishedNormally = true;
            } catch (Throwable ex) {
                producerFailure = ex;
                toThrow = ex;
            }

            try {
                queue.shutdown();
                // FIXME: Too early exception from queuePollerManager would make us block forever.
                //        queuePollerManager should realize this, and should not block if startWorkers was not called.
                queuePollerManager.waitForWorkers();
                queue.clear();

                toThrow = consumerFailureRef.consumeLatestAndUpdate(toThrow);
            } catch (Throwable ex) {
                toThrow = consumerFailureRef.consumeLatestAndUpdate(toThrow);
                toThrow = ExceptionCollector.updateException(toThrow, ex);
            }

            ExceptionHelper.rethrowCheckedIfNotNull(toThrow, Exception.class);
        }
    }
}