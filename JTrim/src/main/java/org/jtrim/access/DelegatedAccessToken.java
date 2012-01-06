package org.jtrim.access;

import java.util.*;
import java.util.concurrent.*;
import org.jtrim.utils.*;

/**
 * A base class that delegates all of its methods to a specific
 * {@link AccessToken} implementation.
 * <P>
 * This implementation does not declare any new methods that {@code AccessToken}
 * offers but implements all of them by forwarding to another
 * {@code AccessToken} implementation specified at construction time.
 * This class was designed to allow a safer way of class inheritance, so there
 * can be no unexpected dependencies on overridden methods. To imitate inheritance
 * subclass {@code DelegatedAccessToken}: specify the {@code AccessToken} you
 * want to "subclass" in the constructor and and override the required methods
 * or provide new ones.
 *
 * @param <IDType> the type of the access ID (see {@link #getAccessID()})
 *
 * @author Kelemen Attila
 */
public abstract class DelegatedAccessToken<IDType> implements AccessToken<IDType> {
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
    public void removeAccessListener(AccessListener listener) {
        wrappedToken.removeAccessListener(listener);
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
    public void addAccessListener(AccessListener listener) {
        wrappedToken.addAccessListener(listener);
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
