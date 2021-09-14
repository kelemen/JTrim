package org.jtrim2.jobprocessing;

import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.executor.TaskExecutor;

public interface StatefulJobConsumerStarter<T> {
    public StatefulJobConsumer<T> startConsumer(CancellationToken cancelToken) throws Exception;

    public default AsyncJobConsumerStarter<T> toAsync(TaskExecutor executor) {
        return toAsync(executor, executor);
    }

    public default AsyncJobConsumerStarter<T> toAsync(TaskExecutor startExecutor, TaskExecutor processorExecutor) {
        return new SimpleAsyncJobConsumerStarter<>(startExecutor, processorExecutor, this);
    }
}
