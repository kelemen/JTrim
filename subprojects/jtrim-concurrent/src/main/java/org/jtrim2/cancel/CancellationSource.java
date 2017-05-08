package org.jtrim2.cancel;

/**
 * Defines an interface for linked {@link CancellationController}
 * and {@link CancellationToken} objects. These two objects returned by the
 * {@code CancellationSource} are linked in a way, that requesting
 * {@link CancellationController#cancel() cancellation} through the
 * {@code CancellationController} will cause the {@code CancellationToken} to
 * move to a canceled state.
 * <P>
 * The two objects can be returned by the {@link #getController() getController}
 * and the {@link #getToken() getToken} methods.
 *
 * <h3>Thread safety</h3>
 * Methods of this interface are required to be safely accessed by multiple
 * threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this interface are required to be
 * <I>synchronization transparent</I> but note that the methods of the
 * {@code CancellationController} and the {@code CancellationSource} are not.
 *
 * @see CancellationController
 * @see CancellationToken
 */
public interface CancellationSource {
    /**
     * Returns the {@code CancellationController} which can be used to signal
     * cancellation to the {@link CancellationToken} returned by the
     * {@link #getToken() getToken()} method. That is, after
     * the {@code getController().cancel()} invocation, the
     * {@code getToken().isCanceled()} invocation will return {@code true}.
     *
     * @return the {@code CancellationController} which can be used to signal
     *   cancellation to the {@link CancellationToken} returned by the
     *   {@link #getToken() getToken()} method. This method never returns
     *   {@code null} and every invocation of this method will return the same
     *   object.
     */
    public CancellationController getController();

    /**
     * Returns the {@code CancellationToken} which detects cancellation
     * requests made through the {@link CancellationController} returned by the
     * {@link #getController() getController()} method. That is, after
     * the {@code getController().cancel()} invocation, the
     * {@code getToken().isCanceled()} invocation will return {@code true}.
     *
     * @return the {@code CancellationToken} which detects cancellation
     *   requests made through the {@link CancellationController} returned by
     *   the {@link #getController() getController()} method. This method never
     *   returns {@code null} and every invocation of this method will return
     *   the same object.
     */
    public CancellationToken getToken();
}
