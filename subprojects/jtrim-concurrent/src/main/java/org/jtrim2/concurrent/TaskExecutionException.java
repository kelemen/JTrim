package org.jtrim2.concurrent;

import org.jtrim2.utils.ExceptionHelper;

/**
 * Defines that an asynchronously executed task has thrown an unchecked
 * exception. This exception is the counter part of the
 * {@code java.util.concurrent.ExecutionException} but unlike
 * {@code ExecutionException}, the {@code TaskExecutionException} is an
 * unchecked exception (which makes sense since the submitted tasks are not
 * allowed to throw checked exceptions).
 * <P>
 * The cause of {@code TaskExecutionException} is the exception actually thrown
 * by the asynchronously executed task and is never {@code null}. That is,
 * {@code getCause()} will never returns {@code null}.
 *
 * @see TaskFuture
 *
 * @author Kelemen Attila
 */
public class TaskExecutionException extends RuntimeException {
    private static final long serialVersionUID = 2723545721279260492L;

    /**
     * Creates a new {@code TaskExecutionException} with a given cause. The
     * cause defines the actual exception thrown by the asynchronously executed
     * task.
     *
     * @param cause the exception thrown by the asynchronously executed task.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified cause is
     *   {@code null}
     */
    public TaskExecutionException(Throwable cause) {
        super(cause);
        ExceptionHelper.checkNotNullArgument(cause, "cause");
    }

    /**
     * Creates a new {@code TaskExecutionException} with a given cause and
     * exception message. The cause defines the actual exception thrown by the
     * asynchronously executed task.
     *
     * @param message the {@code String} to be returned by the
     *   {@code getMessage()} method. This argument is allowed to be
     *   {@code null}.
     * @param cause the exception thrown by the asynchronously executed task.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified cause is
     *   {@code null}
     */
    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
        ExceptionHelper.checkNotNullArgument(cause, "cause");
    }

    /**
     * Throws the cause of this exception if it is an instance of {@link Error}
     * or {@link RuntimeException}, or throws a {@code RuntimeException}
     * exception with the cause of this exception as the cause of the thrown
     * exception.
     * <P>
     * Note that this method never returns normally and always throws an
     * exception. The return value is simply for convenience, so that you
     * can write:
     * <P>
     * {@code throw rethrowCause();}
     * <P>
     * This allows the Java compiler to detect that the code after this method
     * is not reachable.
     *
     * @return this method never returns, the return value is provided solely
     *   for convenience
     */
    public final RuntimeException rethrowCause() {
        throw ExceptionHelper.throwUnchecked(getCause());
    }

    /**
     * Throws the cause of this exception if it is an instance of {@link Error}
     * or {@link RuntimeException} or is of the given type, or throws a
     * {@code RuntimeException} exception with the cause of this exception as
     * the cause of the thrown exception.
     * <P>
     * Note that this method never returns normally and always throws an
     * exception. The return value is simply for convenience, so that you
     * can write:
     * <P>
     * {@code throw rethrowCause(MyException.class);}
     * <P>
     * This allows the Java compiler to detect that the code after this method
     * is not reachable.
     *
     * @param <T> the type of the checked exception type which might be rethrown
     *   as is
     * @param checkedType a type defining a {@code Throwable}, if the cause
     *   implements this class, it is simply rethrown instead of wrapped. This
     *   argument cannot be {@code null}. However, if {@code null} is passed for
     *   this argument, the cause is still rethrown but a
     *   {@code NullPointerException} will be attached to it as a suppressed
     *   exception.
     * @return this method never returns, the return value is provided solely
     *   for convenience
     *
     * @throws T thrown if the given exception is an instance of the given class
     */
    public final <T extends Throwable> RuntimeException rethrowCause(Class<? extends T> checkedType) throws T {
        throw ExceptionHelper.throwChecked(getCause(), checkedType);
    }
}
