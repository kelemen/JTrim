package org.jtrim2.jobprocessing;

import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;

public interface AsyncPolledJobProducerStarter<T> {
    public CompletionStage<AsyncPolledJobProducer<T>> startProducer(CancellationToken cancelToken);
}
