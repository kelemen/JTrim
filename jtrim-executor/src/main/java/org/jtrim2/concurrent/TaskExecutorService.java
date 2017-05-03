package org.jtrim2.concurrent;

import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.ListenerRef;

/**
 * Defines a {@link TaskExecutor} with additional methods to better manage
 * submitted tasks. This interface is a replacement for the
 * {@link java.util.concurrent.ExecutorService} interface in java.
 * <P>
 * The {@link TaskExecutorService} provides new {@code submit} methods to submit
 * a task for execution and track its current phase of execution (as defined by
 * {@link TaskState}).
 * <P>
 * In general, {@code TaskExecutorService} implementations must be shut down
 * once no longer needed, so that implementations may terminate their internal
 * threads. When a {@code TaskExecutorService} has been shut down it will no
 * longer accept submitting tasks and as a consequence, it will immediately
 * execute the associated cleanup task (if there is any). It is implementation
 * dependent how cleanup task are executed after the executer has been shut
 * down, so you have to refer to the documentation of the implementation.
 * <P>
 * {@code TaskExecutorService} defines two ways for shutting down itself:
 * One is the {@link #shutdown() shutdown()} method and the other is the
 * {@link #shutdownAndCancel() shutdownAndCancel()} method. The difference
 * between them is that while {@code shutdown()} only prevents submitting
 * subsequent tasks, {@code shutdownAndCancel()} will actively cancel already
 * submitted tasks.
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
 * @see AbstractTaskExecutorService
 *
 * @author Kelemen Attila
 */
public interface TaskExecutorService extends TaskExecutor {
    /**
     * Executes the task at some time in the future and when the task terminates
     * due to any reason, it executes the specified cleanup task. When and on
     * what thread, the task is to be executed is completely implementation
     * dependent. Implementations may choose to execute tasks later on a
     * separate thread or synchronously in the calling thread at the discretion
     * of the implementation.
     * <P>
     * This method is different from
     * {@link TaskExecutor#execute(CancellationToken, CancelableTask, CleanupTask)}
     * only by returning a {@code TaskFuture} object to track its current phase
     * of execution of the submitted task.
     *
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if the submitted task is to be canceled. If this
     *   {@code CancellationToken} signals a cancellation request, this
     *   {@code TaskExecutor} may choose to not even attempt to execute the
     *   submitted task. In any case, the {@code cleanupTask} will be executed.
     *   This argument may not be {@code null}. When the task cannot be
     *   canceled, use the static
     *   {@link org.jtrim2.cancel.Cancellation#UNCANCELABLE_TOKEN} for this
     *   argument.
     * @param task the task to be executed by this {@code TaskExecutor}. This
     *   argument cannot be {@code null}.
     * @param cleanupTask the task to be executed after the submitted task has
     *   terminated or {@code null} if no task is needed to be executed. This
     *   cleanup task is executed always and only after the submitted task
     *   terminates or will never be executed (due to cancellation).
     * @return the {@code TaskFuture} which can be used to track the current
     *   phase of execution of the submitted task. The returned
     *   {@code TaskFuture} will return {@code null} as the result of the task.
     *   This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the {@code CancellationToken}
     *   or the task is {@code null}
     *
     * @see org.jtrim2.cancel.Cancellation#createCancellationSource()
     * @see org.jtrim2.cancel.Cancellation#UNCANCELABLE_TOKEN
     */
    @SuppressWarnings("overloads")
    public TaskFuture<?> submit(
            CancellationToken cancelToken,
            CancelableTask task,
            CleanupTask cleanupTask);

    /**
     * Executes the task with a return value at some time in the future and when
     * the task terminates due to any reason, it executes the specified cleanup
     * task. When and on what thread, the task is to be executed is completely
     * implementation dependent. Implementations may choose to execute tasks
     * later on a separate thread or synchronously in the calling thread at the
     * discretion of the implementation.
     *
     * @param <V> the type of the return value of the submitted task
     * @param cancelToken the {@code CancellationToken} which is to be checked
     *   if the submitted task is to be canceled. If this
     *   {@code CancellationToken} signals a cancellation request, this
     *   {@code TaskExecutor} may choose to not even attempt to execute the
     *   submitted task. In any case, the {@code cleanupTask} will be executed.
     *   This argument may not be {@code null}. When the task cannot be
     *   canceled, use the static
     *   {@link org.jtrim2.cancel.Cancellation#UNCANCELABLE_TOKEN} for this
     *   argument (even in this case, the {@code TaskExecutorService} may be
     *   able to cancel the task, if it was not submitted for execution).
     * @param task the task to be executed by this {@code TaskExecutor}. This
     *   argument cannot be {@code null}.
     * @param cleanupTask the task to be executed after the submitted task has
     *   terminated or {@code null} if no task is needed to be executed. This
     *   cleanup task is executed always and only after the submitted task
     *   terminates or will never be executed (due to cancellation).
     * @return the {@code TaskFuture} which can be used to track the current
     *   phase of execution of the submitted task. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the {@code CancellationToken}
     *   or the task is {@code null}
     *
     * @see org.jtrim2.cancel.Cancellation#createCancellationSource()
     * @see org.jtrim2.cancel.Cancellation#UNCANCELABLE_TOKEN
     */
    @SuppressWarnings("overloads")
    public <V> TaskFuture<V> submit(
            CancellationToken cancelToken,
            CancelableFunction<V> task,
            CleanupTask cleanupTask);

