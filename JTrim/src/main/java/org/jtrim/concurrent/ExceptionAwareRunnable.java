package org.jtrim.concurrent;

import org.jtrim.utils.ExceptionHelper;


/**
 * Defines a {@link Runnable} wrapping another {@code Runnable} and intercepting
 * all the exceptions thrown by the wrapped {@code Runnable}. Exceptions
 * intercepted will be forwarded to a user defined listener. Note however that
 * after the listener handles the exception, the exception will be propagated
 * to the caller. Exceptions thrown by the listener will be suppressed (See:
 * {@link Throwable#addSuppressed(Throwable)}).
 *
 * <h3>Thread safety</h3>
 * The thread safety property is derived from the wrapped {@code Runnable} and
 * the specified listener.
 *
 * <h4>Synchronization transparency</h4>
 * The <I>synchronization transparent</I> property is derived from the wrapped
 * {@code Runnable} and the specified listener. However it is best to assume
 * that this class is not <I>synchronization transparent</I>.
 *
 * @see ExceptionAwareExecutorService
 * @see ExceptionAwareCallable
 *
 * @author Kelemen Attila
 */
public final class ExceptionAwareRunnable implements Runnable {
    private final Runnable wrappedRunnable;
    private final ExceptionListener<Runnable> listener;

    /**
     * Creates a new {@code ExceptionAwareRunnable} with a given wrapped
     * {@code Runnable} and listener.
     *
     * @param <V> the type of the {@code Runnable} to be wrapped
     * @param wrappedRunnable the {@code Runnable} to which the
     *   {@link #run() run()} method will be forwarded to. This argument
     *   cannot be {@code null}.
     * @param listener the listener to be notified when the wrapped
     *   {@code Runnable} throws an exception. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    @SuppressWarnings("unchecked")
    public <V extends Runnable> ExceptionAwareRunnable(V wrappedRunnable,
            ExceptionListener<? super V> listener) {

        ExceptionHelper.checkNotNullArgument(wrappedRunnable, "wrappedRunnable");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        this.wrappedRunnable = wrappedRunnable;
        // This cast is required only because "V" cannot be used in the field
        // declaration. But note that the constraints in the constructor ensures
        // type safety.
        this.listener = (ExceptionListener<Runnable>) listener;
    }

    /**
     * Invokes the {@code run()} method of the wrapped {@code Runnable}. If the
     * underlying {@code Runnable} throws an exception, the exception will be
     * forwarded to the listener specified at construction time. Note however,
     * that the exception will still be thrown by this method and if the
     * listener throws an exception as well, the exception thrown by the
     * listener will be suppressed (See:
     * {@link Throwable#addSuppressed(Throwable)}).
     */
    @Override
    public void run() {
        try {
            wrappedRunnable.run();
        } catch (Throwable exception) {
            listener.onException(exception, wrappedRunnable);
            throw exception;
        }
    }
}
