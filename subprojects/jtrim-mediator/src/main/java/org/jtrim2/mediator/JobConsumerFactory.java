package org.jtrim2.mediator;

import org.jtrim2.cancel.CancellationToken;

public interface JobConsumerFactory<T> {
    public JobConsumer<T> startConsumer(CancellationToken cancelToken) throws Exception;
}
