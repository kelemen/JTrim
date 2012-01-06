package org.jtrim.access;

import java.util.concurrent.*;

import org.jtrim.event.*;

/**
 * Allows execution of tasks while having read or write (exclusive) access
 * to some resources. An access is considered granted while
 * {@link #isTerminated() isTerminated()} returns {@code false} afterwards
 * the right to access resources are considered lost. Notice that this implies
 * that the access right cannot get lost while a task is actively executing
 * on the {@code AccessToken}.
 * <P>
 * Instances of this class are usually tied to a particular
 * {@link AccessManager} which can grant access to resources. The
 * {@code AccessToken} will hold onto these resources until it has been
 * shutted down (which can also be caused by the owner {@code AccessManager}).
 * <P>
 * Note that every {@code AccessToken} is also an
 * {@link java.util.concurrent.ExecutorService ExecutorService} and as such
 * must be shutted down either by calling {@link #shutdown() shutdown()} or
 * {@link #shutdownNow() shutdownNow()}. Unlike general {@code ExecutorService}
 * implementations, an {@code AccessToken} instance can notify clients
 * asynchronously that it has been shutted down.
 * <P>
 * Implementation can subclass {@link AbstractAccessToken} for convenience.
 *
 * <h3>Thread safety</h3>
 * Instances of this interface are required to be completely thread-safe
 * without any further synchronization.
 * <h4>Synchronization transparency</h4>
 * Unless documented otherwise the methods of this class are not required
 * to be <I>synchronization transparent</I>.
 *
 * @param <IDType> the type of the access ID (see {@link #getAccessID()})
 *
 * @see AbstractAccessToken
 * @see AccessListener
 * @see AccessManager
 * @see AccessTokens
 * @see GenericAccessToken
 * @author Kelemen Attila
 */
public interface AccessToken<IDType> extends ExecutorService {
    /**
     * Returns an ID of the request used to create this {@code AccessToken}
     * instance.
     * <P>
     * Note that this ID does not need to be unique but cannot be {@code null}.
     * The ID is intended to be used to provide information about this access,
     * so a client can determine if it should cancel this token to allow
     * a new token to be created which conflicts with this token.
     * <h5>Thread safety</h5>
     * Implementations of this method required to be completely thread-safe
     * without any further synchronization.
     * <h6>Synchronization transparency</h6>
     * Implementations of this method required to be <I>synchronization
     * transparent</I>.
     * @return the ID of the request used to create this {@code AccessToken}
     * instance. This method always returns the same object for a given
     * {@code AccessToken} instance and never returns {@code null}.
     * @see AccessRequest
     */
    public IDType getAccessID();

    /**
     * Shutdowns this {@code AccessToken} (effectively calling
     * {@link #shutdownNow() shutdownNow()}), and waits until it terminates.
     * <P>
     * If this method returns normally (not throwing any exception),
     * {@link #isTerminated() isTerminated()} must return {@code true}
     * afterwards. So any task executed on this {@code AccessToken}
     * <I>happen-before</I> the successful call to this method.
     * <P>
     * Note that this method cannot be interrupted but it will preserve the
     * interrupted status of the current thread.
     */
    public void release();

    /**
     * Registers a new listener which will be notified if this
     * {@code AccessToken} terminates.
     * <P>
     * If this {@code AccessToken} was shutted down before (or during)
     * registering this listener, the listener may or may not will be notified
     * of this event. Assuming that the listener was implemented to be
     * idempotent (multiple notifications are equivalent to a single
     * notification), the following registration method will always notify
     * the listener (once or twice) if it is not unregistered:
     * <pre>
     * void registerListener(AccessToken<?> token, AccessListener listener) {
     *   token.addAccessListener(listener);
     *   if (token.isTerminated()) {
     *     listener.onLostAccess();
     *   }
     * }
     * </pre>
     * If the same listener is registered twice without unregistering,
     * the following two behaviours are allowed:
     * <ul>
     * <li>The method call is silently ignored.</li>
     * <li>The listener will be registered twice and will be
     * called twice if this {@code AccessToken} terminates.</li>
     * </ul>
     * So to avoid confusion clients are advised against registering
     * the same listener multiple times.
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
     * reference the registered listeners. Note however that returned reference
     * may still reference other (not explicitly unregistered) listener
     * references.
     *
     * @param listener the listener to be registered to notified about
     *    the termination of this {@code AccessToken}.
     * @return the reference to the newly registered listener which can be
     *   used to remove this newly registered listener, so it will no longer
     *   be notified of the terminate event. Note that this method may return
     *   an unregistered listener if this {@code AccessToken} has already been
     *   terminated. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the passed listener is
     *   {@code null}
     * @see #removeAccessListener(AccessListener)
     */
    public ListenerRef<AccessListener> addAccessListener(AccessListener listener);

    /**
     * Executes a task on the current call stack immediately and returns
     * its result.
     * <P>
     * If this {@code AccessToken} was shutted down this method returns
     * immediately without executing the given task.
     * <P>
     * Note that in most cases the {@link #executeNow(java.util.concurrent.Callable)}
     * method is preferred over this method.
     *
     * @param <T> the type of the result of the submitted task
     * @param task the task to be executed. This argument cannot be
     *   {@code null}.
     * @return  the result of the task or {@code null} if this
     *   {@code AccessToken} was shutted down before it could execute the
     *   requested task
     *
     * @throws NullPointerException thrown if the passed task is {@code null}
     * @throws TaskInvokeException thrown if the task has raised an exception.
     *   The cause of the {@code TaskInvokeException} can be retrieved to
     *   query the exception raised by the task.
     */
    public <T> T executeNow(Callable<T> task);

