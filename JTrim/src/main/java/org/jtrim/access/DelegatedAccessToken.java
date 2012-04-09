package org.jtrim.access;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

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
        ExceptionHelper.checkNotNullArgument(token, "token");

        this.wrappedToken = token;
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
    public IDType getAccessID() {
        return wrappedToken.getAccessID();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean executeNowAndShutdown(Runnable task) {
        return wrappedToken.executeNowAndShutdown(task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <T> T executeNowAndShutdown(Callable<T> task) {
        return wrappedToken.executeNowAndShutdown(task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean executeNow(Runnable task) {
        return wrappedToken.executeNow(task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <T> T executeNow(Callable<T> task) {
        return wrappedToken.executeNow(task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void executeAndShutdown(Runnable task) {
        wrappedToken.executeAndShutdown(task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void awaitTermination() throws InterruptedException {
        wrappedToken.awaitTermination();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ListenerRef<AccessListener> addAccessListener(AccessListener listener) {
        return wrappedToken.addAccessListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void execute(Runnable command) {
        wrappedToken.execute(command);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Future<?> submit(Runnable task) {
        return wrappedToken.submit(task);
    }

    /**
     * {@inheritDoc }
     *
     * @param <T> the type of the return value of the specified task
     */
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return wrappedToken.submit(task, result);
    }

    /**
     * {@inheritDoc }
     *
     * @param <T> the type of the return value of the specified task
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return wrappedToken.submit(task);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public List<Runnable> shutdownNow() {
        return wrappedToken.shutdownNow();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void shutdown() {
        wrappedToken.shutdown();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isTerminated() {
        return wrappedToken.isTerminated();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isShutdown() {
        return wrappedToken.isShutdown();
    }

    /**
     * {@inheritDoc }
     *
     * @param <T> the type of the return value of the specified tasks
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return wrappedToken.invokeAny(tasks, timeout, unit);
    }

    /**
     * {@inheritDoc }
     *
     * @param <T> the type of the return value of the specified tasks
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return wrappedToken.invokeAny(tasks);
    }

    /**
     * {@inheritDoc }
     *
     * @param <T> the type of the return value of the specified tasks
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return wrappedToken.invokeAll(tasks, timeout, unit);
    }

    /**
     * {@inheritDoc }
     *
     * @param <T> the type of the return value of the specified tasks
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return wrappedToken.invokeAll(tasks);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return wrappedToken.awaitTermination(timeout, unit);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String toString() {
        return wrappedToken.toString();
    }
}
