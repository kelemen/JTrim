package org.jtrim2.jobprocessing;

public interface StatefulJobConsumer<T> {
    public void processJob(T job) throws Exception;

    public void finishProcessing(ConsumerCompletionStatus finalStatus) throws Exception;
}
