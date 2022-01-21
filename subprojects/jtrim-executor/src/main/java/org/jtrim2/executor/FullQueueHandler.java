package org.jtrim2.executor;

import org.jtrim2.cancel.CancellationToken;

/**
 * Defines a handler to create a custom exception, when a thread pool finds its task queue full.
 * The handler may block and wait, but note that then it will block the thread trying to
 * submit a task.
 *
 * <h2>Thread safety</h2>
 * Instances of this interface must assume that they might be called from multiple threads
 * concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * Instances of this interface must either be <I>synchronization transparent</I> (recommended),
 * or consider all the scenarios from where a task can be submitted to the executor they are
 * associated with.
 *
 * @see ThreadPoolBuilder#setFullQueueHandler(FullQueueHandler) ThreadPoolBuilder.setFullQueueHandler
 */
public interface FullQueueHandler {
    /**
     * Returns a handler never producing a failure, and thus always instructing the executor
     * to block and wait.
     *
     * @return a handler never producing a failure, and thus always instructing the executor
     *   to block and wait. This method never returns {@code null}.
     */
    public static FullQueueHandler blockAlwaysHandler() {
        return cancelToken -> null;
    }

    /**
     * Creates an exception to be thrown to signal that the executor queue is full, or returns {@code null}
     * to instruct the executor to block and wait until it can execute the task. This method is
     * only called in the event when a full queue was actually observed for a task. Also, this method
     * will not be called twice for the same task submission.
     * <P>
     * The exception created (or thrown) will be propagated to the code trying to submit the task.
     *
     * @param cancelToken the {@code CancellationToken} signalling cancellation if the currently blocking
     *   task was canceled or if the executor was
     *   {@link TaskExecutorService#shutdownAndCancel() shutdown with cancellation}. This argument can
     *   never be {@code null}.
     * @return the exception to be thrown to signal that the executor queue is full, or {@code null}
     *   to instruct the executor to block and wait until it can execute the task
     *
     * @throws RuntimeException this method might throw an exception, which is effectively the same
     *   as returning the exception
     */
    public RuntimeException tryGetFullQueueException(CancellationToken cancelToken);
}