    /**
     * Executes a task on the current call stack immediately.
     * <P>
     * If this {@code AccessToken} was shutted down this method returns
     * immediately without executing the given task.
     * <P>
     * Note that in most cases the {@link #executeNowAndShutdown(Runnable)}
     * method is preferred over this method.
     * <P>
     * Be wary with this method because
     * {@link AccessManager#getScheduledAccess(org.jtrim.access.AccessRequest) scheduled tokens}
     * might have to wait for the right this access token represents to become
     * available which is dead-lock prone in some situation.
     *
     * @param task the task to be executed. This argument cannot be
     *   {@code null}.
     * @return {@code true} if the task was executed successfully, {@code false}
     *   if the given task could not be executed because this
     *   {@code AccessToken} was shutted down
     *
     * @throws NullPointerException thrown if the passed task is {@code null}
     * @throws TaskInvokeException thrown if the task has raised an exception.
     *   The cause of the {@code TaskInvokeException} can be retrieved to
     *   query the exception raised by the task.
     */
    public boolean executeNow(Runnable task);

    /**
     * Executes a task on the current call stack immediately, returns
     * its result and shutdowns down this {@code AccessToken}.
     * <P>
     * If this {@code AccessToken} was shutted down this method returns
     * immediately without executing the given task.
     * <P>
     * Note that this method does not guarantee that the last task to be
     * executed is the submitted task but regardless how this task returns
     * (even if it throws an exception) this {@code AccessToken} will be
     * shutted down.
     * <P>
     * Be wary with this method because
     * {@link AccessManager#getScheduledAccess(org.jtrim.access.AccessRequest) scheduled tokens}
     * might have to wait for the right this access token represents to become
     * available which is dead-lock prone in some situation.
     *
     * @param <T> the type of the result of the submitted task
     * @param task task the task to be executed. This argument cannot be
     *   {@code null}.
     * @return the result of the task or {@code null} if this
     *   {@code AccessToken} was shutted down before it could execute the
     *   requested task
     *
     * @throws NullPointerException thrown if the passed task is {@code null}.
     *   Note that in this case the executor will not be shutted down.
     * @throws TaskInvokeException thrown if the task has raised an exception.
     *   The cause of the {@code TaskInvokeException} can be retrieved to
     *   query the exception raised by the task.
     */
    public <T> T executeNowAndShutdown(Callable<T> task);

    /**
     * Executes a task on the current call stack immediately and
     * shutdowns down this {@code AccessToken}.
     * <P>
     * If this {@code AccessToken} was shutted down this method returns
     * immediately without executing the given task.
     * <P>
     * Note that this method does not guarantee that the last task to be
     * executed is the submitted task but regardless how this task returns
     * (even if it throws an exception) this {@code AccessToken} will be
     * shutted down.
     * <P>
     * Be wary with this method because
     * {@link AccessManager#getScheduledAccess(org.jtrim.access.AccessRequest) scheduled tokens}
     * might have to wait for the right this access token represents to become
     * available which is dead-lock prone in some situation.
     *
     * @param task task the task to be executed. This argument cannot be
     *   {@code null}.
     * @return {@code true} if the task was executed successfully, {@code false}
     *   if the given task could not be executed because this
     *   {@code AccessToken} was shutted down
     *
     * @throws NullPointerException thrown if the passed task is {@code null}.
     *   Note that in this case the executor will not be shutted down.
     * @throws TaskInvokeException thrown if the task has raised an exception.
     *   The cause of the {@code TaskInvokeException} can be retrieved to
     *   query the exception raised by the task.
     */
    public boolean executeNowAndShutdown(Runnable task);

    /**
     * Executes a task some time in the future and shuts down this
     * {@code AccessToken}. In general the task can be executed on any thread
     * (including the calling thread) but subclasses may further define
     * restrictions on what thread the submitted task can execute.
     * <P>
     * Note that this method does not guarantee that the last task to be
     * executed is the submitted task but regardless how this task returns
     * (even if it throws an exception) this {@code AccessToken} will be
     * shutted down.
     * <P>
     * Be wary with this method because
     * {@link AccessManager#getScheduledAccess(org.jtrim.access.AccessRequest) scheduled tokens}
     * might have to wait for the right this access token represents to become
     * available which is dead-lock prone in some situation.
     *
     * @param task the task to be executed. This argument cannot be
     *   {@code null}.
     * @throws NullPointerException thrown if the passed task is {@code null}.
     *   Note that in this case the executor will not be shutted down.
     */
    public void executeAndShutdown(Runnable task);

    /**
     * Waits until this {@code AccessToken} terminates.
     * <P>
     * If this method returns normally (without throwing an exception),
     * {@link #isTerminated() isTerminated()} will return {@code true}
     * afterwards. So any task executed on this {@code AccessToken}
     * <I>happen-before</I> the successful call to this method.
     *
     * @throws InterruptedException thrown if the current thread was
     *   interrupted. The interrupted status will be cleared in case this
     *   exception was thrown.
     */
    public void awaitTermination() throws InterruptedException;
}
