package org.jtrim2.jobprocessing;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.TaskExecutor;

final class SimpleAsyncPolledJobProducerStarter<T> implements AsyncPolledJobProducerStarter<T> {
    private final TaskExecutor startExecutor;
    private final TaskExecutor producerExecutor;
    private final PolledJobProducerStarter<T> syncProducerFactory;

    public SimpleAsyncPolledJobProducerStarter(
            TaskExecutor startExecutor,
            TaskExecutor producerExecutor,
            PolledJobProducerStarter<T> syncProducerFactory) {

        this.startExecutor = Objects.requireNonNull(startExecutor, "startExecutor");
        this.producerExecutor = Objects.requireNonNull(producerExecutor, "producerExecutor");
        this.syncProducerFactory = Objects.requireNonNull(syncProducerFactory, "syncProducerFactory");
    }

    @Override
    public CompletionStage<AsyncPolledJobProducer<T>> startProducer(CancellationToken cancelToken) {
        TaskExecutor producerExecutorCapture = producerExecutor;
        PolledJobProducerStarter<T> syncProducerFactoryCapture = syncProducerFactory;

        return startExecutor.executeFunction(cancelToken, taskCancelToken -> {
            return syncProducerFactoryCapture.startProducer(taskCancelToken).toAsync(producerExecutorCapture);
        });
    }

}
