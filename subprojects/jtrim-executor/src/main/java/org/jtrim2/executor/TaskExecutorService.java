package org.jtrim2.executor;

import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.ListenerRef;

/**
 * Defines a {@link TaskExecutor} with additional methods to better manage
 * submitted tasks. This interface is a replacement for the
 * {@link java.util.concurrent.ExecutorService} interface in java.
 * <P>
 * In general, {@code TaskExecutorService} implementations must be shut down
 * once no longer needed, so that implementations may terminate their internal
 * threads. When a {@code TaskExecutorService} has been shut down it will no
 * longer accept submitting tasks and as a consequence, it will complete them
 * exceptionally with an {@link org.jtrim2.cancel.OperationCanceledException}.
 * <P>
 * {@code TaskExecutorService} defines two ways for shutting down itself:
 * One is the {@link #shutdown() shutdown()} method and the other is the
 * {@link #shutdownAndCancel() shutdownAndCancel()} method. The difference
 * between them is that while {@code shutdown()} only prevents submitting
 * subsequent tasks, {@code shutdownAndCancel()} will actively cancel already
 * submitted tasks.
 *
 * <h2>Thread safety</h2>
 * Implementations of this interface are required to be safely accessible from
 * multiple threads concurrently.
 *
 * <h3>Synchronization transparency</h3>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I> because they may execute tasks, handlers added
 * to {@code CompletionStage}, etc.
 *
 * @see AbstractTaskExecutorService
 */
public interface TaskExecutorService extends TaskExecutor {
    /**
     * Shuts down this {@code TaskExecutorService}, so that it will not execute
     * tasks submitted to it after this method call returns.
     * <P>
     * Already submitted tasks will execute normally but tasks submitted after
     * this method returns will immediately be completed exceptionally
     * with an {@link org.jtrim2.cancel.OperationCanceledException}.
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
     * tasks submitted after this method returns will immediately be completed
     * exceptionally with an {@link org.jtrim2.cancel.OperationCanceledException}.
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
     * and will only complete them exceptionally with an
     * {@link org.jtrim2.cancel.OperationCanceledException}.
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
     * not execute the submitted tasks but complete them exceptionally with an
     * {@link org.jtrim2.cancel.OperationCanceledException}.
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
     * {@code execute} methods will not execute the submitted tasks but complete
     * them exceptionally with an {@link org.jtrim2.cancel.OperationCanceledException}.
     * <P>
     * The default implementation simply calls {@code tryAwaitTermination} until it
     * returns {@code true}.
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
    public default void awaitTermination(CancellationToken cancelToken) {
        while (!tryAwaitTermination(cancelToken, Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
            // Repeat until it has been terminated, or throws an exception.
        }
    }

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
     * the submitted tasks but complete them exceptionally with an
     * {@link org.jtrim2.cancel.OperationCanceledException}.
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
