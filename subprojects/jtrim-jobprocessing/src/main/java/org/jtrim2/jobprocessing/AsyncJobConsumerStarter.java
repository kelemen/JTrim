package org.jtrim2.jobprocessing;

import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;

public interface AsyncJobConsumerStarter<T> {
    public CompletionStage<AsyncJobConsumer<T>> startConsumer(CancellationToken cancelToken);
}
