package org.jtrim.concurrent.executor;

import org.jtrim.cancel.CancellationToken;

/**
 * Defines a task which can be canceled through a {@link CancellationToken}.
 * There is no constraint on what an implementation of {@code CancelableTask}
 * may do, it must implement its sole {@link #execute(CancellationToken) execute}
 * method do whatever task it wishes.
 * <P>
 * How a task must respond to cancellation requests is implementation dependent
 * but it may always terminate by throwing a {@link OperationCanceledException}.
 *
 * <h3>Thread safety</h3>
 * The thread-safety property of {@code CancelableTask} is completely
 * implementation dependent, so in general they does not need to be thread-safe.
 *
 * <h4>Synchronization transparency</h4>
 * {@code CancelableTask} is not required to be
 * <I>synchronization transparent</I>.
 *
 * @see CancelableFunction
 * @see TaskExecutor
 *
 * @author Kelemen Attila
 */
public interface CancelableTask {
    /**
     * Executes the the implementation dependent task. The task may check
     * periodically the specified {@code CancellationToken} to detect
     * cancellation requests and return immediately or throw a
     * {@link OperationCanceledException}.
     *
     * @param cancelToken the {@code CancellationToken} which can be checked
     *   periodically by this task to detect cancellation requests. This
     *   argument cannot be {@code null}.
     *
     * @throws OperationCanceledException thrown if the task detects that it was
     *   canceled (usually by checking the provided {@code CancellationToken})
     */
    public void execute(CancellationToken cancelToken);
}
