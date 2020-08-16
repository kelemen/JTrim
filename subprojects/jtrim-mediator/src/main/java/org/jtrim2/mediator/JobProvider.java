package org.jtrim2.mediator;

import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;

public interface JobProvider<T> {
    CompletionStage<Void> startProvider(CancellationToken cancelToken, JobConsumerFactory<? super T> consumerFactory);
}
