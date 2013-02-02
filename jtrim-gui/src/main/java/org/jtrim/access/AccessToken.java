package org.jtrim.access;

import java.util.concurrent.TimeUnit;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.ContextAwareTaskExecutor;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.event.ListenerRef;

/**
 * Allows execution of tasks while having read or write (exclusive) access
 * to some resources. An access is considered granted while
 * {@link #isReleased() isReleased()} returns {@code false} afterwards
 * the right to access resources are considered lost. Notice that this implies
 * that the access right cannot get lost while a task is actively executing
 * on a {@link TaskExecutor} of the {@code AccessToken}.
 * <P>
 * Instances of this class are usually tied to a particular
 * {@link AccessManager} which can grant access to resources. The
 * {@code AccessToken} will hold onto these resources until it has been
 * released (which can also be caused by the owner {@code AccessManager}).
 * <P>
 * {@code TaskExecutor} instances which can be used to actually execute
 * tasks can be created by the
 * {@link #createExecutor(TaskExecutor) createExecutor} method. This executors
 * are referenced in the documentation as
 * "executors of the {@code AccessToken}".
 *
 * <h3>Thread safety</h3>
 * Instances of this interface are required to be completely thread-safe
 * without any further synchronization.
 *
 * <h4>Synchronization transparency</h4>
 * Unless documented otherwise the methods of this class are not required
 * to be <I>synchronization transparent</I>.
 *
 * @param <IDType> the type of the access ID (see {@link #getAccessID()})
 *
 * @see AbstractAccessToken
 * @see AccessManager
 * @see AccessTokens
 * @see GenericAccessToken
 * @author Kelemen Attila
 */
public interface AccessToken<IDType> {
    /**
     * Returns an ID of the request used to create this {@code AccessToken}
     * instance.
     * <P>
     * Note that this ID does not need to be unique but cannot be {@code null}.
     * The ID is intended to be used to provide information about this access,
     * so a client can determine if it should cancel this token to allow
     * a new token to be created which conflicts with this token.
     *
     * <h5>Thread safety</h5>
     * Implementations of this method required to be completely thread-safe
     * without any further synchronization.
     *
     * <h6>Synchronization transparency</h6>
     * Implementations of this method required to be <I>synchronization
     * transparent</I>.
     *
     * @return the ID of the request used to create this {@code AccessToken}
     *   instance. This method always returns the same object for a given
     *   {@code AccessToken} instance and never returns {@code null}.
     *
     * @see AccessRequest
     */
    public IDType getAccessID();

    /**
     * Creates a {@code TaskExecutor} backed by the specified
     * {@code TaskExecutor} which will not execute tasks after this
     * {@code AccessToken} was released.
     * <P>
     * The returned executor is useful to execute tasks which may only be
     * executed if this {@code AccessToken} is not released. It is guaranteed,
     * that this {@code AccessToken} will not be released while a task submitted
     * to the returned {@code TaskExecutor} is currently executing.
     * <P>
     * Note that cleanup tasks are always executed, even if this
     * {@code AccessToken} has already been released.
     *
     * @param executor the {@code TaskExecutor} which will actually execute
     *   tasks submitted to the returned {@code TaskExecutor}. This
     *   argument cannot be {@code null}.
     * @return a {@code TaskExecutor} backed by the specified
     *   {@code TaskExecutor} which will not execute tasks after this
     *   {@code AccessToken} was released.
     *
     * @throws NullPointerException thrown if the specified executor is
     *   {@code null}
     */
    public ContextAwareTaskExecutor createExecutor(TaskExecutor executor);

    /**
     * Returns {@code true} if the calling code is executing in the context of
     * this access token. That is, it is executed by a task submitted to an
     * executor created by the {@link #createExecutor(TaskExecutor) createExecutor}
     * method of this access token.
     * <P>
     * This method can be used to check that a method call is executing in the
     * context it was designed for.
     *
     * @return {@code true} if the calling code is executing in the context of
     *   this access token, {@code false} otherwise
     */
    public boolean isExecutingInThis();

    /**
     * Registers a new listener which will be notified if this
     * {@code AccessToken} was released. When the listener is notified,
     * {@link #isReleased()} already returns {@code true}.
     * <P>
     * The listener is notified even if it has been released prior to adding
     * the listener and the listener is never notified more than once.
     * <P>
     * Registering the same listener twice is allowed and will cause that
     * listener to be notified as many times as it have been registered to be
     * notified of the release of this access token.
     * <P>
     * <B>Note that the listener must be very agnostic about on which thread
     * it can be called.</B> It is perfectly allowed for an implementation
     * to notify the listener in this method invocation.
     *
     * <h5>Unregistering the listener</h5>
     * Unlike the general {@code removeXXX} idiom in Swing listeners, this
     * listener can be removed using the returned reference.
     * <P>
     * The unregistering of the listener is not necessary and in general
     * implementations of {@code AccessToken} are required to no longer
     * reference the registered listeners.
     *
     * @param listener the listener to be registered to be notified when this
     *   {@code AccessToken} has been released. This argument cannot be
     *   {@code null}.
     * @return the reference to the newly registered listener which can be
     *   used to remove this newly registered listener, so it will no longer
     *   be notified of the release event. Note that this method may return
     *   an unregistered listener if this {@code AccessToken} has already been
     *   released. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the passed listener is
     *   {@code null}
     */
    public ListenerRef addReleaseListener(Runnable listener);

