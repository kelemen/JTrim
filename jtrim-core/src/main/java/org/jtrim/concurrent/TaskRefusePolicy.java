package org.jtrim.concurrent;

/**
 * An interface defining what to do when a task was refused to be executed by an
 * {@code Executor} or {@code ExecutorService}. The executor must invoke
 * {@link #refuseTask(Runnable) refuseTask} in the {@code submit} or
 * {@code execute} method immediately.
 * <P>
 * Examples of policies are to execute the task synchronously in the calling
 * thread or discard it silently but many more implementations are possible.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safe to use by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @see AbortTaskRefusePolicy
 * @see SilentTaskRefusePolicy
 * @see TaskListExecutorImpl
 * @author Kelemen Attila
 */
public interface TaskRefusePolicy {
    /**
     * Invoked when an {@code Executor} or {@code ExecutorService} refused
     * to execute a certain task. This method must be called synchronously
     * in the {@code submit} or {@code execute} method of the executor.
     *
     * @param task the task which was refused to be executed. This argument
     *   cannot be {@code null}.
     */
    public void refuseTask(Runnable task);
}
