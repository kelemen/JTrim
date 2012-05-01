package org.jtrim.concurrent.executor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Kelemen Attila
 */
public interface TaskFuture<V> {
    public TaskState getTaskState();

    public V tryGetResult() throws ExecutionException;
    public V waitAndGet(CancellationToken cancelToken) throws ExecutionException;
    public V waitAndGet(CancellationToken cancelToken, long timeout, TimeUnit timeUnit) throws ExecutionException;
}
