package org.jtrim2.executor;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim2.utils.ExceptionHelper;

/**
 * Stores the result of some possibly asynchronous computation. The result of
 * the computation can be either an error or an object.
 * <P>
 * This class can be used in situations where one thread initiates a task and
 * needs to wait for the result of its computation. This is already possible
 * when working with executors by waiting for their {@code Future} but maybe not
 * otherwise. See the following simple example for usage:
 * <pre>{@code
 * FutureResultHolder<String> resultHolder = new FutureResultHolder<>();
 * new Thread(() -> {
 *   resultHolder.tryStoreResult("SUCCESS");
 * }).start();
 * String result = resultHolder.waitResult();
 * assert "SUCCESS".equals(result);
 * }</pre>
 * In the above code, the should always succeed unless the thread executing the
 * code snippet is executed.
 * <P>
 * There are only two ways to signal the completion of a task:
 * <ul>
 *  <li>
 *   Invoke the {@link #tryStoreResult(Object) tryStoreResult(ResultType)}
 *   method to signal a successful completion of the computation.
 *  </li>
 *  <li>
 *   Invoke the {@link #trySetError(Throwable) trySetError(Throwable)} method to
 *   signal that the computation could not be completed due to an error where
 *   the specified error describes the failure.
 *  </li>
 * </ul>
 * The first of the above calls will determine the result of the computation,
 * subsequent signals for the completion of a task
 * <P>
 * Note that for compatibility with the JDK, instances of this class can be
 * viewed as {@link Future} object. This {@code Future} assumes
 * {@link CancellationException} errors to be cancellation requests and acts
 * according to the contract of the {@code Future} interface.
 *
 * <h3>Thread safety</h3>
 * Instances of this class are safe to be used by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Unless otherwise noted, methods of this class are not
 * <I>synchronization transparent</I>.
 *
 * @param <ResultType> the type of the result of the computation
 */
public final class FutureResultHolder<ResultType> {
    private final Lock storeLock;
    private final Condition storeSignal;
    private final Future<ResultType> futureView;

    private volatile boolean hasResult;
    private volatile ResultType result;
    private volatile Throwable error;

    /**
     * Creates a new {@code FutureResultHolder} with no result or error set.
     * <P>
     * To store the result of the computation invoke
     * {@link #tryStoreResult(Object) tryStoreResult(ResultType)}. To specify
     * that the computation failed due to an exception, call the
     * {@link #trySetError(Throwable) trySetError(Throwable)} method.
     */
    public FutureResultHolder() {
        this.storeLock = new ReentrantLock();
        this.storeSignal = storeLock.newCondition();
        this.futureView = new FutureView();
        this.hasResult = false;
        this.result = null;
        this.error = null;
    }

    private boolean tryStoreResult(ResultType result, Throwable error) {
        if (hasResult) {
            return false;
        }

        storeLock.lock();
        try {
            if (!hasResult) {
                this.result = result;
                this.error = error;
                this.hasResult = true;
                storeSignal.signalAll();
                return true;
            } else {
                return false;
            }
        } finally {
            storeLock.unlock();
        }
    }

    /**
     * Stores the result of a computation if it has not completed yet. In case
     * the computation has already been completed either by a previous
     * {@link #tryStoreResult(Object) tryStoreResult(ResultType)} or a
     * {@link #trySetError(Throwable) trySetError(Throwable)} method call, this
     * method does nothing but returns {@code false}.
     * <P>
     * After this method invocation the {@link #hasCompleted() hasCompleted()}
     * method will return {@code true} and {@code waitResult} will not block
     * but return immediately (possibly with an exception if there was a
     * previous successful {@code trySetError} call).
     *
     * <h3>Synchronization transparency</h3>
     * This method call is <I>synchronization transparent</I>.
     *
     * @param result the result of the computation. This argument is ignored
     *   and discarded if the computation has already been completed. This
     *   argument can be {@code null}.
     * @return {@code true} if the specified result of the computation was
     *   stored as the result. In this case the {@code waitResult} methods will
     *   return immediately without error by returning the specified object.
     *   In case the computation has already been completed, this method returns
     *   {@code false}. Note that at most one {@code tryStoreResult} call return
     *   {@code true} for the same {@code FutureResultHolder} instance.
     *
     * @see #trySetError(Throwable) trySetError(Throwable)
     * @see #waitCompletion() waitCompletion()
     * @see #waitCompletion(long, TimeUnit) waitCompletion(long, TimeUnit)
     */
    public boolean tryStoreResult(ResultType result) {
        return tryStoreResult(result, null);
    }

