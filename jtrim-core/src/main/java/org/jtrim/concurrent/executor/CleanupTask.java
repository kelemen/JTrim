package org.jtrim.concurrent.executor;

/**
 * Defines a cleanup action to be taken after a task has been terminated.
 * Usually the cleanup action is executed by a {@link TaskExecutor} or a
 * {@link TaskExecutorService}, the executor executes the cleanup
 * action always, regardless the way the task submitted to it has terminated
 * (canceled, thrown an exception or completed normally).
 * <P>
 * The way the task has completed is passed to the cleanup task. That is, if it
 * has been canceled, thrown an exception or completed normally.
 *
 * <h3>Thread safety</h3>
 * The cleanup task is called once per task associated with it, therefore in
 * general it does not need to be thread-safe.
 *
 * <h4>Synchronization transparency</h4>
 * The cleanup task does not need to be <I>synchronization transparent</I> but
 * must expect to be called from various contexts, so it must not wait for
 * external events (such as an IO operation) and return as quickly as possible.
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
     * exception or returned normally.
     * <P>
     * This method might be called in various context, so implementations should
     * do as little work as possible and should never throw an exception. That
     * is, if this method throws an exception, it is considered a programming
     * error.
     * <P>
     * If an implementation finds itself in a situation where it needs to
     * execute a relatively expensive cleanup operation (such as restoring
     * files), the {@code cleanup} method should be implemented to only submit
     * a task to another thread (or submit it to a {@code TaskExecutor} which
     * can never be shutted down and will guarantee to execute every task,
     * scheduled to it).
     *
     * @param canceled {@code true} if the task could not complete normally due
     *   to being canceled. This argument is only {@code true} if the task did
     *   not even started or thrown a {@link OperationCanceledException}, it is
     *   {@code false} if it completed normally despite any cancellation
     *   requests.
     * @param error the exception thrown by the task with which this cleanup
     *   task is associated. This argument is {@code null} if the task did not
     *   throw any exception (either because it had not even be started or
     *   terminated normally). If the task thrown a
     *   {@link OperationCanceledException}, this argument is actually the
     *   thrown {@code OperationCanceledException} and the {@code canceled}
     *   argument is {@code true} as well.
     */
    public void cleanup(boolean canceled, Throwable error);
}
