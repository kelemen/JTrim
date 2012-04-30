package org.jtrim.concurrent.executor;

import org.jtrim.event.ListenerRef;

/**
 * Defines an interface to detect cancellation requests. How cancellation is
 * done is implementation dependent but it is usually controlled by an
 * associated {@link CancellationController} sharing the same
 * {@link CancellationSource}.
 * <P>
 * In general there are two ways to listen for cancellation requests:
 * <ul>
 *  <li>
 *   The {@code isCanceled()} method may be checked periodically and act
 *   accordingly when it returns {@code true}. Usually the best way to react
 *   to a cancellation request is to throw a {@link TaskCanceledException}. In
 *   this case the convenient {@link #checkCanceled() checkCanceled()} method
 *   can be used.
 *  </li>
 *  <li>
 *   A listener maybe added to be notified immediately when cancellation occurs.
 *   This can be done by calling the
 *   {@link #addCancellationListener(Runnable) addCancellationListener(Runnable)}
 *   method. This asynchronous notification is usually preferable when a subtask
 *   (such as an IO operation) must be canceled as a response to a cancellation
 *   request .
 *  </li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * The methods of this interface must be safe to be accessed by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The {@code isCanceled()} and the {@code checkCanceled()} methods must be
 * <I>synchronization transparent</I> but the {@code addCancellationListener}
 * is not (as it may invoke cancellation listeners).
 *
 * @see CancellationSource
 * @see CancellationSource#CANCELED_TOKEN
 * @see CancellationSource#UNCANCELABLE_TOKEN
 *
 * @author Kelemen Attila
 */
public interface CancellationToken {
    /**
     * Adds a listener which is to be called when cancellation was requested.
     * In case cancellation has already been requested, the listener may be
     * notified on the current thread. Note that the listener may be notified
     * in various places such as the {@link CancellationController#cancel() cancel}
     * method of {@link CancellationController} so it must not do anything
     * expensive and especially must not wait for external events (such as IO).
     * <P>
     * The specified listener will always be notified when cancellation is
     * requested regardless when it occurred and it will not be notified
     * multiple times.
     * <P>
     * Note that since this method may need to notify the listener on the
     * current thread, it may propagate any unchecked exception the listener
     * throws to the caller.
     *
     * @param listener the {@code Runnable} whose {@code run} method is to be
     *   called when cancellation was requested. This listener can be notified
     *   in various places, so it is especially important that this listener
     *   does as little task as possible and must not wait for external events.
     *   This argument cannot be {@code null}.
     * @return the {@code ListenerRef} which can be used to unregister the
     *   listener, so that it will no longer be notified of cancellation
     *   requests. Unregistering the listener prevents notifications of
     *   subsequent cancellation requests but if cancellation was requested
     *   prior to this call (or concurrently with this call), the listener
     *   might be notified even after the listener has been unregistered.
     *   This method never returns {@code null}.
     */
    public ListenerRef<Runnable> addCancellationListener(Runnable listener);

    /**
     * Returns {@code true} if cancellation was requested. This method may be
     * checked periodically to detect cancellation requests but since the usual
     * way to respond to such request is throwing a
     * {@link TaskCanceledException}, it is more convenient to use the
     * {@link #checkCanceled() checkCanceled()} method.
     * <P>
     * This method must be implemented so, that once it returned {@code true},
     * it must return {@code true} forever after (it cannot revert to returning
     * {@code false}).
     *
     * @return {@code true} if cancellation was requested, {@code false} if not
     */
    public boolean isCanceled();

    /**
     * Checks if cancellation has been requested and throws a
     * {@link TaskCanceledException} if so otherwise returns immediately without
     * doing anything.
     * <P>
     * This method is only provided for convenience and it is a shorthand for
     * the following code:
     * <code><pre>
     * if (token.isCanceled()) {
     *   throw new TaskCanceledException();
     * }
     * </pre></code>
     *
     * @throws TaskCanceledException thrown if cancellation was requested. If
     *   this exception is thrown, {@link #isCanceled() isCanceled()} returns
     *   {@code true}.
     */
    public void checkCanceled();
}
