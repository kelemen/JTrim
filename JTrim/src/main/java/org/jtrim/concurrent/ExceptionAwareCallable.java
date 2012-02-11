package org.jtrim.concurrent;

import java.util.concurrent.*;
import org.jtrim.utils.*;

/**
 * Defines a {@link Callable} wrapping another {@code Callable} and intercepting
 * all the exceptions thrown by the wrapped {@code Callable}. Exceptions
 * intercepted will be forwarded to a user defined listener. Note however that
 * after the listener handles the exception, the exception will be propagated
 * to the caller. Exceptions thrown by the listener will be suppressed (See:
 * {@link Throwable#addSuppressed(Throwable)}).
 *
 * <h3>Thread safety</h3>
 * The thread safety property is derived from the wrapped {@code Callable} and
 * the specified listener.
 *
 * <h4>Synchronization transparency</h4>
 * The <I>synchronization transparent</I> property is derived from the wrapped
 * {@code Callable} and the specified listener. However it is best to assume
 * that this class is not <I>synchronization transparent</I>.
 *
 * @param <T> the type of the result of the {@link #call() call()} method.
 *
 * @see ExceptionAwareExecutorService
 * @see ExceptionAwareRunnable
 *
 * @author Kelemen Attila
 */
public final class ExceptionAwareCallable<T> implements Callable<T> {
    private final Callable<T> wrappedCallable;
    private final ExceptionListener<Callable<T>> listener;

    /**
     * Creates a new {@code ExceptionAwareCallable} with a given wrapped
     * {@code Callable} and listener.
     *
     * @param <V> the type of the {@code Callable} to be wrapped
     * @param wrappedCallable the {@code Callable} to which the
     *   {@link #call() call()} method will be forwarded to. This argument
     *   cannot be {@code null}.
     * @param listener the listener to be notified when the wrapped
     *   {@code Callable} throws an exception. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    @SuppressWarnings("unchecked")
    public <V extends Callable<T>> ExceptionAwareCallable(
            V wrappedCallable, ExceptionListener<? super V> listener) {

        ExceptionHelper.checkNotNullArgument(wrappedCallable, "wrappedCallable");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        this.wrappedCallable = wrappedCallable;
        // This cast is required only because "V" cannot be used in the field
        // declaration. But note that the constraints in the constructor ensures
        // type safety.
        this.listener = (ExceptionListener<Callable<T>>)listener;
    }

    /**
     * Invokes the {@code call()} method of the wrapped {@code Callable} and
     * returns its result. If the underlying {@code Callable} throws an
     * exception, the exception will be forwarded to the listener specified at
     * construction time. Note however, that the exception will still be thrown
     * by this method and if the listener throws an exception as well, the
     * exception thrown by the listener will be suppressed (See:
     * {@link Throwable#addSuppressed(Throwable)}).
     *
     * @return the result of the wrapped {@code Callable}
     *
     * @throws Exception thrown if the underlying {@code Callable} throws an
     *   exception. This is the same exception as the one thrown by the wrapped
     *   {@code Callable}.
     */
    @Override
    public T call() throws Exception {
        try {
            return wrappedCallable.call();
        } catch (Throwable ex) {
            try {
                listener.onException(ex, wrappedCallable);
            } catch (Throwable listenerEx) {
                ex.addSuppressed(listenerEx);
            }
            throw ex;
        }
    }
}