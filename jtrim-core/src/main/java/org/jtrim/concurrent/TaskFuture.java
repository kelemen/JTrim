package org.jtrim.concurrent;

import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.CancellationToken;

/**
 * Represents the result and the current phase of execution of an asynchronous
 * computation (usually submitted to a {@code TaskExecutorService}. The main
 * feature of {@code TaskFuture} is that, other tasks may wait for the
 * completion and the result of the task the {@code TaskFuture} represents.
 * These waits can be done by the {@code waitAndGet} methods.
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface are required to be safely accessible from
 * multiple threads concurrently except for the {@link #getTaskState()} method.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this interface are not required to be
 * <I>synchronization transparent</I>.
 *
 * @param <V> the type of the result of the asynchronous computation
 *
 * @see TaskExecutorService
 *
 * @author Kelemen Attila
 */
public interface TaskFuture<V> {
    /**
     * Returns the current phase of execution of the asynchronous computation.
     * That is, if it has been started, currently running or terminated.
     * There are three possible ways for an asynchronous computation to
     * terminate: be canceled, throw an exception or complete normally.
     * <P>
     * If the return value of this method defines that the computation has
     * already terminated (i.e.: {@code getTaskState.isDone() == true}),
     * subsequent {@code waitAndGet} methods will not block but return
     * immediately (either by returning the tasks result of throwing an
     * exception if it did not complete normally).
     * <P>
     * Note that this method must be <I>synchronization transparent</I>.
     *
     * @return the current phase of execution of the asynchronous computation.
     *   This method never returns {@code null}.
     */
    public TaskState getTaskState();

    /**
     * Retrieves the result of the asynchronous computation if it already
     * completed or returns {@code null} if it did not. This method always
     * returns immediately and will not try to wait for the asynchronous
     * computation to complete.
     * <P>
     * In case {@code getTaskState.isDone() == true} prior to this call, this
     * method will never fail to retrieve the result of the computation: It will
     * either return the result of the computation or throw an exception if the
     * computation did not complete normally.
     *
     * @return the result of the asynchronous computation if it already
     *   completed or {@code null} if it did not yet complete. Note that a
     *   return value of {@code null} may also means, that the asynchronous
     *   computation returned {@code null}.
     *
     * @throws OperationCanceledException thrown if the asynchronous computation
     *   has been canceled (and also terminated)
     * @throws TaskExecutionException thrown if the asynchronous computation
     *   has already terminated by throwing an exception. The actual exception
     *   is the cause of this {@code TaskExecutionException} which is never
     *   {@code null}.
     */
    public V tryGetResult();

    /**
     * Waits until the asynchronous computation terminates and returns the
     * result of the computation or throws a {@code OperationCanceledException},
     * if this method is requested to be canceled.
     * <P>
     * In case {@code getTaskState.isDone() == true} prior to this call, this
     * method will never fail to retrieve the result of the computation and will
     * not block: It will either return the result of the computation or throw
     * an exception if the computation did not complete normally.
     *
     * @param cancelToken the {@code CancellationToken} which can be used to
     *   signal that this method needs to stop waiting for the asynchronous task
     *   a terminate by throwing a {@code OperationCanceledException}
     * @return the result of the asynchronous computation which can possibly be
     *   {@code null}
     *
     * @throws OperationCanceledException thrown if the asynchronous computation
     *   has been canceled (and also terminated) or this method was requested
     *   to stop waiting by the specified {@code CancellationToken}. Which of
     *   these events occurred can be detected by the result of the
     *   {@link #getTaskState()} method.
     * @throws TaskExecutionException thrown if the asynchronous computation
     *   has already terminated by throwing an exception. The actual exception
     *   is the cause of this {@code TaskExecutionException} which is never
     *   {@code null}.
     * @throws NullPointerException thrown if the specified
     *   {@code CancellationToken} is {@code null}
     */
    public V waitAndGet(CancellationToken cancelToken);

    /**
     * Waits until the asynchronous computation terminates or the specified
     * timeout expires and returns the result of the computation or throws a
     * {@code OperationCanceledException}, if this method is requested to be
     * canceled.
     * <P>
     * In case {@code getTaskState.isDone() == true} prior to this call, this
     * method will never fail to retrieve the result of the computation and will
     * not block: It will either return the result of the computation or throw
     * an exception if the computation did not complete normally.
     *
     * @param cancelToken the {@code CancellationToken} which can be used to
     *   signal that this method needs to stop waiting for the asynchronous task
     *   a terminate by throwing a {@code OperationCanceledException}
     * @param timeout the maximum time to wait for the result of the computation
     *   in the given timeout. This argument must be greater than or equal to
     *   zero.
     * @param timeUnit the time unit of the {@code timeout} argument. This
     *   argument cannot be {@code null}.
     * @return the result of the asynchronous computation which can possibly be
     *   {@code null}
     *
     * @throws OperationCanceledException thrown if:
     *   <ul>
     *    <li>
     *     the asynchronous computation has been canceled (and also terminated)
     *     or
     *    </li>
     *    <li>
     *     this method was requested to stop waiting by the specified
     *     {@code CancellationToken} or
     *    </li>
     *    <li>
     *     the specified timeout elapsed without the asynchronous task
     *     terminating.
     *    </li>
     *   </ul>
     *   Which of these events occurred can be detected by the result of the
     *   {@link #getTaskState()} method and the
     *   {@code cancelToken.isCanceled()} method.
     * @throws TaskExecutionException thrown if the asynchronous computation
     *   has already terminated by throwing an exception. The actual exception
     *   is the cause of this {@code TaskExecutionException} which is never
     *   {@code null}.
     * @throws IllegalArgumentException throw if the specified timeout value
     *   is less than zero
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public V waitAndGet(CancellationToken cancelToken, long timeout, TimeUnit timeUnit);
}
