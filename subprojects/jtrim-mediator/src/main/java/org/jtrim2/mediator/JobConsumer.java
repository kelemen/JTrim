package org.jtrim2.mediator;

import org.jtrim2.cancel.CancellationToken;

public interface JobConsumer<T> {
    public void processJob(CancellationToken cancelToken, T job) throws Exception;

    public void finishProcessing(ConsumerCompletionStatus finalStatus) throws Exception;
}
