package org.jtrim2.jobprocessing;

import java.util.concurrent.CompletionStage;
import org.jtrim2.cancel.CancellationToken;

public interface AsyncPolledJobProducer<T> {
    public CompletionStage<T> getNextJob(CancellationToken cancelToken);
    public CompletionStage<Void> closeAsync();
}
