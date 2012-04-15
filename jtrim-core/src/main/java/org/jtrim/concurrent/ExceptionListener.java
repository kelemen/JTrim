package org.jtrim.concurrent;

/**
 * The listener interface to be notified when a task throws an exception.
 *
 * <h3>Thread safety</h3>
 * This interface is highly recommended to be safe to use by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * This interface is not required to be <I>synchronization transparent</I>.
 *
 * @param <TaskType> the type of the tasks of which this listener will be
 *   notified of when they throw an exception. This is usually a
 *   {@code Runnable} or {@code Callable} or class implementing one of these
 *   interfaces.
 *
 * @see ExceptionAwareCallable
 * @see ExceptionAwareExecutorService
 * @see ExceptionAwareRunnable
 *
 * @author Kelemen Attila
 */
public interface ExceptionListener<TaskType> {
    /**
     * Invoked when a task throws an exception. The exception should be
     * propagated to the caller after this method call.
     *
     * @param error the exception thrown by the task. This argument cannot be
     *   {@code null}.
     * @param task the task throwing the exception. This argument cannot be
     *   {@code null}.
     */
    public void onException(Throwable error, TaskType task);
}
