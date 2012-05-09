package org.jtrim.concurrent;

import org.jtrim.utils.ExceptionHelper;


/**
 * @deprecated MARKED FOR DELETION
 *
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
@Deprecated
public final class ExceptionAwareRunnable implements Runnable {
    private final ListenerWithRunnable<? extends Runnable> wrapped;

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
    public <V extends Runnable> ExceptionAwareRunnable(V wrappedRunnable,
            ExceptionListener<? super V> listener) {
        this.wrapped = new ListenerWithRunnable<>(wrappedRunnable, listener);
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
        wrapped.run();
    }

    private static class ListenerWithRunnable<T extends Runnable> {
        private final T wrappedRunnable;
        private final ExceptionListener<? super T> listener;

        public ListenerWithRunnable(
                T wrappedRunnable,
                ExceptionListener<? super T> listener) {

            ExceptionHelper.checkNotNullArgument(wrappedRunnable, "wrappedRunnable");
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            this.wrappedRunnable = wrappedRunnable;
            this.listener = listener;
        }

        public void run() {
            try {
                wrappedRunnable.run();
            } catch (Throwable exception) {
                listener.onException(exception, wrappedRunnable);
                throw exception;
            }
        }
    }
}