    /**
     * Signals the the computation has failed and the failure is described by
     * the specified exception. In case the computation has already been
     * completed either by a previous
     * {@link #tryStoreResult(Object) tryStoreResult(ResultType)} or a
     * {@link #trySetError(Throwable) trySetError(Throwable)} method call, this
     * method does nothing but returns {@code false}.
     * <P>
     * After this method invocation the {@link #hasCompleted() hasCompleted()}
     * method will return {@code true} and {@code waitResult} will not block
     * but return immediately (but maybe successfully without throwing an
     * exception if there was a previous successful {@code tryStoreResult}
     * call).
     *
     * <h3>Synchronization transparency</h3>
     * This method call is <I>synchronization transparent</I>.
     *
     * @param error the exception describing the failure which prevented the
     *   computation from completion. In case this error is a
     *   {@link CancellationException} the {@link #asFuture() future view} of
     *   this {@code FutureResultHolder} is treated as canceled. This argument
     *   cannot be {@code null}.
     * @return {@code true} if this computation was marked as a failed
     *   computation where the specified exception describes the reason of the
     *   failure. In this case the {@code waitResult} methods will
     *   immediately throw an {@link ExecutionException} with the specified
     *   exception as its {@link Throwable#getCause() cause}. In case the
     *   computation has already been completed, this method returns
     *   {@code false}. Note that at most one {@code tryStoreResult} call return
     *   {@code true} for the same {@code FutureResultHolder} instance.
     *
     * @see #tryStoreResult(Object) tryStoreResult(ResultType)
     * @see #waitCompletion() waitCompletion()
     * @see #waitCompletion(long, TimeUnit) waitCompletion(long, TimeUnit)
     */
    public boolean trySetError(Throwable error) {
        Objects.requireNonNull(error, "error");
        return tryStoreResult(null, error);
    }

    /**
     * Checks whether this computation has completed or not. That is, if
     * {@link #tryStoreResult(Object) tryStoreResult(ResultType)} or
     * {@link #trySetError(Throwable) trySetError(Throwable)} has been called
     * or not.
     * <P>
     * In case this method returns {@code true}, subsequent
     * {@code waitCompletion} methods will not block but return immediately or
     * throw an exception. Also if this method returns {@code true}, either
     * {@link #hasCompletedWithSuccess()} or {@link #hasCompletedWithError()}
     * will return {@code true} but only one of them.
     *
     * @return {@code true} if this computation has already completed,
     *   {@code false} otherwise
     *
     * @see #hasCompletedWithError()
     * @see #hasCompletedWithSuccess()
     */
    public boolean hasCompleted() {
        return hasResult;
    }

    /**
     * Returns {@code true} if this computation has been successfully completed
     * with a previous {@link #tryStoreResult(Object) tryStoreResult} method
     * call.
     * <P>
     * In case this method returns {@code true}, subsequent
     * {@code waitCompletion} methods will not block but return immediately
     * without throwing an exception. Also if this method returns {@code true},
     * the {@link #hasCompletedWithError()} method will return {@code false}.
     *
     * @return {@code true} if this computation has already been completed
     *   successfully, {@code false} otherwise
     *
     * @see #hasCompleted()
     * @see #hasCompletedWithError()
     */
    public boolean hasCompletedWithSuccess() {
        return hasResult && error == null;
    }

