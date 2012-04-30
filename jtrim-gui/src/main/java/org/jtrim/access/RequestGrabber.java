package org.jtrim.access;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;

/**
 * Allows to acquire and release a specific {@code AccessRequest} from an
 * {@code AccessManager}. The request and the access manager is specified at
 * construction time and may not be changed later.
 * <P>
 * Once an instance of {@code RequestGrabber} was created, the
 * {@link #acquire() acquire()} method can be used to acquire the right and
 * disallow requests conflicting the request of the {@code RequestGrabber}
 * to be granted to anyone until {@link #release() release()} is called.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed from multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are not <I>synchronization transparent</I>. Note
 * however, that both the {@code acquire()} and {@code release()} method returns
 * without blocking and waiting for external events. That is, they can be called
 * from threads which must not be blocked (e.g.: the AWT event dispatch thread).
 *
 * @author Kelemen Attila
 */
public final class RequestGrabber {
    private final RequestWithManager<?, ?> request;

    /**
     * Creates a {@code RequestGrabber} which will use the specified
     * {@code AccessManager} to aquire the specified {@code AccessRequest}.
     *
     * @param <IDType> the type of the ID requested from the
     *   {@code AccessManager}
     * @param <RightType> the type of the rights which can possible be requested
     *   from the {@code AccessManager}
     * @param manager the {@code AccessManager} from which the given
     *   {@code AccessRequest} is to be acquired. This argument cannot be
     *   {@code null}.
     * @param request the request to be acquired by the
     *   {@link #acquire() acquire()} method call. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public <IDType, RightType> RequestGrabber(
            AccessManager<IDType, RightType> manager,
            AccessRequest<? extends IDType, ? extends RightType> request) {
        this.request = new RequestWithManager<>(manager, request);
    }

    /**
     * Acquires a scheduled {@link AccessToken} from the {@code AccessManager}
     * using the {@code AccessRequest} specified at construction time. In case
     * the request was already acquired by a previous call to {@code acquire()},
     * this method does nothing but returns immediately. That is, this method
     * is idempotent (if the {@code release()} method is not called within the
     * invocations of {@code acquire()}).
     * <P>
     * After this method returns subsequent conflicting {@code AccessToken}
     * requests from the {@code AccessManager} will be denied (except for
     * scheduled tokens as those are always granted but in blocked state) until
     * {@link #release() release()} is called.
     *
     * @see #release()
     */
    public void acquire() {
        request.acquire();
    }

    /**
     * Releases an a previously acquired {@link AccessToken}. If the
     * {@code AccessToken} was not acquired by a previous
     * {@link #acquire() acquire()} method call (or was already released), this
     * method does nothing but returns immediately. Therefore, this method is
     * idempotent. That is, this method is idempotent (if the {@code acquire()}
     * method is not called within the invocations of {@code release()}).
     * <P>
     * After this method returns subsequent conflicting {@code AccessToken}
     * requests from the {@code AccessManager} will be granted unless there are
     * other conflicting acquired access tokens (independent from the token of
     * this {@code RequestGrabber} or {@link #acquire() acquire()} is called.
     *
     * @see #acquire()
     */
    public void release() {
        request.release();
    }

    private class RequestWithManager<IDType, RightType> {
        private final AccessManager<IDType, RightType> manager;
        private final AccessRequest<? extends IDType, ? extends RightType> request;

        private final AtomicReference<AccessToken<?>> acquiredRef;

        public RequestWithManager(
                AccessManager<IDType, RightType> manager,
                AccessRequest<? extends IDType, ? extends RightType> request) {
            ExceptionHelper.checkNotNullArgument(manager, "manager");
            ExceptionHelper.checkNotNullArgument(request, "request");

            this.manager = manager;
            this.request = request;

            this.acquiredRef = new AtomicReference<>(null);
        }

        public void acquire() {
            if (acquiredRef.get() != null) {
                return;
            }

            AccessResult<?> result = manager.getScheduledAccess(request);
            if (!acquiredRef.compareAndSet(null, result.getAccessToken())) {
                result.shutdown();
            }
        }

        public void release() {
            AccessToken<?> token = acquiredRef.getAndSet(null);
            if (token != null) {
                token.shutdown();
            }
        }
    }
}
