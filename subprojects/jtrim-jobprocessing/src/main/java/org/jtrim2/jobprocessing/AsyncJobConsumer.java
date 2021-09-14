package org.jtrim2.jobprocessing;

import java.util.concurrent.CompletionStage;

public interface AsyncJobConsumer<T> {
    public CompletionStage<Void> processJob(T job);

    public CompletionStage<Void> finishProcessing(ConsumerCompletionStatus finalStatus);
}