    /**
     * Checks whether this {@code AccessToken} has lost access to the associated
     * resources or not. That is, if the executors of this {@code AccessToken}
     * will execute submitted tasks or not.
     * <P>
     * In case this method returns {@code true}, it is guaranteed that, no more
     * tasks will be executed by executors of this {@code AccessToken}, and no
     * tasks are currently being executed.
     *
     * @return {@code true} if this {@code AccessToken} has lost access to the
     *   associated resources, {@code false} otherwise
     */
    public boolean isReleased();

    /**
     * Prevents the executors of this {@code AccessToken} to execute tasks
     * submitted after this call. When all the previously submitted tasks
     * finish, notifies the
     * {@link #addReleaseListener(Runnable) release listeners}.
     * <P>
     * This method will prevent execution of tasks even on {@code TaskExecutor}
     * instances created after this method call.
     *
     * @see #addReleaseListener(Runnable)
     */
    public void release();

    /**
     * Prevents the executors of this {@code AccessToken} to execute tasks
     * submitted after this call and cancels the already submitted tasks. When
     * there are no more tasks currently executing (and there will be no more),
     * notifies the {@link #addReleaseListener(Runnable) release listeners}.
     * <P>
     * This method will prevent execution of tasks even on {@code TaskExecutor}
     * instances created after this method call.
     *
     * @see #addReleaseListener(Runnable)
     */
    public void releaseAndCancel();

    /**
     * Waits until the executors of this {@code AccessToken} will not execute
     * any more tasks. After this method returns (without throwing an
     * exception), subsequent {@link #isReleased()} method calls of the
     * executors of this {@code AccessToken} will return {@code true}.
     * <P>
     * After this method returns without throwing an exception, the rights
     * associated with this {@code AccessToken} are available (unless acquired
     * independently of this {@code AccessToken}). That is, if this
     * {@code AccessToken} was created by an {@link AccessManager}, then
     * the rights required to create this token are available again.
     *
     * @param cancelToken the {@code CancellationToken} which can be used to
     *   stop waiting for the release event of this {@code AccessToken}. That
     *   is, if this method detects, that cancellation was requested, it will
     *   throw an {@link org.jtrim.cancel.OperationCanceledException}. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified
     *   {@code CancellationToken} is {@code null}
     * @throws org.jtrim.cancel.OperationCanceledException thrown if
     *   cancellation request was detected by this method before this
     *   {@code AccessToken} has been released. This exception is not thrown if
     *   this {@code AccessToken} was released prior to this method call.
     *
     * @see #addReleaseListener(Runnable)
     */
    public void awaitRelease(CancellationToken cancelToken);

    /**
     * Waits until the executors of this {@code AccessToken} will not execute
     * any more tasks or the given timeout elapses. After this method returns
     * {@code true}, subsequent {@link #isReleased()}
     * method calls of the executors of this {@code AccessToken} will also
     * return {@code true}.
     * <P>
     * After this method returns {@code true}, the rights  associated with this
     * {@code AccessToken} are available (unless acquired independently of this
     * {@code AccessToken}). That is, if this {@code AccessToken} was created by
     * an {@link AccessManager}, then the rights required to create this token
     * are available again.
     *
     * @param cancelToken the {@code CancellationToken} which can be used to
     *   stop waiting for the release event of this {@code AccessToken}. That
     *   is, if this method detects, that cancellation was requested, it will
     *   throw an {@link org.jtrim.cancel.OperationCanceledException}. This
     *   argument cannot be {@code null}.
     * @param timeout the maximum time to wait for this
     *   {@code AccessToken} to be released in the given time unit. This
     *   argument must be greater than or equal to zero.
     * @param unit the time unit of the {@code timeout} argument. This argument
     *   cannot be {@code null}.
     * @return {@code true} if this {@code AccessToken} has been released before
     *   the timeout elapsed, {@code false} if the timeout elapsed first. In
     *   case this {@code code} was released prior to this call this method
     *   always returns {@code true}.
     *
     * @throws IllegalArgumentException thrown if the specified timeout value
     *   is negative
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     * @throws org.jtrim.cancel.OperationCanceledException thrown if
     *   cancellation request was detected by this method before this
     *   {@code AccessToken} was released or the given timeout elapsed. This
     *   exception is not thrown if this {@code AccessToken} was released prior
     *   to this method call.
     *
     * @see #addReleaseListener(Runnable)
     */
    public boolean tryAwaitRelease(CancellationToken cancelToken, long timeout, TimeUnit unit);
}
