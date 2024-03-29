package org.jtrim2.executor;

import org.jtrim2.cancel.CancellationToken;

/**
 * Defines a task which can be canceled through a {@link CancellationToken}.
 * There is no constraint on what an implementation of {@code CancelableTask}
 * may do, it must implement its sole {@link #execute(CancellationToken) execute}
 * method do whatever task it wishes.
 * <P>
 * How a task must respond to cancellation requests is implementation dependent
 * but it may always terminate by throwing an
 * {@link org.jtrim2.cancel.OperationCanceledException}.
 *
 * <h2>Thread safety</h2>
 * The thread-safety property of {@code CancelableTask} is completely
 * implementation dependent, so in general they does not need to be thread-safe.
 *
 * <h3>Synchronization transparency</h3>
 * {@code CancelableTask} is not required to be
 * <I>synchronization transparent</I>.
 *
 * @see CancelableFunction
 * @see TaskExecutor
 */
public interface CancelableTask {
    /**
     * Executes the the implementation dependent task. The task may check
     * periodically the specified {@code CancellationToken} to detect
     * cancellation requests and throw an
     * {@link org.jtrim2.cancel.OperationCanceledException}. In case cancellation
     * has been requested but this method does not throw an
     * {@code OperationCanceledException}, it is assumed that this method
     * ignored the cancellation request and completed normally.
     *
     * @param cancelToken the {@code CancellationToken} which can be checked
     *   periodically by this task to detect cancellation requests. This
     *   argument cannot be {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if the task
     *   detects that it was canceled (usually by checking the provided
     *   {@code CancellationToken})
     * @throws Exception tasks may throw an exception they cannot handle. The
     *   executor should usually log these exceptions as severe problems.
     */
    public void execute(CancellationToken cancelToken) throws Exception;
}