    /**
     * Shuts down this {@code TaskExecutorService}, so that it will not execute
     * tasks submitted to it after this method call returns.
     * <P>
     * Already submitted tasks will execute normally but tasks submitted after
     * this method returns will immediately, enter the
     * {@link TaskState#DONE_CANCELED} state and have their cleanup task be
     * executed. How cleanup tasks are executed after shut down is implementation
     * dependent but implementations are required to execute cleanup tasks, no
     * matter the circumstances (barring JVM termination).
     * <P>
     * Note that it is possible, that some tasks are submitted concurrently with
     * this call. Those tasks can be either canceled or executed normally,
     * depending on the circumstances.
     * <P>
     * If currently executing tasks should be canceled as well, use the
     * {@link #shutdownAndCancel()} method to shutdown this
     * {@code TaskExecutorService}.
     * <P>
     * This method call is idempotent. That is, calling it multiple times must
     * have no further effect.
     *
     * @see #shutdownAndCancel()
     */
    public void shutdown();

    /**
     * Shuts down this {@code TaskExecutorService} and cancels already
     * submitted tasks, so that it will not execute tasks submitted to it after
     * this method call returns.
     * <P>
     * Already submitted tasks will be canceled and the tasks may detect this
     * cancellation request by inspecting their {@code CancellationToken} but
     * tasks submitted after this method returns will immediately, enter the
     * {@link TaskState#DONE_CANCELED} state and have their cleanup task be
     * executed. How cleanup tasks are executed after shut down is implementation
     * dependent but implementations are required to execute cleanup tasks, no
     * matter the circumstances (barring JVM termination).
     * <P>
     * Note that it is possible, that some tasks are submitted concurrently with
     * this call. Those tasks may be treated as if they were submitted before
     * this method call or as if they were submitted after.
     * <P>
     * If currently executing tasks should be left executing, use the
     * {@link #shutdown()} method instead to shutdown this
     * {@code TaskExecutorService}.
     * <P>
     * This method call is idempotent. That is, calling it multiple times must
     * have no further effect. Note however, that calling this method after the
     * {@code shutdown()} method is meaningful because this method will cancel
     * ongoing tasks.
     *
     * @see #shutdown()
     */
    public void shutdownAndCancel();

    /**
     * Checks whether this {@code TaskExecutorService} accepts newly submitted
     * tasks or not. This method returns {@code true}, if and only, if either
     * the {@code #shutdown() shutdown()} or the
     * {@link #shutdownAndCancel() shutdownAndCancel()} method has been called.
     * Therefore if, this method returns {@code true}, subsequent {@code submit}
     * and {@code execute} method invocations will not execute submitted tasks
     * and will only execute their cleanup tasks.
     *
     * @return {@code true} if this {@code TaskExecutorService} accepts newly
     *   submitted tasks, {@code false} if it has been shut down
     *
     * @see #isTerminated()
     */
    public boolean isShutdown();

    /**
     * Checks whether this {@code TaskExecutorService} may execute tasks
     * submitted to it tasks or not. If this method returns {@code true}, no
     * more tasks will be executed by this {@code TaskExecutorService} and no
     * tasks are currently executing. That is, if this method returns
     * {@code true} subsequent {@code submit} or {@code execute} methods will
     * not execute the submitted tasks but cancel them immediately and execute
     * their cleanup tasks.
     * <P>
     * Also if this method returns {@code true}, subsequent
     * {@code awaitTermination} method calls will return immediately without
     * throwing an exception (will not even check for cancellation).
     *
     * @return {@code true} if this {@code TaskExecutorService} accepts newly
     *   submitted tasks, {@code false} if it has been shut down
     *
     * @see #isTerminated()
     */
    public boolean isTerminated();

