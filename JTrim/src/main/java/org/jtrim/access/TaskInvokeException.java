package org.jtrim.access;

import org.jtrim.utils.ExceptionHelper;

/**
 * Defines an unchecked exception that is thrown if a subtask throws an
 * exception.
 * <P>
 * This exception was designed for the case where exceptions are unexpected
 * and cannot be propagated directly to the client. This is the case where
 * a {@link java.util.concurrent.Callable Callable} is wrapped by a
 * {@link Runnable} because {@code Runnable} does not declare any checked
 * exceptions.
 * <P>
 * Note that the {@link #getCause() cause} of this exception can never be
 * {@code null}.
 *
 * @author Kelemen Attila
 */
public class TaskInvokeException extends RuntimeException {
    private static final long serialVersionUID = 6343976582602101291L;

    /**
     * Creates the exception with a specific {@link #getCause() cause}.
     * The {@link #getMessage() detail message} will be the string returned
     * by the {@code cause.toString()} call.
     *
     * @param cause the exception thrown by the subtask. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the {@code cause} is {@code null}.
     */
    public TaskInvokeException(Throwable cause) {
        super(cause);

        ExceptionHelper.checkNotNullArgument(cause, "cause");
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message} and {@link #getCause() cause}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}
     * @param cause the exception thrown by the subtask. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the {@code cause} is {@code null}.
     */
    public TaskInvokeException(String message, Throwable cause) {
        super(message, cause);

        ExceptionHelper.checkNotNullArgument(cause, "cause");
    }
}
