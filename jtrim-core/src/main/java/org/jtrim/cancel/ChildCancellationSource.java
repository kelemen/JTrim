package org.jtrim.cancel;

/**
 * Defines a {@link CancellationSource} whose {@link CancellationToken} can also
 * be canceled through a {@link #getParentToken() parent}
 * {@code CancellationToken}. That is, whenever the parent
 * {@code CancellationToken} signals a cancellation request, the
 * {@code CancellationToken} of the {@code ChildCancellationSource} will also
 * signal cancellation request.
 * <P>
 * When detecting cancellation request is no longer required, the
 * {@code ChildCancellationSource} can be {@link #detachFromParent() detached}
 * from the parent {@code CancellationToken}. Detaching from the parent
 * {@code CancellationToken} allows to cleanup any resources required to be
 * notified of the cancellation request of the parent {@code CancellationToken}.
 * Usually this means, that a cancellation listener is unregistered from the
 * parent {@code CancellationToken}.
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
 * @see Cancellation#createCancellationSource()
 *
 * @author Kelemen Attila
 */
public interface ChildCancellationSource extends CancellationSource {
    /**
     * Returns the parent {@code CancellationToken} whose cancellation request
     * is to be forwarded of the {@code CancellationToken} of this
     * {@code ChildCancellationSource} (i.e.: the token returned by
     * {@code getToken()}). Note that cancellation request is only forwarded
     * until this {@code ChildCancellationSource} has been detached from the
     * parent {@code CancellationToken}.
     * <P>
     * This method can be used even after this {@code ChildCancellationSource}
     * has been detached from the parent {@code CancellationToken}.
     *
     * @return the parent {@code CancellationToken} whose cancellation request
     *   is to be forwarded of the {@code CancellationToken}. This method never
     *   returns {@code null}.
     */
    public CancellationToken getParentToken();

    /**
     * Detaches this {@code ChildCancellationSource} from its
     * {@link #getParentToken() parent} {@code CancellationToken}, so that
     * cancellation requests of the parent token will no longer notify the
     * {@code CancellationToken} of this {@code ChildCancellationSource}
     * (i.e.: the token returned by {@code getToken()}).
     * <P>
     * Cancellation requests made to the parent {@code CancellationToken} will
     * not affect this {@code ChildCancellationSource} after this method
     * returns. Note that it is possible that prior or concurrent cancellation
     * request of the parent {@code CancellationToken} will take effect only
     * after this method returns. That is, cancellation listeners registered
     * to the {@code CancellationToken} of this {@code ChildCancellationSource}
     * may still be invoked after this method returns, even if this cancellation
     * resulted from a cancellation request of the parent
     * {@code CancellationToken}.
     */
    public void detachFromParent();
}
