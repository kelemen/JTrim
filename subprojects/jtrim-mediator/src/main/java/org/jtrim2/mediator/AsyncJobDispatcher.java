package org.jtrim2.mediator;

import org.jtrim2.cancel.CancellationToken;

public interface AsyncJobDispatcher<T> extends AutoCloseable {
    public void cancelProcessing();

    public void dispatchJob(T job);

    public void shutdown();
    public void shutdownAndWait(CancellationToken cancelToken);

    @Override
    public void close();
}
