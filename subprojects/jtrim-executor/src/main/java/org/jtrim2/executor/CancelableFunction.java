package org.jtrim2.executor;

import org.jtrim2.cancel.CancellationToken;

/**
 * Defines a task which can return an object and be canceled through a
 * {@link CancellationToken}. There is no constraint on what an implementation
 * of {@code CancelableFunction} may do, it must implement its sole
 * {@link #execute(CancellationToken) execute} method do whatever task it
 * wishes.
 * <P>
 * How a task must respond to cancellation requests is implementation dependent
 * but it may always terminate by throwing an
 * {@link org.jtrim2.cancel.OperationCanceledException}.
 *
 * <h3>Thread safety</h3>
 * The thread-safety property of {@code CancelableFunction} is completely
 * implementation dependent, so in general they does not need to be thread-safe.
 *
 * <h4>Synchronization transparency</h4>
 * {@code CancelableFunction} is not required to be
 * <I>synchronization transparent</I>.
 *
 * @param <V> the type of the result of the {@code CancelableFunction}
 *
 * @see CancelableTask
 * @see TaskExecutorService
 */
public interface CancelableFunction<V> {
    /**
     * Executes the the implementation dependent task and returns the result of
     * the computation. The task may check periodically the specified
     * {@code CancellationToken} to detect cancellation requests and return
     * immediately or throw an
     * {@link org.jtrim2.cancel.OperationCanceledException}.
     *
     * @param cancelToken the {@code CancellationToken} which can be checked
     *   periodically by this task to detect cancellation requests. This
     *   argument cannot be {@code null}.
     * @return the result of the computation. This method may return
     *   {@code null}.
     *
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if the task
     *   detects that it was canceled (usually by checking the provided
     *   {@code CancellationToken})
     * @throws Exception tasks may throw an exception they cannot handle. The
     *   executor should usually log these exceptions as severe problems.
     */
    public V execute(CancellationToken cancelToken) throws Exception;
}