    /**
     * Returns {@code true} if this computation has failed due to a previous
     * {@link #trySetError(Throwable) trySetError(Throwable)} method call.
     * <P>
     * In case this method returns {@code true}, subsequent
     * {@code waitCompletion} methods will not block but immediately throw an
     * {@link ExecutionException}. Also if this method returns {@code true},
     * the {@link #hasCompletedWithSuccess()} method will return {@code false}.
     *
     * @return {@code true} if this computation has already been completed
     *   successfully, {@code false} otherwise
     *
     * @see #hasCompleted()
     * @see #hasCompletedWithSuccess()
     */
    public boolean hasCompletedWithError() {
        return hasResult && error != null;
    }

    private ResultType fetchResult() throws ExecutionException {
        assert hasResult;
        if (error != null) {
            throw new ExecutionException(error);
        } else {
            return result;
        }
    }

    private void waitCompletion() throws InterruptedException {
        if (!hasResult) {
            storeLock.lock();
            try {
                while (!hasResult) {
                    storeSignal.await();
                }
            } finally {
                storeLock.unlock();
            }
        }
    }

    private boolean waitCompletion(long timeout, TimeUnit unit)
            throws InterruptedException {
        ExceptionHelper.checkArgumentInRange(timeout,
                0, Long.MAX_VALUE, "timeout");
        Objects.requireNonNull(unit, "unit");

        if (!hasResult) {
            long nanosToWait = unit.toNanos(timeout);

            storeLock.lock();
            try {
                while (!hasResult) {
                    nanosToWait = storeSignal.awaitNanos(nanosToWait);
                    if (nanosToWait <= 0) {
                        return false;
                    }
                }
            } finally {
                storeLock.unlock();
            }
        }
        return true;
    }

    /**
     * Waits until the computation has been completed or the current thread
     * has been interrupted and returns the result of the computation (if not
     * interrupted).
     * <P>
     * This method will never block if a previous call to the
     * {@link #hasCompleted() hasCompleted()} returned {@code true}.
     *
     * @return the result of the computation set by a previous call to
     *   {@link #tryStoreResult(Object) tryStoreResult(ResultType)}. This method
     *   may return {@code null} if {@code null} was specified for the
     *   {@code tryStoreResult} method.
     *
     * @throws ExecutionException thrown if the computation has failed. The
     *   cause of the thrown exception is the exception describing the failure
     *   which can never be {@code null}.
     * @throws InterruptedException thrown if the current thread was interrupted
     *   before the result of the computation was available. This exception is
     *   never thrown if the result is available prior th invocation of this
     *   method (i.e.: {@link #hasCompleted() hasCompleted()} returns
     *   {@code true}).
     *
     * @see #hasCompleted() hasCompleted()
     * @see #hasCompletedWithError() hasCompletedWithError()
     * @see #hasCompletedWithSuccess() hasCompletedWithSuccess()
     *
     * @see #tryGetResult() tryGetResult()
     *
     * @see #tryStoreResult(Object) tryStoreResult(ResultType)
     * @see #trySetError(Throwable) trySetError(Throwable)
     */
    public ResultType waitResult()
            throws ExecutionException, InterruptedException {
        waitCompletion();
        return fetchResult();
    }

    /**
     * Waits until the computation has been completed or the current thread
     * has been interrupted or the specified timeout expires and returns the
     * result of the computation (if the timeout did not expire and the current
     * thread was not interrupted).
     * <P>
     * In case the timeout expires this method will return {@code null} without
     * throwing an exception. Note that a {@code null} value may also be
     * returned if {@code null} was specified for a previous
     * {@link #tryStoreResult(Object) tryStoreResult(ResultType)} method call.
     * <P>
     * This method will never block if a previous call to the
     * {@link #hasCompleted() hasCompleted()} returned {@code true}.
     *
     * @param timeout the maximum time to wait for the result of the computation
     *   in the specified time unit. This argument must be greater than or equal
     *   to zero.
     * @param unit the time unit of the {@code timeout} argument. This argument
     *   cannot be {@code null}.
     * @return the result of the computation set by a previous call to
     *   {@link #tryStoreResult(Object) tryStoreResult(ResultType)} or
     *   {@code null} if the timeout expires. This method may return
     *   {@code null} if the timeout expires or {@code null} was specified for
     *   the {@code tryStoreResult} method.
     *
     * @throws ExecutionException thrown if the computation has failed. The
     *   cause of the thrown exception is the exception describing the failure
     *   which can never be {@code null}.
     * @throws InterruptedException thrown if the current thread was interrupted
     *   before the result of the computation was available. This exception is
     *   never thrown if the result is available prior th invocation of this
     *   method (i.e.: {@link #hasCompleted() hasCompleted()} returns
     *   {@code true}).
     *
     * @see #hasCompleted() hasCompleted()
     * @see #hasCompletedWithError() hasCompletedWithError()
     * @see #hasCompletedWithSuccess() hasCompletedWithSuccess()
     *
     * @see #tryGetResult() tryGetResult()
     *
     * @see #tryStoreResult(Object) tryStoreResult(ResultType)
     * @see #trySetError(Throwable) trySetError(Throwable)
     */
    public ResultType waitResult(long timeout, TimeUnit unit)
            throws ExecutionException, InterruptedException {

        if (waitCompletion(timeout, unit)) {
            return fetchResult();
        } else {
            return null;
        }
    }

