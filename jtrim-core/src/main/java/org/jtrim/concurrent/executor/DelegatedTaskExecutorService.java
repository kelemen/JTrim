package org.jtrim.concurrent.executor;

import java.util.concurrent.TimeUnit;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 *
 * @author Kelemen Attila
 */
public class DelegatedTaskExecutorService implements TaskExecutorService {
    protected final TaskExecutorService wrappedExecutor;

    public DelegatedTaskExecutorService(TaskExecutorService wrappedExecutor) {
        ExceptionHelper.checkNotNullArgument(wrappedExecutor, "wrappedExecutor");
        this.wrappedExecutor = wrappedExecutor;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskFuture<?> submit(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask) {
        return wrappedExecutor.submit(cancelToken, task, cleanupTask);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <V> TaskFuture<V> submit(
            CancellationToken cancelToken,
            CancelableFunction<V> task,
            CleanupTask cleanupTask) {
        return wrappedExecutor.submit(cancelToken, task, cleanupTask);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void shutdown() {
        wrappedExecutor.shutdown();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void shutdownAndCancel() {
        wrappedExecutor.shutdownAndCancel();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isShutdown() {
        return wrappedExecutor.isShutdown();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isTerminated() {
        return wrappedExecutor.isTerminated();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ListenerRef addTerminateListener(Runnable listener) {
        return wrappedExecutor.addTerminateListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void awaitTermination(CancellationToken cancelToken) {
        wrappedExecutor.awaitTermination(cancelToken);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean awaitTermination(
            CancellationToken cancelToken, long timeout, TimeUnit unit) {

        return wrappedExecutor.awaitTermination(cancelToken, timeout, unit);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void execute(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask) {
        wrappedExecutor.execute(cancelToken, task, cleanupTask);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        return wrappedExecutor.toString();
    }
}
