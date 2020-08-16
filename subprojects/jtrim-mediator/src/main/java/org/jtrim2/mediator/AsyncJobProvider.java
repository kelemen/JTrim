package org.jtrim2.mediator;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.utils.ExceptionHelper;

final class AsyncJobProvider<T> implements JobProvider<T> {
    private final TaskExecutor executor;
    private final Iterable<? extends T> src;
    private final int maxOutstandingJobs;

    public AsyncJobProvider(
            TaskExecutor executor,
            int maxOutstandingJobs,
            Iterable<? extends T> src) {

        this.executor = Objects.requireNonNull(executor, "executor");
        this.src = Objects.requireNonNull(src, "src");
        this.maxOutstandingJobs = ExceptionHelper
                .checkArgumentInRange(maxOutstandingJobs, 1, Integer.MAX_VALUE, "maxOutstandingJobs");
    }

    @Override
    public CompletionStage<Void> startProvider(
            CancellationToken cancelToken,
            JobConsumerFactory<? super T> consumerFactory) {

        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(consumerFactory, "consumerFactory");

        Iterator<? extends T> srcItr = Objects.requireNonNull(src.iterator(), "src.iterator()");

        JobProviderScheduler<T> providerState;
        try {
            JobConsumer<? super T> consumer = consumerFactory.startConsumer(cancelToken);
            providerState = new JobProviderScheduler<>(
                    cancelToken,
                    executor,
                    srcItr,
                    consumer
            );
        } catch (Throwable ex) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            result.completeExceptionally(ex);
            return result;
        }

        try {
            for (int i = 0; i < maxOutstandingJobs; i++) {
                if (!providerState.scheduleNextJob()) {
                    break;
                }
            }
        } finally {
            // We have to do this in case there was nothing scheduled.
            providerState.completeIfNeeded();
        }

        return providerState.getFuture();
    }

    private static final class JobProviderScheduler<T> {
        private final Lock mainLock;
        private final CompletableFuture<Void> future;
        private final CancellationToken cancelToken;
        private final TaskExecutor executor;
        private final Iterator<? extends T> src;
        private final JobConsumer<? super T> consumer;
        private final AtomicReference<Throwable> firstFailure;
        private final AtomicBoolean completed;
        private boolean doneIterator;
        private int currentOutstandinJobs;

        public JobProviderScheduler(
                CancellationToken cancelToken,
                TaskExecutor executor,
                Iterator<? extends T> src,
                JobConsumer<? super T> consumer) {

            this.mainLock = new ReentrantLock();
            this.future = new CompletableFuture<>();
            this.cancelToken = Objects.requireNonNull(cancelToken, "cancelToken");
            this.executor = Objects.requireNonNull(executor, "executor");
            this.currentOutstandinJobs = 0;
            this.doneIterator = false;
            this.src = Objects.requireNonNull(src, "src");
            this.consumer = Objects.requireNonNull(consumer, "consumer");
            this.firstFailure = new AtomicReference<>(null);
            this.completed = new AtomicBoolean(false);
        }

        public CompletableFuture<Void> getFuture() {
            return future;
        }

        public boolean scheduleNextJob() {
            T job = null;
            Throwable queueFailure = null;
            mainLock.lock();
            try {
                if (src.hasNext()) {
                    job = src.next();
                    currentOutstandinJobs++;
                } else {
                    doneIterator = true;
                    return false;
                }
            } catch (Throwable ex) {
                doneIterator = true;
                queueFailure = ex;
            } finally {
                mainLock.unlock();
            }

            if (queueFailure != null) {
                setFailure(queueFailure);
                return false;
            } else {
                scheduleJob(job);
                return true;
            }
        }

        public void completeIfNeeded() {
            boolean needComplete;
            mainLock.lock();
            try {
                // Note that if this counter reaches zero, then we will not schedule anything anymore,
                // because the only way to decrease this value is when we don't increment it for the next job.
                // And not having a next job means, that the queue is empty, and we can only complete jobs.
                needComplete = currentOutstandinJobs <= 0;
            } finally {
                mainLock.unlock();
            }
            if (needComplete) {
                completeNow();
            }
        }

        private void scheduleJob(T job) {
            AtomicBoolean finishedTask = new AtomicBoolean(false);
            BiConsumer<Void, Throwable> completionTask = (result, newTaskFailure) -> {
                if (finishedTask.compareAndSet(false, true)) {
                    doneOne(newTaskFailure);
                }
            };

            try {
                executor
                        .execute(cancelToken, taskCancelToken -> {
                            consumer.processJob(taskCancelToken, job);
                        })
                        .whenComplete(completionTask);
            } catch (Throwable ex) {
                completionTask.accept(null, ex);
            }
        }

        public void setFailure(Throwable failure) {
            // TODO: We might want to report the discarded failure to the client.
            // We could suppress the discarded exception, however that might cause a long
            // exception list, which we should avoid.
            firstFailure.compareAndSet(null, failure);
        }

        public void doneOne(Throwable completionFailure) {
            if (completionFailure != null) {
                setFailure(completionFailure);
            }

            T job = null;
            boolean hasJob = false;
            boolean completeProviding = false;

            Throwable queueFailure = null;
            mainLock.lock();
            try {
                currentOutstandinJobs--;

                if (doneIterator || !src.hasNext()) {
                    doneIterator = true;
                    if (currentOutstandinJobs <= 0) {
                        completeProviding = true;
                    }
                } else {
                    job = src.next();
                    hasJob = true;
                    currentOutstandinJobs++;
                }
            } catch (Throwable ex) {
                doneIterator = true;
                queueFailure = ex;
            } finally {
                mainLock.unlock();
            }

            if (queueFailure != null) {
                setFailure(queueFailure);
                return;
            }

            if (hasJob) {
                scheduleJob(job);
            } else if (completeProviding) {
                completeNow();
            }
        }

        private void completeNow() {
            if (!completed.compareAndSet(false, true)) {
                return;
            }

            Throwable failure = firstFailure.get();
            try {
                consumer.finishProcessing(ConsumerCompletionStatus.of(
                        failure != null && cancelToken.isCanceled(),
                        failure
                ));
            } catch (Throwable finishEx) {
                if (failure != null) {
                    failure.addSuppressed(finishEx);
                } else {
                    failure = finishEx;
                }
            }

            if (failure != null) {
                future.complete(null);
            } else {
                future.completeExceptionally(failure);
            }
        }
    }
}
