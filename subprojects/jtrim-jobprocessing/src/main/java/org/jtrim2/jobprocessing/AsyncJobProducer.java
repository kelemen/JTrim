package org.jtrim2.jobprocessing;

import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;

public interface AsyncJobProducer<T> {
    public CompletionStage<Void> startTransfer(
            CancellationToken cancelToken,
            AsyncJobConsumerStarter<? super T> consumerFactory);
}
