package org.jtrim2.mediator;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.collections.Streamable;

final class SyncJobProvider<T> implements JobProvider<T> {
    private final Streamable<T> src;

    public SyncJobProvider(Streamable<T> src) {
        this.src = Objects.requireNonNull(src, "src");
    }

    @Override
    public CompletionStage<Void> startProvider(
            CancellationToken cancelToken,
            JobConsumerFactory<? super T> consumerFactory) {

        Objects.requireNonNull(cancelToken, "cancelToken");
        Objects.requireNonNull(consumerFactory, "consumerFactory");

        JobConsumer<? super T> consumer = null;
        Throwable failure = null;
        try {
            consumer = consumerFactory.startConsumer(cancelToken);
            JobConsumer<? super T> consumerCapture = Objects.requireNonNull(consumer, "consumerFactory.startConsumer");
            src.forEach(element -> {
                try {
                    consumerCapture.processJob(cancelToken, element);
                } catch (Exception ex) {
                    if (ex instanceof RuntimeException) {
                        throw (RuntimeException) ex;
                    } else {
                        throw new JobProcessorException(ex);
                    }
                }
            });
        } catch (Throwable ex) {
            failure = ex instanceof JobProcessorException ? ex.getCause() : ex;
        } finally {
            if (consumer != null) {
                try {
                    consumer.finishProcessing(ConsumerCompletionStatus.of(
                            failure != null && cancelToken.isCanceled(),
                            failure
                    ));
                } catch (Throwable ex) {
                    if (failure != null) {
                        failure.addSuppressed(ex);
                    } else {
                        failure = ex;
                    }
                }
            }
        }

        CompletableFuture<Void> result = new CompletableFuture<>();
        if (failure != null) {
            result.completeExceptionally(failure);
        } else {
            result.complete(null);
        }
        return result;
    }

    private static class JobProcessorException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public JobProcessorException(Throwable cause) {
            super(cause);
        }
    }
}
