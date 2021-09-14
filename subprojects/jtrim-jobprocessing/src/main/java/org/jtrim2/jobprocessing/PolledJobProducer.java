package org.jtrim2.jobprocessing;

import org.jtrim2.executor.TaskExecutor;

@SuppressWarnings("try")
public interface PolledJobProducer<T> extends AutoCloseable {
    public T getNextJob() throws Exception;

    @Override
    public void close() throws Exception;

    public default AsyncPolledJobProducer<T> toAsync(TaskExecutor executor) {
        return new SimpleAsyncPolledJobProducer<>(executor, this);
    }
}
