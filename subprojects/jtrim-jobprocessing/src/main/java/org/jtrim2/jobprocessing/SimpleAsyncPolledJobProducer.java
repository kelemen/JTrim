package org.jtrim2.jobprocessing;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.TaskExecutor;

final class SimpleAsyncPolledJobProducer<T> implements AsyncPolledJobProducer<T> {
    private final TaskExecutor executor;
    private final PolledJobProducer<T> syncProducer;

    public SimpleAsyncPolledJobProducer(TaskExecutor executor, PolledJobProducer<T> syncProducer) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.syncProducer = Objects.requireNonNull(syncProducer, "syncProducer");
    }

    @Override
    public CompletionStage<T> getNextJob(CancellationToken cancelToken) {
        return executor.executeFunction(cancelToken, taskCancelToken -> syncProducer.getNextJob());
    }

    @Override
    public CompletionStage<Void> closeAsync() {
        return executor.execute(Cancellation.UNCANCELABLE_TOKEN, cancelToken -> syncProducer.close());
    }
}
