package org.jtrim.concurrent;

/**
 * Defines a cleanup action to be taken after a task has been terminated.
 * Usually the cleanup action is executed by a {@link TaskExecutor} or a
 * {@link TaskExecutorService}, the executor executes the cleanup
 * action always, regardless the way the task submitted to it has terminated
 * (canceled, thrown an exception or completed normally). Also cleanup task is
 * expected to be executed in the same context as the task associated with it.
 * <P>
 * The way the task has completed is passed to the cleanup task. That is, if it
 * has been canceled, thrown an exception or completed normally.
 *
 * <h3>Thread safety</h3>
 * The cleanup task is called once per task associated with it, therefore in
 * general it does not need to be thread-safe.
 *
 * <h4>Synchronization transparency</h4>
 * The cleanup task does not need to be <I>synchronization transparent</I> and
 * may only be called from the same context as the task associated with the
 * cleanup task (or at least the context where the associated task might have
 * been called were it not canceled).
 *
 * @see TaskExecutor
 * @see TaskExecutorService
 *
 * @author Kelemen Attila
 */
public interface CleanupTask {
    /**
     * This method is called when the associated task has been completed.
     * The task might have been completed by being canceled, throwing an
     * exception or returned normally. This method must be called from the same
     * context as the associated task (or if the associated task has been
     * canceled: in the same context where the associated task might have been
     * executed).
     *
     * @param canceled {@code true} if the task could not complete normally due
     *   to being canceled. This argument is only {@code true} if the task did
     *   not even started or thrown a {@link OperationCanceledException}, it is
     *   {@code false} if it completed normally despite any cancellation
     *   requests.
     * @param error the exception thrown by the task with which this cleanup
     *   task is associated with. This argument is {@code null} if the task did
     *   not throw any exception (either because it had not even be started or
     *   terminated normally). If the task thrown a
     *   {@link OperationCanceledException}, this argument is actually the
     *   thrown {@code OperationCanceledException} and the {@code canceled}
     *   argument is {@code true} as well.
     *
     * @throws Exception thrown when some unrecoverable failure occurs in the
     *   cleanup task. Throwing this exception means, that the cleanup action
     *   has failed and there is nothing to do about it. Those executing this
     *   cleanup task should log and hide this exception. Therefore, this
     *   method must not throw exceptions which must not be suppressed.
     */
    public void cleanup(boolean canceled, Throwable error) throws Exception;
}
