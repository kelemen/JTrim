package org.jtrim.cancel;

/**
 * Defines a generic task which can be canceled by interrupting the thread
 * executing the task. The task may respond to cancellation request either by
 * throwing an {@code InterruptedException} or an
 * {@link OperationCanceledException}.
 *
 * @param <ResultType> the type of the result of the task
 *
 * @see Cancellation#doAsCancelable(CancellationToken, InterruptibleTask)
 *
 * @author Kelemen Attila
 */
public interface InterruptibleTask<ResultType> {
    /**
     * Executes the task defined by this {@code InterruptibleTask}. The task
     * may choose to detect cancellation by means of the passed
     * {@code CancellationToken} or checking the interrupted status of the
     * executing thread (or both).
     *
     * @param cancelToken the {@code CancellationToken} which can be checked
     *   to detect cancellation requests. This task may also choose to ignore
     *   this argument and check the interrupted status of the executing thread.
     *   This argument cannot be {@code null}.
     * @return the return value of the task. This return value can be anything
     *   the task wishes
     *
     * @throws InterruptedException the task may throw this exception in
     *   response to a cancellation request
     * @throws OperationCanceledException the task may throw this exception in
     *   response to a cancellation request
     */
    public ResultType execute(CancellationToken cancelToken) throws InterruptedException;
}
