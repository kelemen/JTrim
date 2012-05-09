package org.jtrim.cancel;

/**
 * Thrown by tasks when they detect that they were requested to be canceled.
 *
 * @see CancelableTask
 * @see TaskExecutor
 *
 * @author Kelemen Attila
 */
public class OperationCanceledException extends RuntimeException {
    private static final long serialVersionUID = 6512650623463624418L;

    /**
     * Creates the exception with a specific {@link #getCause() cause}.
     * The {@link #getMessage() detail message} will be the string returned
     * by the {@code cause.toString()} call.
     *
     * @param cause the causing exception of the {@code OperationCanceledException}.
     *   The cause of {@code OperationCanceledException} should usually be
     *   {@code null} but it is possible to specify the cause to preserve the
     *   stack trace of other exceptions signaling cancellation. This argument
     *   can be {@code null} if there is no cause.
     */
    public OperationCanceledException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message} and {@link #getCause() cause}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     * @param cause the causing exception of the {@code OperationCanceledException}.
     *   The cause of {@code OperationCanceledException} should usually be
     *   {@code null} but it is possible to specify the cause to preserve the
     *   stack trace of other exceptions signaling cancellation. This argument
     *   can be {@code null} if there is no cause.
     */
    public OperationCanceledException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates the exception with a specific
     * {@link #getMessage() detail message}.
     *
     * @param message the message to be returned by
     *   {@link #getMessage() getMessage()}. This argument can be {@code null}.
     */
    public OperationCanceledException(String message) {
        super(message);
    }

    /**
     * Creates the exception with {@code null} as its
     * {@link #getMessage() detail message}.
     */
    public OperationCanceledException() {
    }
}
