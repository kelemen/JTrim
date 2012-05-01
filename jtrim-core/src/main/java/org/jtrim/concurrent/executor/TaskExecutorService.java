package org.jtrim.concurrent.executor;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Kelemen Attila
 */
public interface TaskExecutorService extends TaskExecutor {
    public <V> TaskFuture<V> execute(
            CancellationToken cancelToken,
            CancelableFunction<V> task);

    public <V> TaskFuture<V> execute(
            CancellationToken cancelToken,
            CancelableFunction<V> task,
            CleanupTask cleanupTask);

    public void shutdown();
    public void shutdownAndCancel();

    public boolean isShutdown();
    public boolean isTerminated();

    public boolean awaitTermination(CancellationToken cancelToken);
    public boolean awaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit);
}
