package org.jtrim2.executor;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Defines a future of an already done task. No underlying task is actually
 * necessary, only the result of the (possibly not existent) task. This future
 * is not cancelable, attempting to cancel it does nothing.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to use by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * The methods of this class are <I>synchronization transparent</I>.
 *
 * @param <ResultType> the type of the result of this future
 *
 * @author Kelemen Attila
 */
public final class DoneFuture<ResultType> implements Future<ResultType> {
    private final ResultType result;

    /**
     * Creates a new {@code DoneFuture} with the given result.
     * The specified result will be returned by the {@code get} methods without
     * actually waiting.
     *
     * @param result the object to be returned by the {@code get} methods. This
     *   argument can be {@code null}.
     */
    public DoneFuture(ResultType result) {
        this.result = result;
    }

    /**
     * This method does nothing but returns {@code false}.
     *
     * @param mayInterruptIfRunning this argument is ignored
     * @return {@code false}
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * This method does nothing but returns {@code false}.
     *
     * @return {@code false}
     */
    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * This method does nothing but returns {@code true}.
     *
     * @return {@code true}
     */
    @Override
    public boolean isDone() {
        return true;
    }

    /**
     * This method returns the result specified at construction time.
     *
     * @return the object specified at construction time which can also be
     *   {@code null}
     */
    @Override
    public ResultType get() {
        return result;
    }

    /**
     * This method returns the result specified at construction time.
     *
     * @param timeout this argument is ignored
     * @param unit this argument is ignored
     * @return the object specified at construction time which can also be
     *   {@code null}
     */
    @Override
    public ResultType get(long timeout, TimeUnit unit) {
        return result;
    }
}
