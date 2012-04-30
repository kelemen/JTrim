package org.jtrim.concurrent.executor;

/**
 *
 * @author Kelemen Attila
 */
public interface TaskExecutor {
    public void execute(CancellationToken cancelToken, CancelableTask task);
    public void execute(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask);
}
