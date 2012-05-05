package org.jtrim.concurrent.executor;

/**
 * Executes tasks at some time in the future. This interface defines a more
 * robust way to execute tasks than {@code java.util.concurrent.Executor}.
 * That is, this interface defines a simpler way for canceling tasks and allows
 * to execute a cleanup task for every task submitted. The cleanup task is
 * executed regardless of cancellation or any other means of termination. The
 * cleanup task is executed after the associated submitted task has terminated
 * or if the task will never be executed (due to cancellation). The execution
 * of cleanup task is the main feature of {@code TaskExecutor} as there is no
 * reliable alternative for it in the {@code Executor} implementations in Java.
 * <P>
 * For more control over the life of a {@code TaskExecutor}, see the extending
 * {@link TaskExecutorService} interface.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safely accessible from
 * multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I> because they may execute tasks, cleanup
 * tasks, etc.
 *
 * @see CancelableTask
 * @see CleanupTask
 * @see TaskExecutorService
 *
 * @author Kelemen Attila
 */
public interface TaskExecutor {
    /**
     * Executes the task at some time in the future and when the task terminates
     * due to any reason, it executes the specified cleanup task. When and on
     * what thread, the task is to be executed is completely implementation
     * dependent. Implementations may choose to execute tasks later on a
     * separate thread or synchronously in the calling thread at the discretion
     * of the implementation.
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if the submitted task is to be canceled. If this
     *   {@code CancellationToken} signals a cancellation request, this
     *   {@code TaskExecutor} may choose to not even attempt to execute the
     *   submitted task. In any case, the {@code cleanupTask} will be executed.
     *   This argument may not be {@code null}. When the task cannot be
     *   canceled, use the static {@link CancellationSource#UNCANCELABLE_TOKEN}
     *   for this argument (even in this case, the {@code TaskExecutor} may be
     *   able to cancel the task, if it was not submitted for execution).
     * @param task the task to be executed by this {@code TaskExecutor}. This
     *   argument cannot be {@code null}.
     * @param cleanupTask the task to be executed after the submitted task has
     *   terminated or {@code null} if no task is needed to be executed. This
     *   cleanup task is executed always and only after the submitted task
     *   terminates or will never be executed (due to cancellation). This
     *   cleanup task is to be executed in the same context as the submitted
     *   task (or if the associated task has been canceled: in the same context
     *   where the associated task might have been executed).
     *
     * @throws NullPointerException thrown if the {@code CancellationToken}
     *   or the task is {@code null}
     *
     * @see CancellationSource
     * @see CancellationSource#UNCANCELABLE_TOKEN
     */
    public void execute(CancellationToken cancelToken, CancelableTask task, CleanupTask cleanupTask);
}