    /**
     * Adds a listener which is to be notified after this
     * {@code TaskExecutorService} terminates. If the listener has been already
     * terminated, the listener is notified immediately in this
     * {@code addTerminateListener} method call. Also, the listener may only
     * be called at most once (per registration).
     * <P>
     * Whenever these registered listeners are notified, the
     * {@link #isTerminated() isTerminated()} method already returns
     * {@code true} and calling the {@code awaitTermination} or the
     * {@code tryAwaitTermination} method will have no effect (as they return
     * immediately).
     * <P>
     * On what thread, the registered listeners might be called is
     * implementation dependent: They can be called from the thread, the last
     * task executed, the {@link #shutdown() shutdown} or the
     * {@link #shutdownAndCancel() shutdownAndCancel} methods or in any other
     * thread, as this {@code TaskExecutorService} desires. Therefore, the
     * listeners must be written conservatively.
     * <P>
     * The listener can be removed if no longer required to be notified by
     * calling the {@link ListenerRef#unregister() unregister} method of the
     * returned reference.
     *
     * @param listener the {@code Runnable} whose {@code run} method is to be
     *   called after this {@code TaskExecutorService} terminates. This argument
     *   cannot be {@code null}.
     * @return the reference which can be used to removed the currently added
     *   listener, so that it will not be notified anymore. This method never
     *   returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     *
     * @see #awaitTermination(CancellationToken)
     * @see #tryAwaitTermination(CancellationToken, long, TimeUnit)
     */
    public ListenerRef addTerminateListener(Runnable listener);

    /**
     * Waits until this {@code TaskExecutorService} will not execute any more
     * tasks. After this method returns (without throwing an exception),
     * subsequent {@link #isTerminated()} method calls will return {@code true}.
     * <P>
     * After this method returns without throwing an exception, it is true that:
     * No more tasks will be executed by this {@code TaskExecutorService} and no
     * tasks are currently executing. That is, subsequent {@code submit} or
     * {@code execute} methods will not execute the submitted tasks but cancel
     * them immediately and execute their cleanup tasks.
     *
     * @param cancelToken the {@code CancellationToken} which can be used to
     *   stop waiting for the termination of this {@code TaskExecutorService}.
     *   That is, if this method detects, that cancellation was requested, it
     *   will throw an {@link org.jtrim2.cancel.OperationCanceledException}.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code CancellationToken} is {@code null}
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if
     *   cancellation request was detected by this method before this
     *   {@code TaskExecutorService} terminated. This exception is not thrown if
     *   this {@code TaskExecutorService} was terminated prior to this method
     *   call.
     *
     * @see #addTerminateListener(Runnable)
     */
    public void awaitTermination(CancellationToken cancelToken);

    /**
     * Waits until this {@code TaskExecutorService} will not execute any more
     * tasks or the given timeout elapses. After this method returns
     * {@code true}, subsequent {@link #isTerminated()} method calls will also
     * return {@code true}.
     * <P>
     * After this method returns {@code true} (and does not throw an exception),
     * it is true that: No more tasks will be executed by this
     * {@code TaskExecutorService} and no tasks are currently executing. That
     * is, subsequent {@code submit} or {@code execute} methods will not execute
     * the submitted tasks but cancel them immediately and execute their cleanup
     * tasks.
     *
     * @param cancelToken the {@code CancellationToken} which can be used to
     *   stop waiting for the termination of this {@code TaskExecutorService}.
     *   That is, if this method detects, that cancellation was requested, it
     *   will throw an {@link org.jtrim2.cancel.OperationCanceledException}.
     *   This argument cannot be {@code null}.
     * @param timeout the maximum time to wait for this
     *   {@code TaskExecutorService} to terminate in the given time unit. This
     *   argument must be greater than or equal to zero.
     * @param unit the time unit of the {@code timeout} argument. This argument
     *   cannot be {@code null}.
     * @return {@code true} if this {@code TaskExecutorService} has terminated
     *   before the timeout elapsed, {@code false} if the timeout elapsed first.
     *   In case this {@code TaskExecutorService} terminated prior to this call
     *   this method always returns {@code true}.
     *
     * @throws IllegalArgumentException thrown if the specified timeout value
     *   is negative
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws org.jtrim2.cancel.OperationCanceledException thrown if
     *   cancellation request was detected by this method before this
     *   {@code TaskExecutorService} terminated
     *   This exception is not thrown if this {@code TaskExecutorService} was
     *   terminated prior to this method call.
     *
     * @see #addTerminateListener(Runnable)
     */
    public boolean tryAwaitTermination(CancellationToken cancelToken, long timeout, TimeUnit unit);
}
