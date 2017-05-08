package org.jtrim2.access;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.TaskExecutor;

/**
 * An {@code AccessToken} implementation which delegates all of its methods to
 * another {@code AccessToken} specified at construction time.
 * <P>
 * This implementation does not declare any methods other than the ones
 * {@code AccessToken} offers but implements all of them by forwarding to
 * another {@code AccessToken} implementation specified at construction time.
 * <P>
 * This class was designed for two reasons:
 * <ul>
 *  <li>
 *   To allow a safer way of class inheritance, so there can be no unexpected
 *   dependencies on overridden methods. To imitate inheritance subclass
 *   {@code DelegatedAccessToken}: specify the {@code AccessToken} you want to
 *   "subclass" in the constructor and override the required methods or provide
 *   new ones.
 *  </li>
 *  <li>
 *   To hide other public methods of an {@code AccessToken} from external code.
 *   This way, the external code can only access methods which the
 *   {@code AccessToken} interface provides.
 *  </li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * The thread safety properties of this class entirely depend on the wrapped
 * {@code AccessToken} instance.
 *
 * <h4>Synchronization transparency</h4>
 * If instances of this class are <I>synchronization transparent</I> or if its
 * synchronization control can be observed by external code entirely depends on
 * the wrapped {@code AccessToken} instance.
 *
 * @param <IDType> the type of the access ID (see {@link #getAccessID()})
 *
 * @author Kelemen Attila
 */
public class DelegatedAccessToken<IDType> implements AccessToken<IDType> {
    /**
     * The {@code AccessToken} to which the methods are forwarded.
     * This field can never be {@code null} because the constructor throws
     * {@code NullPointerException} if {@code null} was specified as the
     * {@code AccessToken}.
     */
    protected final AccessToken<IDType> wrappedToken;

    /**
     * Initializes the {@link #wrappedToken wrappedToken} field with
     * the specified argument.
     *
     * @param token the {@code AccessToken} to which the methods are forwarded.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified {@code AccessToken}
     *   is {@code null}
     */
    public DelegatedAccessToken(AccessToken<IDType> token) {
        Objects.requireNonNull(token, "token");

        this.wrappedToken = token;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public IDType getAccessID() {
        return wrappedToken.getAccessID();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public TaskExecutor createExecutor(TaskExecutor executor) {
        return wrappedToken.createExecutor(executor);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ListenerRef addReleaseListener(Runnable listener) {
        return wrappedToken.addReleaseListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isReleased() {
        return wrappedToken.isReleased();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void release() {
        wrappedToken.release();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void releaseAndCancel() {
        wrappedToken.releaseAndCancel();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void awaitRelease(CancellationToken cancelToken) {
        wrappedToken.awaitRelease(cancelToken);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean tryAwaitRelease(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        return wrappedToken.tryAwaitRelease(cancelToken, timeout, unit);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        return wrappedToken.toString();
    }

    @Override
    public boolean isExecutingInThis() {
        return wrappedToken.isExecutingInThis();
    }
}
