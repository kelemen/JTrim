package org.jtrim2.jobprocessing;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.TaskExecutor;

final class SimpleAsyncJobConsumerStarter<T> implements AsyncJobConsumerStarter<T> {
    private final TaskExecutor startExecutor;
    private final TaskExecutor processorExecutor;
    private final StatefulJobConsumerStarter<T> syncConsumerFactory;

    public SimpleAsyncJobConsumerStarter(
            TaskExecutor startExecutor,
            TaskExecutor processorExecutor,
            StatefulJobConsumerStarter<T> syncConsumerFactory) {

        this.startExecutor = Objects.requireNonNull(startExecutor, "startExecutor");
        this.processorExecutor = Objects.requireNonNull(processorExecutor, "processorExecutor");
        this.syncConsumerFactory = Objects.requireNonNull(syncConsumerFactory, "syncConsumerFactory");
    }

    @Override
    public CompletionStage<AsyncJobConsumer<T>> startConsumer(CancellationToken cancelToken) {
        Objects.requireNonNull(cancelToken, "cancelToken");

        TaskExecutor processorExecutorCapture = processorExecutor;
        StatefulJobConsumerStarter<? super T> syncConsumerFactoryCapture = syncConsumerFactory;

        return startExecutor.executeFunction(cancelToken, taskCancelToken -> {
            StatefulJobConsumer<? super T> consumer = syncConsumerFactoryCapture.startConsumer(taskCancelToken);
            return new SimpleAsyncJobConsumer<>(cancelToken, processorExecutorCapture, consumer);
        });
    }
}
