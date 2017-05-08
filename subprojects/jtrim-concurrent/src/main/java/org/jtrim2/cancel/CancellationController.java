package org.jtrim2.cancel;

/**
 * Defines an interface to cancel an asynchronously executing task. The
 * {@code CancellationController} is usually associated with a
 * {@link CancellationToken} sharing the same {@link CancellationSource}. In
 * this case the associated {@code CancellationToken} will signal that
 * cancellation was requested after the {@link #cancel() cancel()} method has
 * been called.
 * <P>
 * Canceling a task is an idempotent action (that is, canceling it multiple
 * times has no further effect).
 *
 * <h3>Thread safety</h3>
 * Implementations of this interface must be safe to be accessed by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The {@link #cancel() cancel()} method of this interface is not required to
 * be <I>synchronization transparent</I> but it must not execute expensive
 * computations and must not wait for external events (such as an IO operation).
 *
 * @see CancellationSource
 * @see CancellationToken
 */
public interface CancellationController {
    /**
     * Requests that the associated task be canceled. When and, if the task will
     * actually be canceled depends on the task but tasks should terminate as
     * soon as possible when they detect that cancellation was requested.
     * <P>
     * If this {@code CancellationController} is associated with a
     * {@link CancellationToken}, this method may need to invoke the listeners
     * registered to listen for cancellation requests. In case they throw any
     * unchecked exception (although reasonable listener should not), the
     * exception may be propagated to the caller of this method.
     * <P>
     * This method is idempotent (that is, canceling it multiple times has no
     * further effect).
     */
    public void cancel();
}
