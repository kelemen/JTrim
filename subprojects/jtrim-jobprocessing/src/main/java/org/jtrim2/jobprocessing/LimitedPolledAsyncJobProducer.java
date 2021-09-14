package org.jtrim2.jobprocessing;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.utils.ExceptionHelper;

final class LimitedPolledAsyncJobProducer<T> implements AsyncJobProducer<T> {
    private static final Logger LOGGER = Logger.getLogger(LimitedPolledAsyncJobProducer.class.getName());

    private final int outstandingJobCount;
    private final AsyncPolledJobProducerStarter<? extends T> producerFactory;

    public LimitedPolledAsyncJobProducer(
            int maxOutstandingJobs,
            AsyncPolledJobProducerStarter<? extends T> producerFactory) {

        this.outstandingJobCount = ExceptionHelper
                .checkArgumentInRange(maxOutstandingJobs, 1, Integer.MAX_VALUE, "outstandingJobCount");
        this.producerFactory = Objects.requireNonNull(producerFactory, "producerFactory");
    }

    private static CompletableFuture<Void> completed() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.complete(null);
        return future;
    }

    private static <T> CompletableFuture<T> failed(Throwable failure) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(failure);
        return future;
    }

    private static <T> CompletionStage<Void> failedConsumerStart(
            Throwable rootCause,
            AsyncJobConsumer<? super T> consumer) {

        CompletionStage<Void> consumerFuture;

        try {
            if (consumer != null) {
                consumerFuture = consumer.finishProcessing(ConsumerCompletionStatus.failed(rootCause));
            } else {
                consumerFuture = completed();
            }
        } catch (Throwable ex) {
            consumerFuture = failed(ex);
        }

        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        consumerFuture.whenComplete((result, failure) -> {
            if (failure != null) {
                rootCause.addSuppressed(failure);
            }
            failedFuture.completeExceptionally(rootCause);
        });
        return failedFuture;
    }

    @Override
    public CompletionStage<Void> startTransfer(
            CancellationToken cancelToken,
            AsyncJobConsumerStarter<? super T> consumerFactory) {

        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(consumerFactory, "consumerFactory");

        return consumerFactory.startConsumer(cancelToken)
                .thenCompose(consumer -> {
                    try {
                        return startTransfer(cancelToken, outstandingJobCount, producerFactory, consumer);
                    } catch (Throwable ex) {
                        return failedConsumerStart(ex, consumer);
                    }
                });
    }

    private static <P extends T, T> CompletionStage<Void> startTransfer(
            CancellationToken cancelToken,
            int outstandingJobCount,
            AsyncPolledJobProducerStarter<P> producerFactory,
            AsyncJobConsumer<? super T> consumer) {

        // There is no need for thread safety, but AtomicReference is a convenient container.
        AtomicReference<AsyncJobConsumer<? super T>> forgottenConsumerRef = new AtomicReference<>(consumer);

        return producerFactory
                .startProducer(cancelToken)
                .<CompletionStage<Void>>thenApply(producer -> {
                    if (producer == null) {
                        return failed(new NullPointerException("producer"));
                    }

                    JobScheduler<T> scheduler = new JobScheduler<>(
                                cancelToken,
                                producer,
                                consumer,
                                outstandingJobCount
                    );

                    forgottenConsumerRef.set(null);
                    return scheduler.start();
                })
                .<CompletionStage<Void>>handle((startFuture, failure) -> {
                    if (failure != null) {
                        AsyncJobConsumer<? super T> forgottenConsumer = forgottenConsumerRef.getAndSet(null);
                        if (forgottenConsumer != null) {
                            return failedConsumerStart(failure, forgottenConsumer);
                        } else {
                            return failed(failure);
                        }
                    } else {
                        return startFuture;
                    }
                })
                .thenCompose(Function.identity());
    }

    private static final class JobScheduler<T> {
        private final ReentrantLock mainLock;
        private final CompletableFuture<Void> future;
        private final CancellationToken schedulingCancelToken;

        private final int outstandingJobCount;
        private int currentOutstandinJobs;
        private int currentOutstandingPolls;
        /** Means if we are to no longer read from the producer for whatever reason. */
        private boolean producerFinished;

        private AsyncPolledJobProducer<? extends T> producer;
        private AsyncJobConsumer<? super T> consumer;

        private ConditionalTaskCompleter<AsyncPolledJobProducer<? extends T>> producerCompleter;
        private ConditionalTaskCompleter<AsyncJobConsumer<? super T>> consumerCompleter;
        private ConditionalTaskCompleter<CompletableFuture<Void>> processCompleter;

        private final AtomicReference<Throwable> firstFailureRef;

        public JobScheduler(
                CancellationToken cancelToken,
                AsyncPolledJobProducer<? extends T> producer,
                AsyncJobConsumer<? super T> consumer,
                int outstandingJobCount) {

            this.mainLock = new ReentrantLock();
            this.future = new CompletableFuture<>();
            this.schedulingCancelToken = Objects.requireNonNull(cancelToken, "cancelToken");

            this.outstandingJobCount = outstandingJobCount;
            this.currentOutstandinJobs = 0;
            this.currentOutstandingPolls = 0;
            this.producerFinished = false;

            this.producer = Objects.requireNonNull(producer, "producer");
            this.consumer = Objects.requireNonNull(consumer, "consumer");

            this.firstFailureRef = new AtomicReference<>(null);
        }

        private void setupCompleters() {
            assert processCompleter == null;

            producerCompleter = new ConditionalTaskCompleter<>(this::finishProducerEventually);
            consumerCompleter = new ConditionalTaskCompleter<>(this::finishConsumerEventually);
            processCompleter = new ConditionalTaskCompleter<>(this::finishProcessNow);
        }

        public CompletionStage<Void> start() {
            // TODO: Design error handling during startup, and don't forget to close the producer and consumer
            setupCompleters();

            for (int i = 0; i < outstandingJobCount; i++) {
                submitOneToProducer();
            }

            return future;
        }

        private void setFirstFailure(Throwable failure) {
            if (!firstFailureRef.compareAndSet(null, failure)) {
                if (!AsyncTasks.isCanceled(failure) || !AsyncTasks.isCanceled(firstFailureRef.get())) {
                    LOGGER.log(
                            Level.WARNING,
                            "Suppressed exception during asyncronous job, because there was one already reported",
                            failure
                    );
                }
            }
        }

        private AsyncPolledJobProducer<? extends T> tryCompleteProducerUnlocked() {
            assert mainLock.isHeldByCurrentThread();
            return producerFinished && currentOutstandingPolls <= 0 ? producer : null;
        }

        private void finishProducerEventually(AsyncPolledJobProducer<? extends T> finishedProducer) {
            try {
                finishedProducer
                        .closeAsync()
                        .whenComplete((result, failure) -> {
                            if (failure != null) {
                                setFirstFailure(failure);
                            }
                            onDoneProducer();
                        });
            } catch (Throwable ex) {
                setFirstFailure(ex);
                onDoneProducer();
            }
        }

        private void onDoneProducer() {
            CompletableFuture<Void> doneFuture;

            mainLock.lock();
            try {
                producer = null;
                doneFuture = consumer == null ? future : null;
            } finally {
                mainLock.unlock();
            }

            processCompleter.tryComplete(doneFuture);
        }

        private AsyncJobConsumer<? super T> tryCompleteConsumerUnlocked() {
            assert mainLock.isHeldByCurrentThread();
            return producerFinished && currentOutstandinJobs <= 0 ? consumer : null;
        }

        private void finishConsumerEventually(AsyncJobConsumer<? super T> finishedConsumer) {
            Throwable firstFailure = firstFailureRef.get();
            ConsumerCompletionStatus status = ConsumerCompletionStatus.of(
                    AsyncTasks.isCanceled(firstFailure),
                    firstFailure
            );

            try {
                finishedConsumer
                        .finishProcessing(status)
                        .whenComplete((result, failure) -> {
                            if (failure != null) {
                                setFirstFailure(failure);
                            }
                            onFinishProcessingDone();
                        });
            } catch (Throwable ex) {
                setFirstFailure(ex);
                onFinishProcessingDone();
            }
        }

        private void onFinishProcessingDone() {
            CompletableFuture<Void> doneFuture;
            mainLock.lock();
            try {
                consumer = null;
                doneFuture = producer == null ? future : null;
            } finally {
                mainLock.unlock();
            }

            processCompleter.tryComplete(doneFuture);
        }

        private void finishProcessNow(CompletableFuture<Void> finishedFuture) {
            Throwable firstFailure = firstFailureRef.get();

            try {
                if (firstFailure == null) {
                    finishedFuture.complete(null);
                } else {
                    finishedFuture.completeExceptionally(firstFailure);
                }
            } catch (Throwable ex) {
                setFirstFailure(ex);
            }
        }

        private AsyncPolledJobProducer<? extends T> preparePolling() {
            mainLock.lock();
            try {
                AsyncPolledJobProducer<? extends T> selectedProducer = producerFinished ? null : producer;
                if (selectedProducer == null) {
                    return null;
                }
                currentOutstandinJobs++;
                currentOutstandingPolls++;
                return selectedProducer;
            } finally {
                mainLock.unlock();
            }
        }

        private void finishPolling() {
            AsyncPolledJobProducer<? extends T> doneProducer;
            AsyncJobConsumer<? super T> doneConsumer;

            mainLock.lock();
            try {
                currentOutstandingPolls--;
                doneProducer = tryCompleteProducerUnlocked();
                doneConsumer = tryCompleteConsumerUnlocked();
            } finally {
                mainLock.unlock();
            }

            tryComplete(doneProducer, doneConsumer);
        }

        private void doneOne(Throwable failure) {
            if (failure != null) {
                setFirstFailure(failure);
            }

            AsyncPolledJobProducer<? extends T> doneProducer;
            AsyncJobConsumer<? super T> doneConsumer;

            mainLock.lock();
            try {
                if (failure != null) {
                    producerFinished = true;
                }
                currentOutstandinJobs--;
                doneProducer = tryCompleteProducerUnlocked();
                doneConsumer = tryCompleteConsumerUnlocked();
            } finally {
                mainLock.unlock();
            }

            tryComplete(doneProducer, doneConsumer);

            if (failure == null) {
                submitOneToProducer();
            }
        }

        private void onReceivedJob(T job, AtomicBoolean handled) {
            if (job != null) {
                consumer.processJob(job)
                        .whenComplete((result, failure) -> {
                            if (handled.compareAndSet(false, true)) {
                                doneOne(failure);
                            }
                        });

                finishPolling();
            } else {
                if (!handled.compareAndSet(false, true)) {
                    return;
                }

                AsyncPolledJobProducer<? extends T> doneProducer;
                AsyncJobConsumer<? super T> doneConsumer;

                mainLock.lock();
                try {
                    producerFinished = true;
                    currentOutstandinJobs--;
                    currentOutstandingPolls--;
                    doneProducer = tryCompleteProducerUnlocked();
                    doneConsumer = tryCompleteConsumerUnlocked();
                } finally {
                    mainLock.unlock();
                }

                tryComplete(doneProducer, doneConsumer);
            }
        }

        private void submitOneToProducer() {
            AsyncPolledJobProducer<? extends T> selectedProducer = preparePolling();
            if (selectedProducer == null) {
                return;
            }

            // This is just to be fool proof, it is not really possible to call this twice,
            // unless whenComplete behaves horribly.
            AtomicBoolean handled = new AtomicBoolean(false);

            Consumer<Throwable> pollFailureTask = failure -> {
                if (handled.compareAndSet(false, true)) {
                    mainLock.lock();
                    try {
                        producerFinished = true;
                    } finally {
                        mainLock.unlock();
                    }

                    doneOne(failure);
                    finishPolling();
                }
            };

            try {
                selectedProducer
                        .getNextJob(schedulingCancelToken)
                        .whenComplete((job, failure) -> {
                            if (failure != null) {
                                pollFailureTask.accept(failure);
                            } else {
                                onReceivedJob(job, handled);
                            }
                        });
            } catch (Throwable ex) {
                pollFailureTask.accept(ex);
            }
        }

        private void tryComplete(
                AsyncPolledJobProducer<? extends T> doneProducer,
                AsyncJobConsumer<? super T> doneConsumer) {

            producerCompleter.tryComplete(doneProducer);
            consumerCompleter.tryComplete(doneConsumer);
        }
    }

    private static final class ConditionalTaskCompleter<T> {
        private final AtomicReference<Consumer<? super T>> completionActionRef;

        public ConditionalTaskCompleter(Consumer<? super T> completionAction) {
            this.completionActionRef
                    = new AtomicReference<>(Objects.requireNonNull(completionAction, "completionAction"));
        }

        public void tryComplete(T completedObj) {
            if (completedObj == null) {
                return;
            }

            Consumer<? super T> completionAction = completionActionRef.getAndSet(null);
            if (completionAction != null) {
                completionAction.accept(completedObj);
            }
        }
    }
}
