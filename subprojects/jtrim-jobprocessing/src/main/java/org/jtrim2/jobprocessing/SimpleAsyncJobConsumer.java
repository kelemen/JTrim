package org.jtrim2.jobprocessing;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.TaskExecutor;

final class SimpleAsyncJobConsumer<T> implements AsyncJobConsumer<T> {
    private final CancellationToken cancelToken;
    private final TaskExecutor executor;
    private final StatefulJobConsumer<? super T> syncConsumer;

    public SimpleAsyncJobConsumer(
            CancellationToken cancelToken,
            TaskExecutor executor,
            StatefulJobConsumer<? super T> syncConsumer) {

        this.cancelToken = Objects.requireNonNull(cancelToken, "cancelToken");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.syncConsumer = Objects.requireNonNull(syncConsumer, "syncConsumer");
    }

    @Override
    public CompletionStage<Void> processJob(T job) {
        Objects.requireNonNull(job, "job");
        return executor.execute(cancelToken, taskCancelToken -> syncConsumer.processJob(job));
    }

    @Override
    public CompletionStage<Void> finishProcessing(ConsumerCompletionStatus finalStatus) {
        Objects.requireNonNull(finalStatus, "finalStatus");
        return executor.execute(Cancellation.UNCANCELABLE_TOKEN, taskCancelToken -> {
            syncConsumer.finishProcessing(finalStatus);
        });
    }
}
