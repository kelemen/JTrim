package org.jtrim.concurrent;

import org.jtrim.utils.ExceptionHelper;

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
}