    /**
     * Returns the result of the computation if it is available or {@code null}
     * if it is not. This method will never block, it will return the result if
     * available immediately, otherwise returns {@code null}.
     * <P>
     * In case a previous call to the {@link #hasCompleted() hasCompleted()}
     * method returned {@code true}, this method will return the result of the
     * computation instead of failing by returning {@code null}. Note that this
     * method may still return {@code null} if {@code null} is the actual result
     * of the computation.
     *
     * @return the result of the computation or {@code null} if it is not yet
     *   available. Note that {@code null} result may indicate that the
     *   {@link #tryStoreResult(Object) tryStoreResult(ResultType)} method
     *   has been successfully called with {@code null} argument.
     *
     * @throws ExecutionException thrown if the computation has been terminated
     *   with a failure. The cause of the thrown exception is the exception
     *   describing the failure which can never be {@code null}.
     *
     * @see #hasCompleted() hasCompleted()
     * @see #hasCompletedWithError() hasCompletedWithError()
     * @see #hasCompletedWithSuccess() hasCompletedWithSuccess()
     *
     * @see #tryStoreResult(Object) tryStoreResult(ResultType)
     * @see #trySetError(Throwable) trySetError(Throwable)
     */
    public ResultType tryGetResult() throws ExecutionException {
        return hasResult ? fetchResult() : null;
    }

    /**
     * Returns this {@code FutureResultHolder} viewed as a {@link Future}. This
     * method is provided for compatibility with the JDK.
     * <P>
     * The returns {@code Future} is a fully functional {@code Future} whose
     * every method is implemented according to the contract of the
     * {@code Future} interface. The {@code Future} returned will treat failures
     * with {@link CancellationException} as a simple cancellation.
     * Appropriately, canceling the {@code Future} is equivalent to a
     * {@code trySetError(new CancellationException())} call.
     *
     * @return this {@code FutureResultHolder} viewed as a {@link Future}. This
     *   method never returns {@code null}.
     */
    public Future<ResultType> asFuture() {
        return futureView;
    };

    private class FutureView implements Future<ResultType> {
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return trySetError(new CancellationException());
        }

        @Override
        public boolean isCancelled() {
            if (hasCompletedWithError()) {
                return error instanceof CancellationException;
            } else {
                return false;
            }
        }

        @Override
        public boolean isDone() {
            return hasCompleted();
        }

        public ResultType fetchAndCheckCancel() throws ExecutionException {
            assert hasResult;

            if (error instanceof CancellationException) {
                // We create a new cancellation exception to preserve the
                // current stack trace in the thrown exception.
                CancellationException toThrow = new CancellationException();
                toThrow.initCause(error);
                throw toThrow;
            } else {
                return fetchResult();
            }
        }

        @Override
        public ResultType get()
                throws InterruptedException, ExecutionException {
            waitCompletion();
            return fetchAndCheckCancel();
        }

        @Override
        public ResultType get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {

            if (!waitCompletion(timeout, unit)) {
                throw new TimeoutException();
            } else {
                return fetchAndCheckCancel();
            }
        }
    }
}
