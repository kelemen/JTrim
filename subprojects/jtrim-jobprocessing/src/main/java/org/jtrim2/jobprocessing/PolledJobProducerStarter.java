package org.jtrim2.jobprocessing;

import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.TaskExecutor;

public interface PolledJobProducerStarter<T> {
    public PolledJobProducer<T> startProducer(CancellationToken cancelToken) throws Exception;

    public default AsyncPolledJobProducerStarter<T> toAsync(TaskExecutor executor) {
        return toAsync(executor, executor);
    }

    public default AsyncPolledJobProducerStarter<T> toAsync(
            TaskExecutor startExecutor,
            TaskExecutor processorExecutor) {

        return new SimpleAsyncPolledJobProducerStarter<>(startExecutor, processorExecutor, this);
    }
}
